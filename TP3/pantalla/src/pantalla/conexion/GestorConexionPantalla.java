package pantalla.conexion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import pantalla.disponibilidad.Direccion;
import pantalla.interfaces.IConexionPantalla;
import pantalla.protocolo.Mensaje;
import pantalla.protocolo.TipoMensaje;

public class GestorConexionPantalla extends Thread {

    private static final String MSG_SUSCRIBIR = "SUSCRIBIR_PANTALLA";
    private static final int TIMEOUT_CONEXION_MS = 3000;

    private final String ip;
    private final int puerto;
    private final IConexionPantalla panelVisualizacion;

    private Socket socket;
    private BufferedReader in;

    public GestorConexionPantalla(Direccion direccion, IConexionPantalla panelVisualizacion) {
        this.ip = direccion.getIp();
        this.puerto = direccion.getPuerto();
        this.panelVisualizacion = panelVisualizacion;
        setDaemon(true);
    }

    public void conectar() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(this.ip, this.puerto), TIMEOUT_CONEXION_MS);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out.println(MSG_SUSCRIBIR);
    }

    public void detener() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void run() {
        try {
            String linea;
            while ((linea = in.readLine()) != null) {
                procesarLinea(linea);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (socket != null)
                    socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void procesarLinea(String linea) {
        Mensaje msj;
        try {
            msj = Mensaje.parse(linea);
        } catch (IllegalArgumentException ignored) {
            return;
        }
        try {
            switch (msj.getTipo()) {
                case EVENTO_LLAMADO:
                    if (msj.getArgs().size() >= 2)
                        panelVisualizacion.recibirLlamado(msj);
                    break;
                case EVENTO_RENOTIFICACION:
                    if (msj.getArgs().size() >= 2)
                        panelVisualizacion.recibirRenotificacion(msj);
                    break;
                case EVENTO_AUSENTE:
                    if (!msj.getArgs().isEmpty())
                        panelVisualizacion.recibirAusente(msj);
                    break;
                default:
                    break;
            }
        } catch (NumberFormatException ignored) {
        }
    }


}
