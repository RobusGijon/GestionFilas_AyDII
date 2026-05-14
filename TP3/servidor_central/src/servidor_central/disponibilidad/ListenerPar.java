package servidor_central.disponibilidad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

import servidor_central.logging.ILogger;
import servidor_central.protocolo.Mensaje;

/**
 * Acepta conexiones par-a-par en el puerto del nodo secundario y enrutea cada
 * mensaje recibido a un handler segun su TipoMensaje:
 *   HEARTBEAT                       -> handlerHeartbeat
 *   HOLA_PAR / SNAPSHOT / REPLICAR  -> handlerReplicacion
 *
 * No interpreta el contenido: solo demultiplexa.
 */
public class ListenerPar {

    private final int puerto;
    private final ILogger log;

    private volatile Consumer<Mensaje> handlerHeartbeat;
    private volatile Consumer<Mensaje> handlerReplicacion;

    private volatile boolean corriendo;
    private ServerSocket serverSocket;
    private Thread hiloAccept;

    public ListenerPar(int puerto, ILogger log) {
        this.puerto = puerto;
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
        try (BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            String linea;
            while (corriendo && (linea = in.readLine()) != null) {
                Mensaje m;
                try {
                    m = Mensaje.parse(linea);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                if (!dispatch(m)) {
                    break;
                }
            }
        } catch (IOException e) {
            // conexion cerrada o cortada
        } finally {
            try { s.close(); } catch (IOException e) { /* ignore */ }
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
