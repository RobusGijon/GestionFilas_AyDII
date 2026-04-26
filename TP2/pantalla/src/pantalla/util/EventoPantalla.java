package pantalla.util;

public class EventoPantalla {

    public enum Tipo { LLAMADO, RENOTIFICACION }

    private final Tipo tipo;
    private final String dni;
    private final int idPuesto;
    private final int intentos;

    public EventoPantalla(Tipo tipo, String dni, int idPuesto, int intentos) {
        this.tipo = tipo;
        this.dni = dni;
        this.idPuesto = idPuesto;
        this.intentos = intentos;
    }

    public Tipo getTipo() { return tipo; }
    public String getDni() { return dni; }
    public int getIdPuesto() { return idPuesto; }
    public int getIntentos() { return intentos; }
}
