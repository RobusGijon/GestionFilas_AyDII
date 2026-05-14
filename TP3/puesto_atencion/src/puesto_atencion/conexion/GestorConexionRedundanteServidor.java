package puesto_atencion.conexion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import puesto_atencion.disponibilidad.ConfigHA;
import puesto_atencion.disponibilidad.Direccion;
import puesto_atencion.interfaces.IGestorConexionServidor;
import puesto_atencion.interfaces.IListenerEventosServidor;
import puesto_atencion.protocolo.Mensaje;
import puesto_atencion.protocolo.TipoMensaje;


public class GestorConexionRedundanteServidor implements IGestorConexionServidor {

    private final List<Direccion> direcciones;
    private final List<IGestorConexionServidor> nodos;
    private volatile int activo = 0;

    private volatile boolean suscripcionCorriendo;
    private volatile IListenerEventosServidor listenerSuscripcion;
    private Thread hiloSuscripcion;

    public GestorConexionRedundanteServidor(Direccion primaria, Direccion secundaria) {
        this.direcciones = new ArrayList<>();
        this.nodos = new ArrayList<>();
        if (primaria == null) {
            throw new IllegalArgumentException("direccion primaria requerida");
        }
        this.direcciones.add(primaria);
        this.nodos.add(new GestorConexionServidor(primaria.getIp(), primaria.getPuerto()));
        if (secundaria != null) {
            this.direcciones.add(secundaria);
            this.nodos.add(new GestorConexionServidor(secundaria.getIp(), secundaria.getPuerto()));
        }
    }

    @Override
    public int conectarPuesto() throws Exception {
        return ejecutarConFailover("CONECTAR_PUESTO", IGestorConexionServidor::conectarPuesto);
    }

    @Override
    public String llamarSiguiente(int idPuesto) throws Exception {
        return ejecutarConFailover("LLAMAR_SIGUIENTE", g -> g.llamarSiguiente(idPuesto));
    }

    @Override
    public int reNotificar(int idPuesto) throws Exception {
        return ejecutarConFailover("RENOTIFICAR", g -> g.reNotificar(idPuesto));
    }

    @Override
    public String eliminarCliente(int idPuesto) throws Exception {
        return ejecutarConFailover("ELIMINAR_CLIENTE", g -> g.eliminarCliente(idPuesto));
    }

    @Override
    public synchronized void suscribirEventos(IListenerEventosServidor listener) {
        if (listener == null)
            throw new IllegalArgumentException("listener nulo");
        if (suscripcionCorriendo)
            throw new IllegalStateException("ya hay suscripcion activa");
        this.listenerSuscripcion = listener;
        this.suscripcionCorriendo = true;
        this.hiloSuscripcion = new Thread(this::bucleSuscripcion, "puesto-suscripcion-redundante");
        this.hiloSuscripcion.setDaemon(true);
        this.hiloSuscripcion.start();
    }

    @Override
    public synchronized void cerrarSuscripcion() {
        suscripcionCorriendo = false;
        if (hiloSuscripcion != null)
            hiloSuscripcion.interrupt();
    }

    @FunctionalInterface
    private interface OpCliente<T> {
        T apply(IGestorConexionServidor g) throws Exception;
    }

    private <T> T ejecutarConFailover(String op, OpCliente<T> fn) throws Exception {
        Exception ultima = null;
        int n = nodos.size();
        int inicio = activo;
        for (int idxIntento = 0; idxIntento < n; idxIntento++) {
            int idx = (inicio + idxIntento) % n;
            for (int intento = 0; intento < ConfigHA.RETRY_MAX_INTENTOS; intento++) {
                try {
                    T resultado = fn.apply(nodos.get(idx));
                    if (idx != activo) {
                        log("op " + op + " OK contra " + direcciones.get(idx)
                                + " - failover desde " + direcciones.get(activo));
                        activo = idx;
                    }
                    return resultado;
                } catch (Exception e) {
                    ultima = e;
                    if (intento < ConfigHA.RETRY_MAX_INTENTOS - 1) {
                        try {
                            Thread.sleep(ConfigHA.RETRY_BACKOFF_MS[intento]);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw e;
                        }
                    }
                }
            }
            log("op " + op + " agotado en " + direcciones.get(idx)
                    + " (3 intentos)" + (idxIntento < n - 1 ? " - probando otro nodo" : " - sin mas nodos"));
        }
        throw ultima;
    }

    private void bucleSuscripcion() {
        log("suscripcion supervisora iniciada, nodos=" + direcciones);
        while (suscripcionCorriendo) {
            int n = direcciones.size();
            boolean alguienConecto = false;
            for (int idxIntento = 0; idxIntento < n && suscripcionCorriendo; idxIntento++) {
                int idx = (activo + idxIntento) % n;
                Socket s = null;
                try {
                    Direccion d = direcciones.get(idx);
                    s = new Socket();
                    s.connect(new InetSocketAddress(d.getIp(), d.getPuerto()),
                            ConfigHA.SOCKET_CONNECT_TIMEOUT_MS);
                    PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

                    out.println(new Mensaje(TipoMensaje.SUSCRIBIR_PUESTO).serializar());
                    String confirm = in.readLine();
                    if (confirm == null)
                        continue;
                    Mensaje resp = Mensaje.parse(confirm);
                    if (resp.getTipo() != TipoMensaje.OK
                            || resp.getArgs().isEmpty()
                            || !"SUSCRITO".equals(resp.getArg(0))) {
                        continue;
                    }
                    if (idx != activo) {
                        log("suscripcion: failover " + direcciones.get(activo) + " -> " + d);
                        activo = idx;
                    } else {
                        log("suscripcion conectada a " + d);
                    }
                    alguienConecto = true;
                    bucleEventos(in);
                    // Si bucleEventos retorna sin excepcion: linea null (servidor cerro).
                } catch (IOException e) {
                    // probar el otro
                } finally {
                    cerrarSilencioso(s);
                }
                if (!suscripcionCorriendo)
                    break;
            }
            if (!alguienConecto && suscripcionCorriendo) {
                try {
                    Thread.sleep(ConfigHA.SUSCRIPCION_PAUSA_REINTENTO_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log("suscripcion supervisora detenida");
    }

    private void bucleEventos(BufferedReader in) throws IOException {
        String linea;
        while (suscripcionCorriendo && (linea = in.readLine()) != null) {
            Mensaje ev;
            try {
                ev = Mensaje.parse(linea);
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (ev.getTipo() == TipoMensaje.EVENTO_FILA_ACTUALIZADA && !ev.getArgs().isEmpty()) {
                try {
                    int n = Integer.parseInt(ev.getArg(0));
                    listenerSuscripcion.onFilaActualizada(n);
                } catch (NumberFormatException ex) {
                    // ignorar payload mal formado
                }
            }
        }
    }

    private void cerrarSilencioso(Socket s) {
        if (s != null) {
            try {
                s.close();
            } catch (IOException e) {
                /* ignore */ }
        }
    }

    private void log(String msg) {
        System.out.println("[PUESTO] " + msg);
    }

    @Override
    public String toString() {
        return "GestorConexionRedundanteServidor[" + Arrays.toString(direcciones.toArray()) + "]";
    }
}
