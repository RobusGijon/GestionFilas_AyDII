package puesto_atencion.conexion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;
import puesto_atencion.interfaces.IConexionPuesto;
import puesto_atencion.interfaces.IManejoFila;

public class GestorConexionesAtencion extends Thread implements IConexionPuesto {

    private int puertoEscucha;
    private String ipMonitor;
    private int puertoMonitor;
    private IManejoFila manejadorFila;
    private Consumer<String> onClienteAgregado;

    public GestorConexionesAtencion(int puertoEscucha, String ipMonitor, int puertoMonitor,
                                     IManejoFila manejadorFila, Consumer<String> onClienteAgregado) {
        this.puertoEscucha = puertoEscucha;
        this.ipMonitor = ipMonitor;
        this.puertoMonitor = puertoMonitor;
        this.manejadorFila = manejadorFila;
        this.onClienteAgregado = onClienteAgregado;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(puertoEscucha);
            while (true) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String dni = in.readLine();
                if (dni != null && !dni.isEmpty()) {
                    manejadorFila.agregarCliente(dni);
                    if (onClienteAgregado != null) {
                        onClienteAgregado.accept(dni);
                    }
                }
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void enviarDNI(String dni) throws Exception {
        Socket socket = new Socket(ipMonitor, puertoMonitor);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println("LLAMADO:" + dni);
        out.close();
        socket.close();
    }
}
