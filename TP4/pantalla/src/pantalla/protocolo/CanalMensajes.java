package pantalla.protocolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import pantalla.disponibilidad.ConfigHA;
import pantalla.interfaces.ICanalMensaje;
import pantalla.protocolo.mensajes.Direccion;
import pantalla.protocolo.mensajes.Mensaje;


/**
 * Canal de mensajes sobre un socket TCP. Abstrae el framing por lineas, el
 * parseo a {@link Mensaje} y (en la subclase) el cifrado.
 *
 * Tiene dos modos segun como se construya:
 *
 *  - Connect-out ({@link #CanalMensajes(Direccion)}): el canal recuerda la
 *    Direccion y se conecta hacia afuera. Como un Socket TCP queda muerto si el
 *    peer se cae, {@link #asegurarConexion()} abre un socket NUEVO de forma
 *    transparente antes de enviar/recibir. Lo usan los clientes y el link HA
 *    par-a-par del primario.
 *
 *  - Socket aceptado ({@link #CanalMensajes(Socket)}): el canal envuelve un
 *    socket que ya llego por {@code ServerSocket.accept()}. Un servidor no puede
 *    "reconectarse" a su cliente, asi que en este modo no hay reconexion: si el
 *    peer cierra, {@link #recibir()} devuelve null. Lo usan ManejadorConexion y
 *    el ListenerPar del secundario.
 */
public class CanalMensajes implements ICanalMensaje {

    protected final Direccion direccion;
    protected Socket socket;
    protected PrintWriter out;
    protected BufferedReader in;
    private boolean roto;

    /** Modo connect-out: se conecta a la direccion y reconecta si hace falta. */
    public CanalMensajes(Direccion direccion) throws IOException {
        this.direccion = direccion;
        abrirConexion();
    }

    /** Modo socket aceptado: envuelve un socket ya conectado, sin reconexion. */
    public CanalMensajes(Socket socketAceptado) throws IOException {
        this.direccion = null;
        this.socket = socketAceptado;
        this.out = new PrintWriter(socketAceptado.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socketAceptado.getInputStream()));
        this.roto = false;
    }

    private void abrirConexion() throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(direccion.getIp(), direccion.getPuerto()), ConfigHA.SOCKET_CONNECT_TIMEOUT_MS);
        this.socket = s;
        this.out = new PrintWriter(s.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        this.roto = false;
    }

    /**
     * Reabre la conexion (new Socket) si el canal esta muerto o roto. En modo
     * socket aceptado no hay a donde reconectar: lanza IOException.
     */
    protected synchronized void asegurarConexion() throws IOException {
        if (!roto && estaVivo()) {
            return;
        }
        if (direccion == null) {
            throw new IOException("canal sobre socket aceptado: el peer cerro la conexion");
        }
        cerrarRecursos();
        abrirConexion();
    }

    @Override
    public synchronized boolean enviar(Mensaje mensaje) {
        try {
            asegurarConexion();
        } catch (IOException e) {
            return false; // el peer sigue caido: no se pudo revivir
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

    /**
     * Lee la proxima linea y la parsea. Devuelve null si el peer cerro.
     * Propaga IllegalArgumentException si la linea es invalida.
     */
    @Override
    public synchronized Mensaje recibir() throws IOException {
        asegurarConexion();
        String linea;
        try {
            linea = in.readLine();
        } catch (SocketTimeoutException e) {
            throw e; // timeout de lectura: la conexion sigue usable, no la marcamos rota
        } catch (IOException e) {
            roto = true;
            throw e;
        }
        if (linea == null) {
            roto = true; // EOF: el peer cerro; el proximo enviar reconecta (si hay direccion)
            return null;
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

    @Override
    public synchronized boolean estaVivo() {
        return socket != null && !socket.isClosed() && !out.checkError();
    }

    @Override
    public boolean estaRespondiendo() {
        if (direccion == null) {
            return estaVivo();
        }
        try (Socket sonda = new Socket()) {
            sonda.connect(new InetSocketAddress(direccion.getIp(), direccion.getPuerto()),
                    ConfigHA.SOCKET_CONNECT_TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public synchronized Socket getSocket() {
        return socket;
    }

    @Override
    public void cerrar() {
        // NO sincronizar: si otro hilo esta bloqueado en recibir() (readLine) con
        // el monitor tomado, sincronizar aca colgaria para siempre. Cerrar el
        // socket directamente desbloquea ese readLine (lanza IOException), que
        // libera el monitor; despues se liberan los streams.
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
