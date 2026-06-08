package servidor_central.disponibilidad;

import servidor_central.conexion.GestorConexion;
import servidor_central.interfaces.ICoordinadorServidor;
import servidor_central.interfaces.IDisponibilidad;
import servidor_central.interfaces.IGestorHeartbeat;
import servidor_central.interfaces.IGestorReplicacion;
import servidor_central.interfaces.IObservadorReplicacion;
import servidor_central.logging.ILogger;
import servidor_central.protocolo.mensajes.Direccion;
import servidor_central.protocolo.mensajes.Mensaje;


public class SupervisorRol implements IDisponibilidad, IObservadorReplicacion {

    private final ILogger log;
    private final Direccion par;
    private volatile RolNodo rolActual;

    private ICoordinadorServidor coord;
    private IGestorHeartbeat heartbeat;
    private IGestorReplicacion replicacion;
    private ListenerPar listenerPar;
    private GestorConexion srvConexion;
    private Thread hiloServidor;

    public SupervisorRol(RolNodo rolInicial, Direccion par, ILogger log) {
        this.rolActual = rolInicial;
        this.par = par;
        this.log = log;
    }

    public void setCoord(ICoordinadorServidor c)       { this.coord = c; }
    public void setHeartbeat(IGestorHeartbeat h)       { this.heartbeat = h; }
    public void setReplicacion(IGestorReplicacion r)   { this.replicacion = r; }
    public void setListenerPar(ListenerPar l)          { this.listenerPar = l; }
    public void setGestorConexion(GestorConexion s)    { this.srvConexion = s; }

    public void iniciar() {
        log.logInfo("[HA] Soy " + rolActual + ". Par configurado: " + par);
        if (rolActual == RolNodo.SECUNDARIO) {
            log.logInfo("[HA] Modo secundario: NO se aceptan conexiones de clientes");
        }
    }

    public void detener() {
        log.logInfo("[HA] Detener supervisor (rol actual: " + rolActual + ")");
    }

    public RolNodo getRol() {
        return rolActual;
    }

    public Direccion getPar() {
        return par;
    }


    @Override
    public void publicarMutacion(Mensaje m) {
        IGestorReplicacion r = this.replicacion;
        if (r == null) return;
        r.replicar(m);
    }

    @Override
    public void onParConectado() {
        ICoordinadorServidor c = this.coord;
        if (c == null) return;
        c.enlazarDisponibilidad(this);
    }

    @Override
    public void onParDesconectado() {
        ICoordinadorServidor c = this.coord;
        if (c == null) return;
        c.desenlazarDisponibilidad();
    }

    @Override
    public void onSnapshotRecibido(SnapshotEstado snapshot) {
        ICoordinadorServidor c = this.coord;
        if (c == null) return;
        c.aplicarSnapshot(snapshot);
    }

    @Override
    public void onReplicarRecibido(Mensaje replicar) {
        ICoordinadorServidor c = this.coord;
        if (c == null) return;
        c.aplicarReplicacion(replicar);
    }

    // ===== Cambios de rol =====

    public synchronized void promoverAPrimario() {
        if (rolActual == RolNodo.PRIMARIO) return;
        log.logInfo("[HA] PROMOCION: SECUNDARIO -> PRIMARIO");
        rolActual = RolNodo.PRIMARIO;

        if (listenerPar != null) {
            listenerPar.detener();
        }

        if (heartbeat != null)   heartbeat.cambiarRolAPrimario();
        if (replicacion != null) replicacion.cambiarRolAPrimario();

        // Ahora este nodo es el primario: pasa a ser responsable de la persistencia.
        // Activamos y volcamos el estado actual (que tiene en RAM por replicacion).
        if (coord != null) {
            coord.setPersistenciaActiva(true);
            coord.persistirAhora();
        }

        if (srvConexion != null) {
            hiloServidor = new Thread(srvConexion::iniciar, "gestor-conexion");
            hiloServidor.start();
        }

        log.logInfo("[HA] PROMOCION completa: ahora se aceptan conexiones de clientes");
    }

    public synchronized void demoverASecundario() {
        if (rolActual == RolNodo.SECUNDARIO) return;
        log.logInfo("[HA] DEMOCION: PRIMARIO -> SECUNDARIO (split-brain detectado)");
        rolActual = RolNodo.SECUNDARIO;

        // Vuelve a ser secundario: deja de persistir (la persistencia la maneja
        // el primario que provoco el split-brain).
        if (coord != null) {
            coord.setPersistenciaActiva(false);
        }

        if (srvConexion != null) {
            srvConexion.detener();
        }

        if (heartbeat != null)   heartbeat.cambiarRolASecundario();
        if (replicacion != null) replicacion.cambiarRolASecundario();

        if (listenerPar != null) {
            listenerPar.iniciar();
        }

        log.logInfo("[HA] DEMOCION completa: nodo en modo secundario, esperando snapshot del primario");
    }
}
