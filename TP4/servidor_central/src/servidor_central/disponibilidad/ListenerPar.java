package servidor_central.disponibilidad;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

import servidor_central.interfaces.ICanalMensaje;
import servidor_central.interfaces.IEstrategiaEncriptacion;
import servidor_central.logging.ILogger;
import servidor_central.protocolo.CanalMensajes;
import servidor_central.protocolo.CanalMensajesEncriptados;
import servidor_central.protocolo.mensajes.Mensaje;

/**
 * Acepta conexiones par-a-par en el puerto del nodo secundario y enrutea cada
 * mensaje recibido a un handler segun su TipoMensaje:
 *   HEARTBEAT                       -> handlerHeartbeat
 *   HOLA_PAR / SNAPSHOT / REPLICAR  -> handlerReplicacion
 *
 * No interpreta el contenido: solo demultiplexa. Cada conexion entrante se
 * envuelve en un {@link ICanalMensaje} que se encarga del framing y, si hay
 * estrategia configurada, de desencriptar cada linea recibida del par.
 */
public class ListenerPar {

    private final int puerto;
    private final IEstrategiaEncriptacion encriptacion;
    private final ILogger log;

    private volatile Consumer<Mensaje> handlerHeartbeat;
    private volatile Consumer<Mensaje> handlerReplicacion;

    private volatile boolean corriendo;
    private ServerSocket serverSocket;
    private Thread hiloAccept;

    public ListenerPar(int puerto, IEstrategiaEncriptacion encriptacion, ILogger log) {
        this.puerto = puerto;
        this.encriptacion = encriptacion;
        this.log = log;
    }

    public void setHandlerHeartbeat(Consumer<Mensaje> h)   { this.handlerHeartbeat = h; }
    public void setHandlerReplicacion(Consumer<Mensaje> h) { this.handlerReplicacion = h; }

    public void iniciar() {
        try {
            serverSocket = new ServerSocket(puerto);
        } catch (IOException e) {
            log.logError("LISTENER_PAR_BIND", e);
            return;
        }
        corriendo = true;
        hiloAccept = new Thread(this::bucleAccept, "listener-par-accept");
        hiloAccept.setDaemon(true);
        hiloAccept.start();
        log.logInfo("[LISTENER_PAR] escuchando en puerto " + puerto);
    }

    public void detener() {
        corriendo = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try { serverSocket.close(); } catch (IOException e) { /* ignore */ }
        }
    }

    private void bucleAccept() {
        while (corriendo) {
            try {
                final Socket s = serverSocket.accept();
                Thread t = new Thread(() -> manejar(s), "listener-par-conn");
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                if (corriendo) log.logInfo("[LISTENER_PAR] error en accept: " + e.getMessage());
            }
        }
    }

    private void manejar(Socket s) {
        log.logInfo("[LISTENER_PAR] conexion entrante de "
            + s.getInetAddress().getHostAddress() + ":" + s.getPort());
        ICanalMensaje canal;
        try {
            canal = encriptacion == null
                    ? new CanalMensajes(s)
                    : new CanalMensajesEncriptados(s, encriptacion);
        } catch (IOException e) {
            try { s.close(); } catch (IOException ex) { /* ignore */ }
            log.logInfo("[LISTENER_PAR] no se pudo abrir el canal entrante: " + e.getMessage());
            return;
        }
        try {
            while (corriendo) {
                Mensaje m;
                try {
                    m = canal.recibir();
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                if (m == null) {
                    break;
                }
                if (!dispatch(m)) {
                    break;
                }
            }
        } catch (IOException e) {
            // conexion cerrada o cortada
        } finally {
            canal.cerrar();
            log.logInfo("[LISTENER_PAR] conexion cerrada");
        }
    }

    private boolean dispatch(Mensaje m) {
        switch (m.getTipo()) {
            case HEARTBEAT:
                if (handlerHeartbeat != null) handlerHeartbeat.accept(m);
                return true;
            case HOLA_PAR:
            case SNAPSHOT:
            case REPLICAR:
                if (handlerReplicacion != null) handlerReplicacion.accept(m);
                return true;
            default:
                log.logInfo("[LISTENER_PAR] mensaje inesperado: " + m.getTipo() + " - cerrando conexion");
                return false;
        }
    }
}
