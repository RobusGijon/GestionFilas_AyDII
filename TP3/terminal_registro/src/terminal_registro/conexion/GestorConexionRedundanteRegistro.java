package terminal_registro.conexion;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import terminal_registro.disponibilidad.ConfigHA;
import terminal_registro.disponibilidad.Direccion;
import terminal_registro.interfaces.IConexionRegistro;

public class GestorConexionRedundanteRegistro implements IConexionRegistro {

    private final List<Direccion> direcciones;
    private final List<IConexionRegistro> conexiones;
    private volatile int conexionPrincipal = 0;

    public GestorConexionRedundanteRegistro(Direccion primaria, Direccion secundaria) {
        if (primaria == null)
            throw new IllegalArgumentException("direccion primaria requerida");

        this.direcciones = new ArrayList<>();
        this.conexiones = new ArrayList<>();

        this.direcciones.add(primaria);
        this.conexiones.add(new GestorConexionRegistro(primaria.getIp(), primaria.getPuerto()));

        if (secundaria != null) {
            this.direcciones.add(secundaria);
            this.conexiones.add(new GestorConexionRegistro(secundaria.getIp(), secundaria.getPuerto()));
        }
    }

    @Override
    public ResultadoRegistro registrar(String dni) {
        int totalConexiones = conexiones.size();

        ResultadoRegistro mensajeServidor = ResultadoRegistro.ERROR_CONEXION;

        for (int numConexion = 0; numConexion < totalConexiones; numConexion++) {

            int tipoConexion = (conexionPrincipal + numConexion) % totalConexiones;

            for (int intento = 0; intento < ConfigHA.RETRY_MAX_INTENTOS; intento++) {
                ResultadoRegistro respuestaServidor = conexiones.get(tipoConexion).registrar(dni);

                if (respuestaServidor != ResultadoRegistro.ERROR_CONEXION) {
                    if (tipoConexion != conexionPrincipal) {
                        log("registrar OK contra " + direcciones.get(tipoConexion) + " - failover desde "
                                + direcciones.get(conexionPrincipal));
                        conexionPrincipal = tipoConexion;
                    }
                    return respuestaServidor;
                }

                mensajeServidor = respuestaServidor;

                if (intento < ConfigHA.RETRY_MAX_INTENTOS - 1) {
                    try {
                        Thread.sleep(ConfigHA.RETRY_BACKOFF_MS[intento]);
                    } catch (InterruptedException ie) {
                        log("internal error: Hilo no pudo hacer sleep");
                        Thread.currentThread().interrupt();
                        return respuestaServidor;
                    }
                }
            }
            log("registrar agotado en " + direcciones.get(tipoConexion)
                    + (numConexion < totalConexiones - 1 ? " - probando otro nodo" : " - sin mas nodos"));
        }
        return mensajeServidor;
    }

    private void log(String msg) {
        System.out.println("[REDUNDANTE-TERMINAL] " + msg);
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
}
