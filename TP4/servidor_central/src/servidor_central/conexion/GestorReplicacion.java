package servidor_central.conexion;

import java.io.IOException;
import java.net.SocketTimeoutException;

import servidor_central.disponibilidad.ConfigHA;
import servidor_central.disponibilidad.RolNodo;
import servidor_central.disponibilidad.SnapshotEstado;
import servidor_central.interfaces.ICanalMensaje;
import servidor_central.interfaces.IEstrategiaEncriptacion;
import servidor_central.interfaces.IGestorReplicacion;
import servidor_central.interfaces.IObservadorReplicacion;
import servidor_central.logging.ILogger;
import servidor_central.protocolo.CanalMensajes;
import servidor_central.protocolo.CanalMensajesEncriptados;
import servidor_central.protocolo.mensajes.Direccion;
import servidor_central.protocolo.mensajes.Mensaje;
import servidor_central.protocolo.mensajes.TipoMensaje;

public class GestorReplicacion implements IGestorReplicacion {

    private final ILogger log;
    private volatile RolNodo rol;
    private final Direccion par;
    private final IEstrategiaEncriptacion encriptacion;
    private final IObservadorReplicacion observador;

    private final SnapshotEstado.Builder snapshotBuilder = new SnapshotEstado.Builder();

    private volatile boolean corriendo;
    private volatile Runnable callbackSplitBrain;
    private Thread hiloMantenimiento;
    private final Object lockCanal = new Object();
    private ICanalMensaje canal;

    public GestorReplicacion(RolNodo rol, Direccion par, IEstrategiaEncriptacion encriptacion,
                              IObservadorReplicacion observador, ILogger log) {
        this.rol = rol;
        this.par = par;
        this.encriptacion = encriptacion;
        this.observador = observador;
        this.log = log;
    }

    public void iniciar() {
        corriendo = true;
        if (rol == RolNodo.PRIMARIO) {
            hiloMantenimiento = new Thread(this::bucleMantenimiento, "replicacion-conn");
            hiloMantenimiento.setDaemon(true);
            hiloMantenimiento.start();
            log.logInfo("[REPLICACION] mantenedor de conexion iniciado, par=" + par);
        } else {
            log.logInfo("[REPLICACION] handler de secundario listo");
        }
    }

    public void detener() {
        corriendo = false;
        if (hiloMantenimiento != null) hiloMantenimiento.interrupt();
        cerrarCanal();
        log.logInfo("[REPLICACION] detenido");
    }

    public void setCallbackSplitBrain(Runnable r) { this.callbackSplitBrain = r; }

    @Override
    public synchronized void cambiarRolAPrimario() {
        if (rol == RolNodo.PRIMARIO) return;
        log.logInfo("[REPLICACION] cambio de rol SECUNDARIO -> PRIMARIO");
        rol = RolNodo.PRIMARIO;
        hiloMantenimiento = new Thread(this::bucleMantenimiento, "replicacion-conn");
        hiloMantenimiento.setDaemon(true);
        hiloMantenimiento.start();
        log.logInfo("[REPLICACION] mantenedor de conexion iniciado, par=" + par);
    }

    @Override
    public synchronized void cambiarRolASecundario() {
        if (rol == RolNodo.SECUNDARIO) return;
        log.logInfo("[REPLICACION] cambio de rol PRIMARIO -> SECUNDARIO");
        rol = RolNodo.SECUNDARIO;
        cerrarCanal();
        if (hiloMantenimiento != null) {
            hiloMantenimiento.interrupt();
            hiloMantenimiento = null;
        }
        snapshotBuilder.reset();
        log.logInfo("[REPLICACION] handler de secundario listo");
    }

    public void manejarMensajeRecibido(Mensaje m) {
        switch (m.getTipo()) {
            case HOLA_PAR:
                log.logInfo("[REPLICACION] HOLA_PAR recibido del primario");
                break;
            case SNAPSHOT:
                if (snapshotBuilder.procesar(m)) {
                    SnapshotEstado snap = snapshotBuilder.build();
                    observador.onSnapshotRecibido(snap);
                    log.logInfo("[REPLICACION] snapshot aplicado: "
                        + snap.getFila().size() + " en fila, "
                        + snap.getLlamados().size() + " llamados, "
                        + snap.getPuestos().size() + " puestos, "
                        + "proximoIdPuesto=" + snap.getProximoIdPuesto());
                }
                break;
            case REPLICAR:
                observador.onReplicarRecibido(m);
                break;
            default:
                break;
        }
    }

    @Override
    public void replicar(Mensaje m) {
        ICanalMensaje c;
        synchronized (lockCanal) {
            c = canal;
        }
        // Si el canal no esta vivo, no dejamos que enviar() lo reconecte solo: la
        // reconexion debe pasar por el handshake completo (HOLA + snapshot) que
        // hace bucleMantenimiento, para no mandarle un REPLICAR suelto a un par
        // que todavia no tiene el estado base.
        if (c == null || !c.estaVivo()) {
            cerrarCanal();
            return;
        }
        if (!c.enviar(m)) {
            cerrarCanal();
        }
    }

    private void bucleMantenimiento() {
        boolean caidaYaLogueada = false;
        while (corriendo) {
            if (canalActivo()) {
                caidaYaLogueada = false;
                try {
                    Thread.sleep(ConfigHA.HEARTBEAT_RECONEXION_PAUSA_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }
            try {
                conectarYEnviarSnapshot();
            } catch (IOException e) {
                if (corriendo && !caidaYaLogueada) {
                    log.logInfo("[REPLICACION] sin conexion al par "
                        + par + " (" + e.getMessage() + "), reintentando cada "
                        + ConfigHA.HEARTBEAT_RECONEXION_PAUSA_MS + "ms");
                    caidaYaLogueada = true;
                }
                cerrarCanal();
            }
            if (corriendo) {
                try {
                    Thread.sleep(ConfigHA.HEARTBEAT_RECONEXION_PAUSA_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void conectarYEnviarSnapshot() throws IOException {
        ICanalMensaje c = crearCanal();
        c.getSocket().setSoTimeout(ConfigHA.SOCKET_TIMEOUT_MS);

        c.enviar(new Mensaje(TipoMensaje.HOLA_PAR, "PRIMARIO"));
        boolean otroEsPrimario = false;
        try {
            Mensaje rm = c.recibir();
            if (rm != null
                    && rm.getTipo() == TipoMensaje.ERROR
                    && !rm.getArgs().isEmpty()
                    && "YA_HAY_PRIMARIO".equals(rm.getArg(0))) {
                otroEsPrimario = true;
            }
        } catch (SocketTimeoutException e) {
            // normal: un secundario no responde al HOLA_PAR
        } catch (IllegalArgumentException e) {
            // respuesta no parseable: la ignoramos
        }

        if (otroEsPrimario) {
            log.logInfo("[REPLICACION] par responde YA_HAY_PRIMARIO - autodemovere a SECUNDARIO");
            c.cerrar();
            Runnable cb = callbackSplitBrain;
            if (cb != null) {
                try { cb.run(); }
                catch (RuntimeException e) { log.logError("CALLBACK_SPLIT_BRAIN", e); }
            }
            return;
        }

        synchronized (lockCanal) {
            this.canal = c;
        }
        log.logInfo("[REPLICACION] conectado al par " + par);

        observador.onParConectado();
    }

    private ICanalMensaje crearCanal() throws IOException {
        return encriptacion == null
                ? new CanalMensajes(par)
                : new CanalMensajesEncriptados(par, encriptacion);
    }

    private boolean canalActivo() {
        synchronized (lockCanal) {
            return canal != null && canal.estaVivo();
        }
    }

    private void cerrarCanal() {
        synchronized (lockCanal) {
            if (canal != null) {
                canal.cerrar();
                canal = null;
            }
        }
        observador.onParDesconectado();
    }
}
