package puesto_atencion.conexion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import puesto_atencion.interfaces.IGestorConexionServidor;
import puesto_atencion.protocolo.Mensaje;
import puesto_atencion.protocolo.TipoMensaje;

public class GestorConexionServidor implements IGestorConexionServidor {

    private final String ipServidor;
    private final int puertoServidor;

    public GestorConexionServidor(String ipServidor, int puertoServidor) {
        this.ipServidor = ipServidor;
        this.puertoServidor = puertoServidor;
    }

    @Override
    public int conectarPuesto() throws Exception {
        Mensaje resp = enviar(new Mensaje(TipoMensaje.CONECTAR_PUESTO));
        validarOk(resp);
        if (resp.getArgs().size() < 2 || !"ID_PUESTO".equals(resp.getArg(0))) {
            throw new Exception("Respuesta inesperada del servidor: " + resp.serializar());
        }
        return Integer.parseInt(resp.getArg(1));
    }

    @Override
    public String llamarSiguiente(int idPuesto) throws Exception {
        Mensaje resp = enviar(new Mensaje(TipoMensaje.LLAMAR_SIGUIENTE, String.valueOf(idPuesto)));
        return parsearTurnoOFilaVacia(resp);
    }

    @Override
    public int reNotificar(int idPuesto) throws Exception {
        Mensaje resp = enviar(new Mensaje(TipoMensaje.RENOTIFICAR, String.valueOf(idPuesto)));
        validarOk(resp);
        if (resp.getArgs().size() < 2 || !"INTENTOS".equals(resp.getArg(0))) {
            throw new Exception("Respuesta inesperada del servidor: " + resp.serializar());
        }
        return Integer.parseInt(resp.getArg(1));
    }

    @Override
    public String eliminarCliente(int idPuesto) throws Exception {
        Mensaje resp = enviar(new Mensaje(TipoMensaje.ELIMINAR_CLIENTE, String.valueOf(idPuesto)));
        return parsearTurnoOFilaVacia(resp);
    }

    private String parsearTurnoOFilaVacia(Mensaje resp) throws Exception {
        validarOk(resp);
        if (resp.getArgs().isEmpty()) {
            throw new Exception("Respuesta inesperada del servidor: " + resp.serializar());
        }
        String sub = resp.getArg(0);
        if ("FILA_VACIA".equals(sub)) {
            return null;
        }
        if ("TURNO".equals(sub) && resp.getArgs().size() >= 2) {
            return resp.getArg(1);
        }
        throw new Exception("Respuesta inesperada del servidor: " + resp.serializar());
    }

    private void validarOk(Mensaje resp) throws Exception {
        if (resp.getTipo() == TipoMensaje.ERROR) {
            String detalle = resp.getArgs().isEmpty() ? "(sin detalle)" : resp.getArg(0);
            throw new Exception("Servidor respondio ERROR: " + detalle);
        }
        if (resp.getTipo() != TipoMensaje.OK) {
            throw new Exception("Tipo de respuesta inesperado: " + resp.getTipo());
        }
    }

    private Mensaje enviar(Mensaje req) throws Exception {
        try (Socket socket = new Socket(ipServidor, puertoServidor);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(req.serializar());
            String linea = in.readLine();
            if (linea == null) {
                throw new Exception("El servidor cerro la conexion sin responder");
            }
            return Mensaje.parse(linea);
        }
    }
}
