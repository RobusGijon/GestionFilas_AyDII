package servidor_central.conexion;

import java.io.IOException;
import java.net.Socket;

import servidor_central.datos.ClienteAtendido;
import servidor_central.datos.EstadoReconexionPuesto;
import servidor_central.datos.Turno;
import servidor_central.interfaces.ICanalMensaje;
import servidor_central.interfaces.ICoordinadorServidor;
import servidor_central.interfaces.IEstrategiaEncriptacion;
import servidor_central.interfaces.IPantalla;
import servidor_central.logging.ILogger;
import servidor_central.protocolo.CanalMensajes;
import servidor_central.protocolo.CanalMensajesEncriptados;
import servidor_central.protocolo.mensajes.Mensaje;
import servidor_central.protocolo.mensajes.TipoMensaje;

public class ManejadorConexion implements Runnable {

    private final Socket socket;
    private final ICoordinadorServidor coordinador;
    private final IPantalla pub;
    private final IEstrategiaEncriptacion encriptacion;
    private final ILogger log;

    /** id del puesto atendido por esta conexion request/response (-1 si no es puesto). */
    private int idPuestoConexion = -1;

    public ManejadorConexion(Socket socket, ICoordinadorServidor coordinador, IPantalla pub,
            IEstrategiaEncriptacion encriptacion, ILogger log) {
        this.socket = socket;
        this.coordinador = coordinador;
        this.pub = pub;
        this.encriptacion = encriptacion;
        this.log = log;
    }

    @Override
    public void run() {
        ICanalMensaje canal;
        try {
            canal = encriptacion == null
                    ? new CanalMensajes(socket)
                    : new CanalMensajesEncriptados(socket, encriptacion);
        } catch (IOException e) {
            log.logError("MANEJADOR_CONEXION", e);
            cerrarSocket();
            return;
        }

        boolean retener = false;
        try {
            while (true) {
                Mensaje m;
                try {
                    m = canal.recibir();
                } catch (IllegalArgumentException e) {
                    log.logError("PARSE", e);
                    canal.enviar(new Mensaje(TipoMensaje.ERROR, "MENSAJE_INVALIDO"));
                    continue;
                }
                if (m == null) {
                    break; // el peer cerro la conexion
                }
                try {
                    if (despachar(m, canal)) {
                        retener = true; // el canal quedo cedido a un publicador
                        return;
                    }
                } catch (RuntimeException e) {
                    String msg = (e.getMessage() != null) ? e.getMessage() : e.getClass().getSimpleName();
                    canal.enviar(new Mensaje(TipoMensaje.ERROR, sanitizar(msg)));
                }
            }
        } catch (IOException e) {
            log.logError("MANEJADOR_CONEXION", e);
        } finally {
            if (!retener) {
                canal.cerrar();
            }
            // (B1) Esta conexion request/response cerro: si llevaba un puesto, se
            // da de baja una conexion viva. El metodo es idempotente y solo deja
            // el puesto DESASIGNADO cuando cae la ultima conexion.
            if (idPuestoConexion != -1) {
                coordinador.notificarDesconexionPuesto(idPuestoConexion);
            }
        }
    }

    private boolean despachar(Mensaje m, ICanalMensaje canal) {
        switch (m.getTipo()) {
            case REGISTRAR_CLIENTE: {
                String dni = m.getArg(0);
                boolean ok = coordinador.registrarCliente(dni);
                canal.enviar(ok
                        ? new Mensaje(TipoMensaje.OK, "REGISTRADO")
                        : new Mensaje(TipoMensaje.ERROR, "DUPLICADO"));
                return false;
            }
            case CONECTAR_PUESTO: {
                String host = socket.getInetAddress().getHostAddress();
                int idDeseado = m.getArgs().isEmpty() ? -1 : parseIdDeseado(m.getArg(0));
                EstadoReconexionPuesto estado;
                if (idPuestoConexion == -1) {
                    // Primer CONECTAR sobre esta conexion: el coordinador reclama o
                    // crea el slot y cuenta esta conexion como viva.
                    estado = coordinador.conectarPuesto(host, idDeseado);
                    idPuestoConexion = estado.getIdPuesto();
                } else {
                    // Re-CONECTAR sobre una conexion ya registrada (reclamo de un
                    // puesto vivo cuyo canal no se cayo): solo se reenvia el estado,
                    // sin volver a contar la conexion.
                    estado = coordinador.estadoPuesto(idPuestoConexion);
                }
                responderSnapshotPuesto(canal, estado);
                return false;
            }
            case LLAMAR_SIGUIENTE: {
                int id = Integer.parseInt(m.getArg(0));
                Turno t = coordinador.llamarSiguiente(id);
                canal.enviar(t == null
                        ? new Mensaje(TipoMensaje.OK, "FILA_VACIA")
                        : new Mensaje(TipoMensaje.OK, "TURNO", t.getDni()));
                return false;
            }
            case RENOTIFICAR: {
                int id = Integer.parseInt(m.getArg(0));
                int n = coordinador.reNotificar(id);
                canal.enviar(new Mensaje(TipoMensaje.OK, "INTENTOS", String.valueOf(n)));
                return false;
            }
            case ELIMINAR_CLIENTE: {
                int id = Integer.parseInt(m.getArg(0));
                Turno t = coordinador.eliminarCliente(id);
                canal.enviar(t == null
                        ? new Mensaje(TipoMensaje.OK, "FILA_VACIA")
                        : new Mensaje(TipoMensaje.OK, "TURNO", t.getDni()));
                return false;
            }
            case SUSCRIBIR_PANTALLA: {
                boolean ok = pub.agregarObservador(canal);
                if (ok) {
                    canal.enviar(new Mensaje(TipoMensaje.OK, "SUSCRITO"));
                    log.logInfo("Pantalla suscripta desde " + socket.getInetAddress().getHostAddress());
                    coordinador.replicarEstadoAPantalla();
                    return true;
                } else {
                    canal.enviar(new Mensaje(TipoMensaje.ERROR, "SUSCRIPCION_FALLIDA"));
                    return false;
                }
            }
            case SUSCRIBIR_PUESTO: {
                int idPuesto = m.getArgs().isEmpty() ? -1 : parseIdDeseado(m.getArg(0));
                canal.enviar(new Mensaje(TipoMensaje.OK, "SUSCRITO"));
                boolean ok = coordinador.suscribirPuestoAEventos(idPuesto, canal);
                if (ok) {
                    return true;
                } else {
                    canal.enviar(new Mensaje(TipoMensaje.ERROR, "SUSCRIPCION_FALLIDA"));
                    return false;
                }
            }
            case HOLA_PAR: {
                // Si el listener de pares no está activo, este nodo está corriendo
                // como PRIMARIO (sólo el primario abre GestorConexion). Si el otro
                // se anuncia también como primario, hay split-brain: respondemos
                // para que se demueva.
                String rolPropuesto = m.getArgs().isEmpty() ? "" : m.getArg(0);
                if ("PRIMARIO".equals(rolPropuesto)) {
                    log.logInfo("[HA] HOLA_PAR primario duplicado desde "
                        + socket.getInetAddress().getHostAddress() + " - rechazando");
                    canal.enviar(new Mensaje(TipoMensaje.ERROR, "YA_HAY_PRIMARIO"));
                } else {
                    canal.enviar(new Mensaje(TipoMensaje.OK, "HOLA"));
                }
                return false;
            }
            default:
                canal.enviar(new Mensaje(TipoMensaje.ERROR, "TIPO_NO_SOPORTADO"));
                return false;
        }
    }

    /**
     * (C3) Responde el bloque enmarcado con el estado del puesto:
     * <pre>
     *   OK|ID_PUESTO|&lt;id&gt;
     *   OK|INICIO_SNAPSHOT
     *   EN_ATENCION|&lt;dni&gt;|&lt;intentos&gt;   (0 o 1 linea)
     *   ATENDIDO|&lt;dni&gt;|&lt;hora&gt;          (0..N lineas, historial cronologico)
     *   OK|FIN_SNAPSHOT
     * </pre>
     * El bloque se envia siempre (aunque este vacio) para que el puesto lo lea de
     * forma determinista tras el ID_PUESTO.
     */
    private void responderSnapshotPuesto(ICanalMensaje canal, EstadoReconexionPuesto estado) {
        canal.enviar(new Mensaje(TipoMensaje.OK, "ID_PUESTO", String.valueOf(estado.getIdPuesto())));
        canal.enviar(new Mensaje(TipoMensaje.OK, "INICIO_SNAPSHOT"));
        Turno turno = estado.getTurnoEnAtencion();
        if (turno != null) {
            canal.enviar(new Mensaje(TipoMensaje.EN_ATENCION,
                    turno.getDni(), String.valueOf(turno.getIntentosLlamados())));
        }
        for (ClienteAtendido c : estado.getHistorial()) {
            canal.enviar(new Mensaje(TipoMensaje.ATENDIDO, c.getDni(), c.getHora()));
        }
        canal.enviar(new Mensaje(TipoMensaje.OK, "FIN_SNAPSHOT"));
    }

    private int parseIdDeseado(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void cerrarSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            // ignorar
        }
    }

    private String sanitizar(String s) {
        return s.replace('|', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
