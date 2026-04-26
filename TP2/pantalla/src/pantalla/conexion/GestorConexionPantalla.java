package pantalla.conexion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import pantalla.interfaces.IConexionPantalla;

public class GestorConexionPantalla extends Thread {

    private static final String MSG_SUSCRIBIR = "SUSCRIBIR_PANTALLA";
    private static final int TIMEOUT_CONEXION_MS = 3000;

    private final String ip;
    private final int puerto;
    private final IConexionPantalla panelVisualizacion;

    private Socket socket;
    private BufferedReader in;

    public GestorConexionPantalla(String ip, int puerto, IConexionPantalla panelVisualizacion) {
        this.ip = ip;
        this.puerto = puerto;
        this.panelVisualizacion = panelVisualizacion;
        setDaemon(true);
    }

    public void conectar() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(ip, puerto), TIMEOUT_CONEXION_MS);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out.println(MSG_SUSCRIBIR);
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
            try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        }
    }

    private void procesarLinea(String linea) {
        String[] partes = linea.split("\\|", -1);
        if (partes.length < 2) {
            return;
        }
        String tipo = partes[0];
        try {
            switch (tipo) {
                case "EVENTO_LLAMADO":
                    if (partes.length >= 3) {
                        panelVisualizacion.recibirLlamado(partes[1], Integer.parseInt(partes[2]));
                    }
                    break;
                case "EVENTO_RENOTIFICACION":
                    if (partes.length >= 3) {
                        panelVisualizacion.recibirRenotificacion(partes[1], Integer.parseInt(partes[2]));
                    }
                    break;
                case "EVENTO_AUSENTE":
                    panelVisualizacion.recibirAusente(Integer.parseInt(partes[1]));
                    break;
                default:
                    break;
            }
        } catch (NumberFormatException ignored) {
        }
    }
}
