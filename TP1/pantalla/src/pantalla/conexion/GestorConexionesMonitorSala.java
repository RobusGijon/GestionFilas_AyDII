package pantalla.conexion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import pantalla.interfaces.IConexionPantalla;

public class GestorConexionesMonitorSala extends Thread {

    private int puerto;
    private IConexionPantalla panelVisualizacion;

    public GestorConexionesMonitorSala(int puerto, IConexionPantalla panelVisualizacion) {
        this.puerto = puerto;
        this.panelVisualizacion = panelVisualizacion;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(puerto);
            while (true) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String mensaje = in.readLine();
                if (mensaje != null && mensaje.startsWith("LLAMADO:")) {
                    String dni = mensaje.substring(8);
                    panelVisualizacion.enviarDNI(dni);
                }
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
