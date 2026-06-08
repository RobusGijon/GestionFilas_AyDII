package servidor_central.datos;

/**
 * Registro server-side de un cliente ya atendido por un puesto: el DNI y la hora
 * de atencion (formato {@code HH:mm:ss}). Es el equivalente en el servidor del
 * {@code ClienteAtendido} del puesto_atencion, pero guarda la hora como texto
 * para serializarla directo en los tres formatos de persistencia.
 */
public class ClienteAtendido {

    private final String dni;
    private final String hora;

    public ClienteAtendido(String dni, String hora) {
        this.dni = dni;
        this.hora = hora;
    }

    public String getDni() {
        return dni;
    }

    public String getHora() {
        return hora;
    }

    @Override
    public String toString() {
        return dni + "  " + hora;
    }
}
