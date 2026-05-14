package puesto_atencion.conexion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import puesto_atencion.disponibilidad.ConfigHA;
import puesto_atencion.interfaces.IGestorConexionServidor;
import puesto_atencion.interfaces.IListenerEventosServidor;
import puesto_atencion.protocolo.Mensaje;
import puesto_atencion.protocolo.TipoMensaje;

public class GestorConexionServidor implements IGestorConexionServidor {

    private final String ipServidor;
    private final int puertoServidor;

    private Socket socketEventos;
    private Thread hiloEventos;

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

    @Override
    public synchronized void suscribirEventos(IListenerEventosServidor listener) throws Exception {
        if (listener == null) {
            throw new IllegalArgumentException("listener nulo");
        }
        if (socketEventos != null) {
            throw new IllegalStateException("Ya hay una suscripcion activa");
        }
        Socket s = new Socket();
        s.connect(new InetSocketAddress(ipServidor, puertoServidor),
                ConfigHA.SOCKET_CONNECT_TIMEOUT_MS);
        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

        out.println(new Mensaje(TipoMensaje.SUSCRIBIR_PUESTO).serializar());
        String confirmacion = in.readLine();
        if (confirmacion == null) {
            cerrarSilencioso(s);
            throw new Exception("El servidor cerro la conexion sin confirmar la suscripcion");
        }
        Mensaje respuesta = Mensaje.parse(confirmacion);
        if (respuesta.getTipo() != TipoMensaje.OK || respuesta.getArgs().isEmpty() || !"SUSCRITO".equals(respuesta.getArg(0))) {
            cerrarSilencioso(s);
            throw new Exception("Suscripcion rechazada por el servidor: " + respuesta.serializar());
        }

        this.socketEventos = s;
        Thread t = new Thread(() -> bucleEventos(in, listener), "puesto-eventos-servidor");
        t.setDaemon(true);
        this.hiloEventos = t;
        t.start();
    }

    @Override
    public synchronized void cerrarSuscripcion() {
        if (socketEventos != null) {
            cerrarSilencioso(socketEventos);
            socketEventos = null;
        }
        hiloEventos = null;
    }

    private void bucleEventos(BufferedReader in, IListenerEventosServidor listener) {
        try {
            String linea;
            while ((linea = in.readLine()) != null) {
                Mensaje respuestaServidor;
                try {
                    respuestaServidor = Mensaje.parse(linea);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                if (respuestaServidor.getTipo() == TipoMensaje.EVENTO_FILA_ACTUALIZADA
                        && !respuestaServidor.getArgs().isEmpty()) {
                    try {
                        int n = Integer.parseInt(respuestaServidor.getArg(0));
                        listener.onFilaActualizada(n);
                    } catch (NumberFormatException ex) {
                        // ignorar mensaje mal formado
                    }
                }
            }
        } catch (IOException e) {
            // socket cerrado o error de red: termina silenciosamente.
        }
    }

    private String parsearTurnoOFilaVacia(Mensaje respuestaServidor) throws Exception {
        validarOk(respuestaServidor);
        if (respuestaServidor.getArgs().isEmpty()) {
            throw new Exception("Respuesta inesperada del servidor: " + respuestaServidor.serializar());
        }
        String sub = respuestaServidor.getArg(0);
        if ("FILA_VACIA".equals(sub)) {
            return null;
        }
        if ("TURNO".equals(sub) && respuestaServidor.getArgs().size() >= 2) {
            return respuestaServidor.getArg(1);
        }
        throw new Exception("Respuesta inesperada del servidor: " + respuestaServidor.serializar());
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

    private Mensaje enviar(Mensaje mensaje) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ipServidor, puertoServidor),
                    ConfigHA.SOCKET_CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(ConfigHA.SOCKET_CONNECT_TIMEOUT_MS);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(mensaje.serializar());
            String textoRecibido = in.readLine();
            if (textoRecibido == null) {
                throw new Exception("El servidor cerro la conexion sin responder");
            }
            return Mensaje.parse(textoRecibido);
        }
    }

    private void cerrarSilencioso(Socket s) {
        try {
            s.close();
        } catch (IOException e) {
            // ignorar
        }
    }
}
