package puesto_atencion.dominio;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClienteAtendido {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String dni;
    private final LocalTime horaAtencion;

    public ClienteAtendido(String dni, LocalTime horaAtencion) {
        this.dni = dni;
        this.horaAtencion = horaAtencion;
    }

    /**
     * Reconstruye un atendido a partir de los datos que reenvia el servidor al
     * reconectar: dni + hora en texto {@code HH:mm:ss}.
     */
    public static ClienteAtendido desde(String dni, String horaHHmmss) {
        return new ClienteAtendido(dni, LocalTime.parse(horaHHmmss, FMT));
    }

    public String getDni() {
        return dni;
    }

    public LocalTime getHoraAtencion() {
        return horaAtencion;
    }

    @Override
    public String toString() {
        return dni + "  " + horaAtencion.format(FMT);
    }
}
