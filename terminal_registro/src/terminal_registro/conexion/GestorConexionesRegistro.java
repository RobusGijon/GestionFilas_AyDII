package terminal_registro.conexion;

import java.io.PrintWriter;
import java.net.Socket;
import terminal_registro.interfaces.IConexionRegistro;

public class GestorConexionesRegistro implements IConexionRegistro {

    @Override
    public void enviarMensaje(String ip, int puerto, String dni) throws Exception {
        Socket socket = new Socket(ip, puerto);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(dni);
        out.close();
        socket.close();
    }
}
