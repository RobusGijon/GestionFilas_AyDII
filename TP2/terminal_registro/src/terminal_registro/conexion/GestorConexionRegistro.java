package terminal_registro.conexion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import terminal_registro.interfaces.IConexionRegistro;
import terminal_registro.protocolo.Mensaje;
import terminal_registro.protocolo.TipoMensaje;

public class GestorConexionRegistro implements IConexionRegistro {

    private final String ipServidor;
    private final int puertoServidor;

    public GestorConexionRegistro(String ipServidor, int puertoServidor) {
        this.ipServidor = ipServidor;
        this.puertoServidor = puertoServidor;
    }

    @Override
    public ResultadoRegistro registrar(String dni) {
        try (Socket socket = new Socket(ipServidor, puertoServidor);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(new Mensaje(TipoMensaje.REGISTRAR_CLIENTE, dni).serializar());

            String linea = in.readLine();
            if (linea == null) {
                return ResultadoRegistro.ERROR_CONEXION;
            }

            Mensaje resp = Mensaje.parse(linea);
            if (resp.getTipo() == TipoMensaje.OK) {
                return ResultadoRegistro.REGISTRADO;
            }
            if (resp.getTipo() == TipoMensaje.ERROR
                    && !resp.getArgs().isEmpty()
                    && "DUPLICADO".equals(resp.getArg(0))) {
                return ResultadoRegistro.DUPLICADO;
            }
            return ResultadoRegistro.ERROR_CONEXION;

        } catch (Exception e) {
            return ResultadoRegistro.ERROR_CONEXION;
        }
    }
}