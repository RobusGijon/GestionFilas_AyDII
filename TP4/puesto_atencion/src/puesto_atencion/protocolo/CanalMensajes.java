package puesto_atencion.protocolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import puesto_atencion.disponibilidad.ConfigHA;
import puesto_atencion.disponibilidad.Direccion;
import puesto_atencion.interfaces.ICanalMensaje;
import puesto_atencion.protocolo.mensajes.Mensaje;

/**
 * Canal de mensajes auto-reparable.
 *
 * Un Socket TCP esta atado a UNA conexion: si el server se cae, ese socket queda
 * muerto para siempre (no se "revive"). Como el canal recuerda la Direccion,
 * puede abrir un socket NUEVO de forma transparente: enviar() reconecta si el
 * canal esta muerto o quedo marcado como roto. recibir() detecta la muerte
 * (EOF/IOException) y la propaga (null / excepcion) para que la capa de
 * suscripcion vuelva a hacer el handshake; el proximo enviar() reconectara.
 *
 * Al reconectar se preserva el SO_TIMEOUT de lectura (que el req/resp setea por
 * afuera con getSocket().setSoTimeout()).
 */
public class CanalMensajes implements ICanalMensaje {

    protected final Direccion direccion;
    protected Socket socket;
    protected PrintWriter out;
    protected BufferedReader in;
    private boolean roto;

    public CanalMensajes(Direccion direccion) throws IOException {
        this.direccion = direccion;
        abrirConexion();
    }

    private void abrirConexion() throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(direccion.getIp(), direccion.getPuerto()), ConfigHA.SOCKET_CONNECT_TIMEOUT_MS);
        this.socket = s;
        this.out = new PrintWriter(s.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        this.roto = false;
    }

    protected void asegurarConexion() throws IOException {
        if (!roto && estaVivo()) {
            return;
        }
        
        cerrarRecursos();
        abrirConexion();
    }

    @Override
    public synchronized boolean enviar(Mensaje mensaje) {
        try {
            asegurarConexion();
        } catch (IOException e) {
            return false; // el server sigue caido: no se pudo revivir
        }
        String plano = mensaje.serializar();
        String cable = alCable(plano);
        logEnviar(plano, cable);
        out.println(cable);
        if (out.checkError()) {
            roto = true; // el proximo enviar/recibir reconectara
            return false;
        }
        return true;
    }

    @Override
    public Mensaje recibir() throws IOException {
        asegurarConexion();
        String linea;
        try {
            linea = in.readLine();
        } catch (IOException e) {
            roto = true;
            throw e;
        }
        if (linea == null) {
            roto = true;
            throw new IOException("Conexion rota con el servidor");
        }
        String plano = delCable(linea);
        logRecibir(linea, plano);
        return Mensaje.parse(plano);
    }

    protected String alCable(String linea) {
        return linea;
    }

    protected String delCable(String linea) {
        return linea;
    }

    // ===== Trazas de mensajes (enviados/recibidos y su cifrado) =====
    /** Poner en false para silenciar el log de mensajes del canal. */
    private static final boolean DEBUG_CANAL = true;
    private static final java.time.format.DateTimeFormatter HORA_FMT =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");

    private static String prefijo() {
        return "[" + java.time.LocalTime.now().format(HORA_FMT) + "][CANAL] ";
    }

    private void logEnviar(String plano, String cable) {
        if (!DEBUG_CANAL) return;
        if (plano.startsWith("HEARTBEAT")) return; // no logueamos heartbeats (ruido)
        if (cable.equals(plano)) {
            System.out.println(prefijo() + "Enviando: " + plano);
        } else {
            System.out.println(prefijo() + "Enviando: " + plano + "  [Encriptado: " + cable + "]");
        }
    }

    private void logRecibir(String cable, String plano) {
        if (!DEBUG_CANAL) return;
        if (plano.startsWith("HEARTBEAT")) return; // no logueamos heartbeats (ruido)
        if (cable.equals(plano)) {
            System.out.println(prefijo() + "Recibiendo: " + plano);
        } else {
            System.out.println(prefijo() + "Recibiendo: " + cable + "  [Desencriptado: " + plano + "]");
        }
    }

    public synchronized boolean estaVivo() {
        return socket != null && !socket.isClosed() && !out.checkError();
    }

    @Override
    public boolean estaRespondiendo() {
        try (Socket sonda = new Socket()) {
            sonda.connect(new InetSocketAddress(direccion.getIp(), direccion.getPuerto()),
                    ConfigHA.SOCKET_CONNECT_TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void cerrar() {
        Socket s = this.socket;
        if (s != null && !s.isClosed()) {
            try {
                s.close();
            } catch (IOException e) {
                /* ignore */
            }
        }
        cerrarRecursos();
    }

    private synchronized void cerrarRecursos() {
        if (out != null) {
            out.close();
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                /* ignore */
            }
        }
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                /* ignore */
            }
        }
    }
}
