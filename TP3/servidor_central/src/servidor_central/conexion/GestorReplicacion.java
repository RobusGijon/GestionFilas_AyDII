package servidor_central.conexion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

import servidor_central.disponibilidad.ConfigHA;
import servidor_central.disponibilidad.DireccionPar;
import servidor_central.disponibilidad.RolNodo;
import servidor_central.disponibilidad.SnapshotEstado;
import servidor_central.interfaces.IGestorReplicacion;
import servidor_central.interfaces.IObservadorReplicacion;
import servidor_central.logging.ILogger;
import servidor_central.protocolo.Mensaje;
import servidor_central.protocolo.TipoMensaje;

public class GestorReplicacion implements IGestorReplicacion {

    private final ILogger log;
    private volatile RolNodo rol;
    private final DireccionPar par;
    private final IObservadorReplicacion observador;

    private final SnapshotEstado.Builder snapshotBuilder = new SnapshotEstado.Builder();

    private volatile boolean corriendo;
    private volatile Runnable callbackSplitBrain;
    private Thread hiloMantenimiento;
    private final Object lockSocket = new Object();
    private Socket socket;
    private PrintWriter out;

    public GestorReplicacion(RolNodo rol, DireccionPar par,
                              IObservadorReplicacion observador, ILogger log) {
        this.rol = rol;
        this.par = par;
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
        cerrarSocket();
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
        cerrarSocket();
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
        PrintWriter w;
        synchronized (lockSocket) {
            w = out;
        }
        if (w == null) return;
        w.println(m.serializar());
        if (w.checkError()) {
            cerrarSocket();
        }
    }

    private void bucleMantenimiento() {
        boolean caidaYaLogueada = false;
        while (corriendo) {
            if (socketActivo()) {
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
                cerrarSocket();
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
        Socket s = new Socket(par.getIp(), par.getPuerto());
        s.setSoTimeout(ConfigHA.SOCKET_TIMEOUT_MS);
        PrintWriter w = new PrintWriter(s.getOutputStream(), true);
        BufferedReader rdr = new BufferedReader(new InputStreamReader(s.getInputStream()));

        w.println(new Mensaje(TipoMensaje.HOLA_PAR, "PRIMARIO").serializar());
        boolean otroEsPrimario = false;
        try {
            String resp = rdr.readLine();
            if (resp != null) {
                Mensaje rm = Mensaje.parse(resp);
                if (rm.getTipo() == TipoMensaje.ERROR
                        && !rm.getArgs().isEmpty()
                        && "YA_HAY_PRIMARIO".equals(rm.getArg(0))) {
                    otroEsPrimario = true;
                }
            }
        } catch (SocketTimeoutException e) {
        } catch (IllegalArgumentException e) {
        }

        if (otroEsPrimario) {
            log.logInfo("[REPLICACION] par responde YA_HAY_PRIMARIO - autodemovere a SECUNDARIO");
            try { s.close(); } catch (IOException e) { /* ignore */ }
            Runnable cb = callbackSplitBrain;
            if (cb != null) {
                try { cb.run(); }
                catch (RuntimeException e) { log.logError("CALLBACK_SPLIT_BRAIN", e); }
            }
            return;
        }

        synchronized (lockSocket) {
            this.socket = s;
            this.out = w;
        }
        log.logInfo("[REPLICACION] conectado al par " + par);

        observador.onParConectado();
    }

    private boolean socketActivo() {
        synchronized (lockSocket) {
            return socket != null && !socket.isClosed() && socket.isConnected();
        }
    }

    private void cerrarSocket() {
        synchronized (lockSocket) {
            if (out != null) {
                try { out.close(); } catch (Exception e) { }
                out = null;
            }
            if (socket != null && !socket.isClosed()) {
                try { socket.close(); } catch (IOException e) { }
            }
            socket = null;
        }
        observador.onParDesconectado();
    }
}
