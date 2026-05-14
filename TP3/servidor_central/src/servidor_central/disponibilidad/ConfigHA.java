package servidor_central.disponibilidad;

public final class ConfigHA {

    public static final int HEARTBEAT_INTERVALO_MS = 1000;
    public static final int HEARTBEAT_TIMEOUT_MS   = 3000;
    public static final int HEARTBEAT_RECONEXION_PAUSA_MS = 2000;
    public static final int MONITOR_TICK_MS = 1000;

    public static final int SOCKET_TIMEOUT_MS = 1500;

    private ConfigHA() {}
}
