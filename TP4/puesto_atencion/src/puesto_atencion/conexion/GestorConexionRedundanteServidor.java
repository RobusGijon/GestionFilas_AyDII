package puesto_atencion.conexion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import puesto_atencion.disponibilidad.ConfigHA;
import puesto_atencion.disponibilidad.Direccion;
import puesto_atencion.dominio.EstadoReconexion;
import puesto_atencion.interfaces.ICanalMensaje;
import puesto_atencion.interfaces.IEstrategiaEncriptacion;
import puesto_atencion.interfaces.IGestorConexionServidor;
import puesto_atencion.interfaces.IListenerEventosServidor;
import puesto_atencion.protocolo.CanalMensajes;
import puesto_atencion.protocolo.CanalMensajesEncriptados;
import puesto_atencion.protocolo.mensajes.Mensaje;
import puesto_atencion.protocolo.mensajes.TipoMensaje;


public class GestorConexionRedundanteServidor implements IGestorConexionServidor {

    private final List<Direccion> direcciones;
    private final IEstrategiaEncriptacion encriptacion;

    // Nodos request/response alineados por indice con 'direcciones'. Se crean
    // bajo demanda (cada uno abre un canal persistente) y se descartan al fallar
    // para forzar reconexion en el proximo intento. Asi el failover funciona
    // aunque un nodo este caido al arrancar.
    private final GestorConexionServidor[] gestoresConexion;
    
    private volatile int activo = 0;
    private volatile boolean suscripcionCorriendo;
    private volatile int idPuestoSuscripcion;
    private volatile IListenerEventosServidor listenerSuscripcion;
    private volatile ICanalMensaje canalSuscripcionActivo;
    private Thread hiloSuscripcion;
    // En la primera suscripcion (recien conectado) no hay que reclamar: el estado
    // ya se obtuvo en conectarPuesto. En las siguientes (reconexion) si.
    private volatile boolean primeraSuscripcion;

    public GestorConexionRedundanteServidor(Direccion primaria, Direccion secundaria, IEstrategiaEncriptacion encriptacion) {
        if (primaria == null) {
            throw new IllegalArgumentException("direccion primaria requerida");
        }
        this.encriptacion = encriptacion;
        this.direcciones = new ArrayList<>();
        this.direcciones.add(primaria);
        if (secundaria != null) {
            this.direcciones.add(secundaria);
        }
        this.gestoresConexion = new GestorConexionServidor[this.direcciones.size()];
    }

    @Override
    public EstadoReconexion conectarPuesto(int idDeseado, IListenerEventosServidor listener) throws Exception {
        // Los nodos solo hacen request/response (listener=null); la suscripcion la
        // maneja este redundante con su propio bucle de failover (bucleSuscripcion).
        EstadoReconexion estado = ejecutarConFailover("CONECTAR_PUESTO", g -> g.conectarPuesto(idDeseado, null));
        this.idPuestoSuscripcion = estado.getIdPuesto();
        if (listener != null) {
            suscribirEventos(estado.getIdPuesto(), listener);
        }
        return estado;
    }

    @Override
    public String llamarSiguiente(int idPuesto) throws Exception {
        return ejecutarConFailover("LLAMAR_SIGUIENTE", g -> g.llamarSiguiente(idPuesto));
    }

    @Override
    public int reNotificar(int idPuesto) throws Exception {
        return ejecutarConFailover("RENOTIFICAR", g -> g.reNotificar(idPuesto));
    }

    @Override
    public String eliminarCliente(int idPuesto) throws Exception {
        return ejecutarConFailover("ELIMINAR_CLIENTE", g -> g.eliminarCliente(idPuesto));
    }

    @Override
    public synchronized void suscribirEventos(int idPuesto, IListenerEventosServidor listener) {
        if (listener == null)
            throw new IllegalArgumentException("listener nulo");
        if (suscripcionCorriendo)
            throw new IllegalStateException("ya hay suscripcion activa");
        this.idPuestoSuscripcion = idPuesto;
        this.listenerSuscripcion = listener;
        this.suscripcionCorriendo = true;
        this.primeraSuscripcion = true;
        this.hiloSuscripcion = new Thread(this::bucleSuscripcion, "puesto-suscripcion-redundante");
        this.hiloSuscripcion.setDaemon(true);
        this.hiloSuscripcion.start();
    }

    @Override
    public synchronized void cerrarSuscripcion() {
        suscripcionCorriendo = false;
        // Cerrar el canal activo desbloquea el recibir() del bucle de eventos.
        ICanalMensaje canal = canalSuscripcionActivo;
        if (canal != null) {
            canal.cerrar();
        }
        if (hiloSuscripcion != null) {
            hiloSuscripcion.interrupt();
        }
    }

    @FunctionalInterface
    private interface OpCliente<T> {
        T apply(IGestorConexionServidor g) throws Exception;
    }

    private <T> T ejecutarConFailover(String op, OpCliente<T> fn) throws Exception {
        Exception ultima = null;
        int n = direcciones.size();   
        int inicio = activo;
        for (int idxIntento = 0; idxIntento < n; idxIntento++) {
            int idx = (inicio + idxIntento) % n;
            for (int intento = 0; intento < ConfigHA.RETRY_MAX_INTENTOS; intento++) {
                try {
                    T resultado = fn.apply(obtenerNodo(idx));
                    if (idx != activo) {
                        log("op " + op + " OK contra " + direcciones.get(idx)
                                + " - failover desde " + direcciones.get(activo));
                        activo = idx;
                    }
                    return resultado;
                } catch (ErrorServidorException e) {
                    // El nodo respondio (esta sano): es un error de negocio, no de
                    // transporte. No tiene sentido reintentar ni hacer failover; lo
                    // propagamos de inmediato.
                    throw e;
                } catch (Exception e) {
                    // El canal persistente quedo inservible: lo descartamos para
                    // reconectar fresco en el proximo intento.
                    descartarNodo(idx);
                    ultima = e;
                    if (intento < ConfigHA.RETRY_MAX_INTENTOS - 1) {
                        try {
                            Thread.sleep(ConfigHA.RETRY_BACKOFF_MS[intento]);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw e;
                        }
                    }
                }
            }
            log("op " + op + " agotado en " + direcciones.get(idx)
                    + " (" + ConfigHA.RETRY_MAX_INTENTOS + " intentos)"
                    + (idxIntento < n - 1 ? " - probando otro nodo" : " - sin mas nodos"));
        }
        throw ultima;
    }

    private synchronized GestorConexionServidor obtenerNodo(int idx) throws IOException {
        if (gestoresConexion[idx] == null) {
            gestoresConexion[idx] = new GestorConexionServidor(direcciones.get(idx), encriptacion);
        }
        return gestoresConexion[idx];
    }

    private synchronized void descartarNodo(int idx) {
        GestorConexionServidor n = gestoresConexion[idx];
        gestoresConexion[idx] = null;
        if (n != null) {
            n.cerrar();
        }
    }

    private ICanalMensaje crearCanal(Direccion d) throws IOException {
        return encriptacion == null
                ? new CanalMensajes(d)
                : new CanalMensajesEncriptados(d, encriptacion);
    }

    private void bucleSuscripcion() {
        log("suscripcion supervisora iniciada, nodos=" + direcciones);
        while (suscripcionCorriendo) {
            int n = direcciones.size();
            boolean alguienConecto = false;
            for (int idxIntento = 0; idxIntento < n && suscripcionCorriendo; idxIntento++) {
                int idx = (activo + idxIntento) % n;
                Direccion d = direcciones.get(idx);
                ICanalMensaje canal = null;
                try {
                    canal = crearCanal(d);
                    canalSuscripcionActivo = canal;

                    if (!canal.enviar(new Mensaje(TipoMensaje.SUSCRIBIR_PUESTO, String.valueOf(idPuestoSuscripcion)))) {
                        continue;
                    }
                    Mensaje resp = canal.recibir();
                    if (resp == null) {
                        continue;
                    }
                    if (resp.getTipo() != TipoMensaje.OK
                            || resp.getArgs().isEmpty()
                            || !"SUSCRITO".equals(resp.getArg(0))) {
                        continue;
                    }
                    if (idx != activo) {
                        log("suscripcion: failover " + direcciones.get(activo) + " -> " + d);
                        activo = idx;
                    } else {
                        log("suscripcion conectada a " + d);
                    }
                    alguienConecto = true;
                    // La suscripcion (re)conecto: si NO es la primera vez, el
                    // servidor pudo haber reiniciado y dejado el slot DESASIGNADO.
                    // Reenviamos CONECTAR_PUESTO|<id> por el canal request/response
                    // para re-reclamar el mismo id (reclamo-por-id) antes de que otro
                    // operador pueda tomarlo.
                    if (!primeraSuscripcion) {
                        reclamarPuesto();
                    }
                    primeraSuscripcion = false;
                    bucleEventos(canal);
                    // Si bucleEventos retorna sin excepcion: el servidor cerro.
                } catch (IOException e) {
                    // probar el otro nodo
                } finally {
                    canalSuscripcionActivo = null;
                    if (canal != null) {
                        canal.cerrar();
                    }
                }
                if (!suscripcionCorriendo) {
                    break;
                }
            }
            if (!alguienConecto && suscripcionCorriendo) {
                try {
                    Thread.sleep(ConfigHA.SUSCRIPCION_PAUSA_REINTENTO_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log("suscripcion supervisora detenida");
    }

    /**
     * Re-reclama el slot del puesto tras una reconexion, reenviando
     * CONECTAR_PUESTO|&lt;id&gt; por el canal request/response (con failover). El
     * estado devuelto se descarta: el puesto vivo ya tiene su dni/intentos/historial
     * en memoria; lo unico que importa es que el servidor vuelva a marcar el slot
     * como ASIGNADO con el mismo id.
     */
    private void reclamarPuesto() {
        try {
            ejecutarConFailover("RECLAMAR", g -> g.conectarPuesto(idPuestoSuscripcion, null));
            log("slot " + idPuestoSuscripcion + " re-reclamado tras reconexion");
        } catch (Exception e) {
            log("no se pudo re-reclamar el slot " + idPuestoSuscripcion + ": " + e.getMessage());
        }
    }

    private void bucleEventos(ICanalMensaje canal) throws IOException {
        while (suscripcionCorriendo) {
            Mensaje ev;
            try {
                ev = canal.recibir();
            } catch (IllegalArgumentException e) {
                continue; // linea mal formada: ignorar y seguir leyendo
            }
            if (ev == null) {
                break; // el servidor cerro la conexion
            }
            if (ev.getTipo() == TipoMensaje.EVENTO_FILA_ACTUALIZADA && !ev.getArgs().isEmpty()) {
                try {
                    int n = Integer.parseInt(ev.getArg(0));
                    listenerSuscripcion.actualizar(n);
                } catch (NumberFormatException ex) {
                    // ignorar payload mal formado
                }
            }
        }
    }

    private void log(String msg) {
        System.out.println("[PUESTO] " + msg);
    }

    @Override
    public String toString() {
        return "GestorConexionRedundanteServidor[" + Arrays.toString(direcciones.toArray()) + "]";
    }
}
