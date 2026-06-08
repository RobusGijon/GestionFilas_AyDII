package terminal_registro.conexion;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import terminal_registro.interfaces.IEstrategiaEncriptacion;
import terminal_registro.protocolo.mensajes.Direccion;
import terminal_registro.disponibilidad.ConfigHA;
import terminal_registro.interfaces.IConexionRegistro;

public class GestorConexionRedundanteRegistro implements IConexionRegistro {

    private final List<Direccion> direcciones;
    private final IEstrategiaEncriptacion encriptacion;

    private final GestorConexionRegistro[] nodosConexion;

    private volatile int indiceConexionPrincipal = 0;

    public GestorConexionRedundanteRegistro(Direccion primaria, Direccion secundaria,
            IEstrategiaEncriptacion encriptacion) {
        if (primaria == null) {
            throw new IllegalArgumentException("direccion primaria requerida");
        }
        this.encriptacion = encriptacion;
        List<Direccion> dirs = new ArrayList<>();
        dirs.add(primaria);
        if (secundaria != null) {
            dirs.add(secundaria);
        }
        this.direcciones = Collections.unmodifiableList(dirs);
        this.nodosConexion = new GestorConexionRegistro[this.direcciones.size()];
    }

    @Override
    public ResultadoRegistro registrar(String dni) {
        int n = direcciones.size();
        for (int ronda = 0; ronda < ConfigHA.RETRY_MAX_INTENTOS; ronda++) {
            for (int desplazamiento = 0; desplazamiento < n; desplazamiento++) {
                int idx = (indiceConexionPrincipal + desplazamiento) % n;
                GestorConexionRegistro nodo = obtenerNodo(idx);
                ResultadoRegistro res = (nodo == null)
                        ? ResultadoRegistro.ERROR_CONEXION
                        : nodo.registrar(dni);

               
                if (res != ResultadoRegistro.ERROR_CONEXION) {
                    if (idx != indiceConexionPrincipal) {
                        log("registrar OK contra " + direcciones.get(idx)
                                + " - failover desde " + direcciones.get(indiceConexionPrincipal));
                        indiceConexionPrincipal = idx;
                    }
                    return res;
                }

                descartarNodo(idx);
            }
            if (ronda < ConfigHA.RETRY_MAX_INTENTOS - 1) {
                if (!dormir(ConfigHA.RETRY_BACKOFF_MS[ronda])) {
                    return ResultadoRegistro.ERROR_CONEXION;
                }
            }
        }
        log("registrar: ningun nodo disponible tras " + ConfigHA.RETRY_MAX_INTENTOS + " rondas");
        return ResultadoRegistro.ERROR_CONEXION;
    }

    private synchronized GestorConexionRegistro obtenerNodo(int idx) {
        if (nodosConexion[idx] == null) {
            try {
                nodosConexion[idx] = new GestorConexionRegistro(direcciones.get(idx), encriptacion);
            } catch (IOException e) {
                log("nodo " + direcciones.get(idx) + " no disponible: " + e.getMessage());
                return null;
            }
        }
        return nodosConexion[idx];
    }

    private synchronized void descartarNodo(int idx) {
        GestorConexionRegistro nodo = nodosConexion[idx];
        nodosConexion[idx] = null;
        if (nodo != null) {
            nodo.cerrar();
        }
    }

    public boolean algunServerRespondiendo() {
        for (Direccion direccion : direcciones) {
            try (Socket sonda = new Socket()) {
                sonda.connect(new InetSocketAddress(direccion.getIp(), direccion.getPuerto()),
                        ConfigHA.SOCKET_CONNECT_TIMEOUT_MS);
                return true;
            } catch (IOException ignore) {
            }
        }
        return false;
    }

    @Override
    public boolean elServerEstaRespondiendo() {
        return algunServerRespondiendo();
    }

    private boolean dormir(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void log(String msg) {
        System.out.println("[REDUNDANTE-TERMINAL] " + msg);
    }
}
