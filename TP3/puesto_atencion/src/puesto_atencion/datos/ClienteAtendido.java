package puesto_atencion.datos;

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
