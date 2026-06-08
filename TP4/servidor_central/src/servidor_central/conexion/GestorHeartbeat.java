package servidor_central.conexion;

import java.io.IOException;

import servidor_central.disponibilidad.ConfigHA;
import servidor_central.disponibilidad.RolNodo;
import servidor_central.interfaces.ICanalMensaje;
import servidor_central.interfaces.IEstrategiaEncriptacion;
import servidor_central.interfaces.IGestorHeartbeat;
import servidor_central.logging.ILogger;
import servidor_central.protocolo.CanalMensajes;
import servidor_central.protocolo.CanalMensajesEncriptados;
import servidor_central.protocolo.mensajes.Direccion;
import servidor_central.protocolo.mensajes.Mensaje;
import servidor_central.protocolo.mensajes.TipoMensaje;

/**
 * Primario: emisor que envia HEARTBEAT al par cada {@link ConfigHA#HEARTBEAT_INTERVALO_MS}.
 * Secundario: monitor que vigila el ultimo timestamp recibido (alimentado por
 * ListenerPar via {@link #manejarMensajeRecibido}) y al superar
 * {@link ConfigHA#HEARTBEAT_TIMEOUT_MS} loguea "PRIMARIO CAIDO" e invoca el
 * callback de caida (tipicamente {@code SupervisorRol.promoverAPrimario}).
 *
 * Soporta cambio de rol en caliente via {@link #cambiarRolAPrimario}.
 */
public class GestorHeartbeat implements IGestorHeartbeat {

    private final ILogger log;
    private final Direccion par;
    private final IEstrategiaEncriptacion encriptacion;

    private volatile RolNodo rol;
    private volatile Runnable callbackCaida;
    private volatile boolean corriendo;
    private volatile long ultimoLatidoRecibidoMs;
    private volatile boolean caidaYaLogueada;

    private Thread hiloEmisor;
    private Thread hiloMonitor;

    public GestorHeartbeat(RolNodo rol, Direccion par, IEstrategiaEncriptacion encriptacion, ILogger log) {
        this.rol = rol;
        this.par = par;
        this.encriptacion = encriptacion;
        this.log = log;
    }

    public void setCallbackCaida(Runnable r) { this.callbackCaida = r; }

    public void iniciar() {
        corriendo = true;
        if (rol == RolNodo.PRIMARIO) {
            iniciarEmisor();
        } else {
            iniciarMonitor();
        }
    }

    public void detener() {
        corriendo = false;
        if (hiloEmisor != null)  hiloEmisor.interrupt();
        if (hiloMonitor != null) hiloMonitor.interrupt();
        log.logInfo("[HEARTBEAT] detenido");
    }

    /** Lo invoca ListenerPar al recibir un HEARTBEAT del par. */
    public void manejarMensajeRecibido(Mensaje m) {
        if (m.getTipo() != TipoMensaje.HEARTBEAT) return;
        ultimoLatidoRecibidoMs = System.currentTimeMillis();
        caidaYaLogueada = false;
    }

    /**
     * Cambio de rol en caliente: el monitor termina solo (la guarda del bucle
     * ve {@code rol != SECUNDARIO}) y se arranca el emisor.
     */
    @Override
    public synchronized void cambiarRolAPrimario() {
        if (rol == RolNodo.PRIMARIO) return;
        log.logInfo("[HEARTBEAT] cambio de rol SECUNDARIO -> PRIMARIO");
        rol = RolNodo.PRIMARIO;
        if (hiloMonitor != null) hiloMonitor.interrupt();
        iniciarEmisor();
    }

    /** Auto-democión por split-brain: detiene el emisor y vuelve a monitorear. */
    @Override
    public synchronized void cambiarRolASecundario() {
        if (rol == RolNodo.SECUNDARIO) return;
        log.logInfo("[HEARTBEAT] cambio de rol PRIMARIO -> SECUNDARIO");
        rol = RolNodo.SECUNDARIO;
        if (hiloEmisor != null) hiloEmisor.interrupt();
        ultimoLatidoRecibidoMs = 0;
        caidaYaLogueada = false;
        iniciarMonitor();
    }

    private void iniciarEmisor() {
        hiloEmisor = new Thread(this::bucleEmisor, "heartbeat-emisor");
        hiloEmisor.setDaemon(true);
        hiloEmisor.start();
        log.logInfo("[HEARTBEAT] emisor iniciado, par=" + par
            + " intervalo=" + ConfigHA.HEARTBEAT_INTERVALO_MS + "ms");
    }

    private void bucleEmisor() {
        boolean caidaYaLogueada = false;
        ICanalMensaje canal = null;
        while (corriendo && rol == RolNodo.PRIMARIO) {
            try {
                if (canal == null) {
                    canal = crearCanal(); // conecta; lanza IOException si el par no responde
                    if (caidaYaLogueada) {
                        log.logInfo("[HEARTBEAT] reconectado al par " + par);
                        caidaYaLogueada = false;
                    } else {
                        log.logInfo("[HEARTBEAT] conectado al par " + par);
                    }
                }

                Mensaje hb = new Mensaje(TipoMensaje.HEARTBEAT,
                    String.valueOf(System.currentTimeMillis()));
                if (!canal.enviar(hb)) {
                    throw new IOException("fallo al enviar HEARTBEAT");
                }
                Thread.sleep(ConfigHA.HEARTBEAT_INTERVALO_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                if (corriendo && !caidaYaLogueada) {
                    log.logInfo("[HEARTBEAT] sin conexion al par " + par
                        + " (" + e.getMessage() + "), reintentando cada "
                        + ConfigHA.HEARTBEAT_RECONEXION_PAUSA_MS + "ms");
                    caidaYaLogueada = true;
                }
                if (canal != null) { canal.cerrar(); canal = null; }
                try {
                    Thread.sleep(ConfigHA.HEARTBEAT_RECONEXION_PAUSA_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (canal != null) canal.cerrar();
    }

    private ICanalMensaje crearCanal() throws IOException {
        return encriptacion == null
                ? new CanalMensajes(par)
                : new CanalMensajesEncriptados(par, encriptacion);
    }

    private void iniciarMonitor() {
        hiloMonitor = new Thread(this::bucleMonitor, "heartbeat-monitor");
        hiloMonitor.setDaemon(true);
        hiloMonitor.start();
        log.logInfo("[HEARTBEAT] monitor iniciado, timeout="
            + ConfigHA.HEARTBEAT_TIMEOUT_MS + "ms");
    }

    private void bucleMonitor() {
        while (corriendo && rol == RolNodo.SECUNDARIO) {
            try {
                Thread.sleep(ConfigHA.MONITOR_TICK_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            long ultimo = ultimoLatidoRecibidoMs;
            if (ultimo == 0) {
                continue;
            }
            long delta = System.currentTimeMillis() - ultimo;
            if (delta > ConfigHA.HEARTBEAT_TIMEOUT_MS && !caidaYaLogueada) {
                log.logInfo("[HEARTBEAT] PRIMARIO CAIDO (detectado, "
                    + delta + "ms sin latido)");
                caidaYaLogueada = true;
                Runnable cb = callbackCaida;
                if (cb != null) {
                    try { cb.run(); }
                    catch (RuntimeException e) { log.logError("CALLBACK_CAIDA", e); }
                }
            }
        }
    }

}
