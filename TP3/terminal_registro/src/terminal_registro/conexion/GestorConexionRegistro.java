package terminal_registro.conexion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import terminal_registro.disponibilidad.ConfigHA;
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
    public boolean estaElServerRespondiendo(String ipServidor, int puertoServidor) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ipServidor, puertoServidor),
                    ConfigHA.SOCKET_CONNECT_TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public ResultadoRegistro registrar(String dni) {
        try (Socket socket = new Socket(ipServidor, puertoServidor);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Mensaje msjRegistrarDNI = new Mensaje(TipoMensaje.REGISTRAR_CLIENTE, dni);

            out.println(msjRegistrarDNI.serializar());

            String linea = in.readLine();
            if (linea == null) {
                return ResultadoRegistro.ERROR_CONEXION;
            }

            Mensaje respuestaServidor = Mensaje.parse(linea);
            if (respuestaServidor.getTipo() == TipoMensaje.OK) {
                return ResultadoRegistro.REGISTRADO;
            }

            if (respuestaServidor.getTipo() == TipoMensaje.ERROR
                    && !respuestaServidor.getArgs().isEmpty()
                    && "DUPLICADO".equals(respuestaServidor.getArg(0))) {
                return ResultadoRegistro.DUPLICADO;
            }
            return ResultadoRegistro.ERROR_CONEXION;
        } catch (Exception e) {
            return ResultadoRegistro.ERROR_CONEXION;
        }
    }

}