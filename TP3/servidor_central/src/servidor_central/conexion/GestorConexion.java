package servidor_central.conexion;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import servidor_central.interfaces.ICoordinadorServidor;
import servidor_central.interfaces.IPantalla;
import servidor_central.logging.ILogger;

public class GestorConexion {

    private final int puerto;
    private final ICoordinadorServidor coordinador;
    private final IPantalla pub;
    private final ILogger log;

    private ServerSocket serverSocket;
    private ExecutorService pool;
    private volatile boolean corriendo;

    public GestorConexion(int puerto, ICoordinadorServidor coordinador, IPantalla pub, ILogger log) {
        this.puerto = puerto;
        this.coordinador = coordinador;
        this.pub = pub;
        this.log = log;
    }

    public void iniciar() {
        try {
            this.serverSocket = new ServerSocket(puerto);
            this.pool = Executors.newCachedThreadPool();
            this.corriendo = true;
            log.logInfo("ServerSocket escuchando en puerto " + puerto);

            while (corriendo) {
                try {
                    Socket cliente = serverSocket.accept();
                    log.logInfo("Conexion aceptada desde "
                            + cliente.getInetAddress().getHostAddress()
                            + ":" + cliente.getPort());
                    pool.submit(new ManejadorConexion(cliente, coordinador, pub, log));
                } catch (IOException e) {
                    if (corriendo) {
                        log.logError("ACCEPT_LOOP", e);
                    }
                }
            }
        } catch (IOException e) {
            log.logError("INICIAR_SERVIDOR", e);
        } finally {
            detener();
        }
    }

    public void detener() {
        corriendo = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.logError("DETENER_SERVIDOR", e);
        }
        if (pool != null) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.logInfo("Servidor detenido");
    }
}
