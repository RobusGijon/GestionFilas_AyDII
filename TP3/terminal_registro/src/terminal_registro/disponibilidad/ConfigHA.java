package terminal_registro.disponibilidad;

public final class ConfigHA {

    public static final int RETRY_MAX_INTENTOS = 3;
    public static final int[] RETRY_BACKOFF_MS = { 1000, 2000, 3000 };

    public static final int SOCKET_CONNECT_TIMEOUT_MS = 1500;

    public static final int SUSCRIPCION_PAUSA_REINTENTO_MS = 2000;

    private ConfigHA() {
    }
}
