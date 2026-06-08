package puesto_atencion.conexion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import puesto_atencion.disponibilidad.Direccion;
import puesto_atencion.dominio.ClienteAtendido;
import puesto_atencion.dominio.EstadoReconexion;
import puesto_atencion.interfaces.ICanalMensaje;
import puesto_atencion.interfaces.IEstrategiaEncriptacion;
import puesto_atencion.interfaces.IGestorConexionServidor;
import puesto_atencion.interfaces.IListenerEventosServidor;
import puesto_atencion.protocolo.CanalMensajes;
import puesto_atencion.protocolo.CanalMensajesEncriptados;
import puesto_atencion.protocolo.mensajes.Mensaje;
import puesto_atencion.protocolo.mensajes.TipoMensaje;


public class GestorConexionServidor implements IGestorConexionServidor {

    private final Direccion direccion;
    private final IEstrategiaEncriptacion encriptacion;
    private final ICanalMensaje canalMensaje;

    // Estado de suscripcion: tocado por el hilo de eventos y por quien cierra,
    // por eso es volatile.
    private volatile ICanalMensaje canalEventos;
    private volatile Thread hiloSuscripcion;
    private volatile boolean hiloCorriendo = false;

    public GestorConexionServidor(Direccion direccion, IEstrategiaEncriptacion encriptacion) throws IOException {
        this.direccion = direccion;
        this.encriptacion = encriptacion;
        this.canalMensaje = crearCanal();
    }

    private ICanalMensaje crearCanal() throws IOException {
        return encriptacion == null
                ? new CanalMensajes(direccion)
                : new CanalMensajesEncriptados(direccion, encriptacion);
    }

    @Override
    public synchronized EstadoReconexion conectarPuesto(int idDeseado, IListenerEventosServidor listener) throws Exception {
        Mensaje pedido = idDeseado > 0
                ? new Mensaje(TipoMensaje.CONECTAR_PUESTO, String.valueOf(idDeseado))
                : new Mensaje(TipoMensaje.CONECTAR_PUESTO);
        Mensaje respuesta = enviarYRecibirMensaje(canalMensaje, pedido);
        if (respuesta.getArgs().size() < 2 || !"ID_PUESTO".equals(respuesta.getArg(0))) {
            throw new Exception("Respuesta inesperada del servidor: " + respuesta.serializar());
        }
        int idPuesto = parsearEntero(respuesta.getArg(1), "id de puesto");
        EstadoReconexion estado = leerSnapshotPuesto(idPuesto);

        if (listener != null) {
            try {
                suscribirEventos(idPuesto, listener);
            } catch (Exception e) {
                // La suscripcion es secundaria: si falla, el puesto igual queda
                // conectado, solo no se actualizara el contador de espera.
                log("no se pudo suscribir a eventos: " + e.getMessage());
            }
        }
        return estado;
    }

    /**
     * Lee el bloque enmarcado (snapshot) que el servidor envia tras ID_PUESTO:
     * {@code OK|INICIO_SNAPSHOT} / {@code EN_ATENCION|dni|intentos} (0 o 1) /
     * {@code ATENDIDO|dni|hora} (0..N) / {@code OK|FIN_SNAPSHOT}.
     */
    private EstadoReconexion leerSnapshotPuesto(int idPuesto) throws Exception {
        Mensaje inicio = canalMensaje.recibir();
        validarRespuesta(inicio);
        if (inicio.getArgs().isEmpty() || !"INICIO_SNAPSHOT".equals(inicio.getArg(0))) {
            throw new Exception("Se esperaba INICIO_SNAPSHOT, llego: " + inicio.serializar());
        }
        String dni = null;
        int intentos = 0;
        List<ClienteAtendido> historial = new ArrayList<>();
        while (true) {
            Mensaje m = canalMensaje.recibir();
            if (m == null) {
                throw new Exception("El servidor cerro la conexion durante el snapshot del puesto");
            }
            if (m.getTipo() == TipoMensaje.OK && !m.getArgs().isEmpty()
                    && "FIN_SNAPSHOT".equals(m.getArg(0))) {
                break;
            }
            switch (m.getTipo()) {
                case EN_ATENCION:
                    dni = m.getArg(0);
                    intentos = parsearEntero(m.getArg(1), "intentos en atencion");
                    break;
                case ATENDIDO:
                    historial.add(ClienteAtendido.desde(m.getArg(0), m.getArg(1)));
                    break;
                default:
                    // linea inesperada dentro del bloque: ignorar
                    break;
            }
        }
        return new EstadoReconexion(idPuesto, dni, intentos, historial);
    }

    @Override
    public synchronized String llamarSiguiente(int idPuesto) throws Exception {
        Mensaje respuesta = enviarYRecibirMensaje(canalMensaje,
                new Mensaje(TipoMensaje.LLAMAR_SIGUIENTE, String.valueOf(idPuesto)));
        return parsearTurnoOFilaVacia(respuesta);
    }

    @Override
    public synchronized int reNotificar(int idPuesto) throws Exception {
        Mensaje respuesta = enviarYRecibirMensaje(canalMensaje,
                new Mensaje(TipoMensaje.RENOTIFICAR, String.valueOf(idPuesto)));
        if (respuesta.getArgs().size() < 2 || !"INTENTOS".equals(respuesta.getArg(0))) {
            throw new Exception("Respuesta inesperada del servidor: " + respuesta.serializar());
        }
        return parsearEntero(respuesta.getArg(1), "numero de intentos");
    }

    @Override
    public synchronized String eliminarCliente(int idPuesto) throws Exception {
        Mensaje respuesta = enviarYRecibirMensaje(canalMensaje,
                new Mensaje(TipoMensaje.ELIMINAR_CLIENTE, String.valueOf(idPuesto)));
        return parsearTurnoOFilaVacia(respuesta);
    }

    @Override
    public synchronized void suscribirEventos(int idPuesto, IListenerEventosServidor listener) throws Exception {
        if (listener == null) {
            throw new IllegalArgumentException("listener nulo");
        }
        if (hiloCorriendo) {
            throw new IllegalStateException("ya hay una suscripcion activa");
        }
        // La suscripcion usa un canal PROPIO (segundo socket): el servidor lo
        // retiene para hacer push de eventos, asi que no puede compartirse con el
        // canal request/response.
        ICanalMensaje canal = crearCanal();
        Mensaje respuesta = enviarYRecibirMensaje(canal,
                new Mensaje(TipoMensaje.SUSCRIBIR_PUESTO, String.valueOf(idPuesto)));
        if (respuesta.getArgs().isEmpty() || !"SUSCRITO".equals(respuesta.getArg(0))) {
            canal.cerrar();
            throw new Exception("Fallo en la suscripcion del puesto: " + respuesta.serializar());
        }

        this.canalEventos = canal;
        this.hiloCorriendo = true;
        this.hiloSuscripcion = new Thread(() -> bucleEventos(canal, listener), "puesto-suscripcion");
        this.hiloSuscripcion.setDaemon(true);
        this.hiloSuscripcion.start();
    }

    @Override
    public synchronized void cerrarSuscripcion() {
        hiloCorriendo = false;
        // Cerrar el canal desbloquea el recibir() del bucle de eventos; interrumpir
        // solo no saca al hilo de un read bloqueante de socket.
        ICanalMensaje canal = canalEventos;
        if (canal != null) {
            canal.cerrar();
        }
        Thread hilo = hiloSuscripcion;
        if (hilo != null) {
            hilo.interrupt();
        }
        canalEventos = null;
        hiloSuscripcion = null;
    }

    /** Cierra la suscripcion (si la hay) y el canal de request/response. */
    public void cerrar() {
        cerrarSuscripcion();
        canalMensaje.cerrar();
    }

    private void bucleEventos(ICanalMensaje canal, IListenerEventosServidor listener) {
        while (hiloCorriendo) {
            Mensaje evento;
            try {
                evento = canal.recibir();
            } catch (IllegalArgumentException e) {
                continue; // linea mal formada: ignorar y seguir leyendo
            } catch (IOException e) {
                break; // socket cerrado/caido: termina la suscripcion
            }
            if (evento == null) {
                break; // el servidor cerro la conexion
            }
            if (evento.getTipo() == TipoMensaje.EVENTO_FILA_ACTUALIZADA && !evento.getArgs().isEmpty()) {
                try {
                    listener.actualizar(Integer.parseInt(evento.getArg(0)));
                } catch (NumberFormatException e) {
                    // payload mal formado: ignorar
                }
            }
        }
    }

    private String parsearTurnoOFilaVacia(Mensaje respuesta) throws Exception {
        if (respuesta.getArgs().isEmpty()) {
            throw new Exception("Respuesta inesperada del servidor: " + respuesta.serializar());
        }
        String sub = respuesta.getArg(0);
        if ("FILA_VACIA".equals(sub)) {
            return null;
        }
        if ("TURNO".equals(sub) && respuesta.getArgs().size() >= 2) {
            return respuesta.getArg(1);
        }
        throw new Exception("Respuesta inesperada del servidor: " + respuesta.serializar());
    }

    private int parsearEntero(String valor, String campo) throws Exception {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new Exception("El servidor devolvio un " + campo + " no numerico: " + valor);
        }
    }

    private void validarRespuesta(Mensaje resp) throws Exception {
        if (resp == null) {
            throw new Exception("El servidor cerro la conexion sin responder");
        }
        if (resp.getTipo() == TipoMensaje.ERROR) {
            String detalle = resp.getArgs().isEmpty() ? "(sin detalle)" : resp.getArg(0);
            throw new ErrorServidorException("Servidor respondio ERROR: " + detalle);
        }
        if (resp.getTipo() != TipoMensaje.OK) {
            throw new Exception("Tipo de respuesta inesperado: " + resp.getTipo());
        }
    }

    private Mensaje enviarYRecibirMensaje(ICanalMensaje canal, Mensaje mensaje) throws Exception {
        if (!canal.enviar(mensaje)) {
            throw new Exception("No se pudo enviar el mensaje al servidor");
        }
        Mensaje resp = canal.recibir();
        validarRespuesta(resp);
        return resp;
    }

    private void log(String mensaje) {
        System.out.println("[GestorConexion]: " + mensaje);
    }
}
