package servidor_central.conexion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import servidor_central.datos.Turno;
import servidor_central.interfaces.ICoordinadorServidor;
import servidor_central.interfaces.IPantalla;
import servidor_central.logging.ILogger;
import servidor_central.protocolo.Mensaje;
import servidor_central.protocolo.TipoMensaje;

public class ManejadorConexion implements Runnable {

    private final Socket socket;
    private final ICoordinadorServidor coordinador;
    private final IPantalla pub;
    private final ILogger log;

    public ManejadorConexion(Socket socket, ICoordinadorServidor coordinador, IPantalla pub, ILogger log) {
        this.socket = socket;
        this.coordinador = coordinador;
        this.pub = pub;
        this.log = log;
    }

    @Override
    public void run() {
        boolean retener = false;
        PrintWriter out = null;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String linea = in.readLine();
            if (linea == null) {
                return;
            }

            Mensaje m;
            try {
                m = Mensaje.parse(linea);
            } catch (IllegalArgumentException e) {
                log.logError("PARSE", e);
                out.println(new Mensaje(TipoMensaje.ERROR, "MENSAJE_INVALIDO").serializar());
                return;
            }

            retener = despachar(m, out);

        } catch (IOException e) {
            log.logError("MANEJADOR_CONEXION", e);
        } catch (RuntimeException e) {
            if (out != null) {
                String msg = (e.getMessage() != null) ? e.getMessage() : e.getClass().getSimpleName();
                out.println(new Mensaje(TipoMensaje.ERROR, sanitizar(msg)).serializar());
            }
        } finally {
            if (!retener) {
                cerrarSocket();
            }
        }
    }

    private boolean despachar(Mensaje m, PrintWriter out) {
        switch (m.getTipo()) {
            case REGISTRAR_CLIENTE: {
                String dni = m.getArg(0);
                boolean ok = coordinador.registrarCliente(dni);
                out.println(ok
                        ? new Mensaje(TipoMensaje.OK, "REGISTRADO").serializar()
                        : new Mensaje(TipoMensaje.ERROR, "DUPLICADO").serializar());
                return false;
            }
            case CONECTAR_PUESTO: {
                String host = socket.getInetAddress().getHostAddress();
                int id = coordinador.conectarPuesto(host);
                out.println(new Mensaje(TipoMensaje.OK, "ID_PUESTO", String.valueOf(id)).serializar());
                return false;
            }
            case LLAMAR_SIGUIENTE: {
                int id = Integer.parseInt(m.getArg(0));
                Turno t = coordinador.llamarSiguiente(id);
                out.println(t == null
                        ? new Mensaje(TipoMensaje.OK, "FILA_VACIA").serializar()
                        : new Mensaje(TipoMensaje.OK, "TURNO", t.getDni()).serializar());
                return false;
            }
            case RENOTIFICAR: {
                int id = Integer.parseInt(m.getArg(0));
                int n = coordinador.reNotificar(id);
                out.println(new Mensaje(TipoMensaje.OK, "INTENTOS", String.valueOf(n)).serializar());
                return false;
            }
            case ELIMINAR_CLIENTE: {
                int id = Integer.parseInt(m.getArg(0));
                Turno t = coordinador.eliminarCliente(id);
                out.println(t == null
                        ? new Mensaje(TipoMensaje.OK, "FILA_VACIA").serializar()
                        : new Mensaje(TipoMensaje.OK, "TURNO", t.getDni()).serializar());
                return false;
            }
            case SUSCRIBIR_PANTALLA: {
                boolean ok = pub.suscribir(socket);
                if (ok) {
                    out.println(new Mensaje(TipoMensaje.OK, "SUSCRITO").serializar());
                    log.logInfo("Pantalla suscripta desde " + socket.getInetAddress().getHostAddress());
                    return true;
                } else {
                    out.println(new Mensaje(TipoMensaje.ERROR, "SUSCRIPCION_FALLIDA").serializar());
                    return false;
                }
            }
            default:
                out.println(new Mensaje(TipoMensaje.ERROR, "TIPO_NO_SOPORTADO").serializar());
                return false;
        }
    }

    private void cerrarSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            // ignorar
        }
    }

    private String sanitizar(String s) {
        return s.replace('|', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
