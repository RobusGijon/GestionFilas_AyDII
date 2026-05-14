package servidor_central.datos;

public class Turno {

    private String dni;
    private int intentosLlamados;
    private Integer idPuestoAsignado;

    public Turno(String dni, int intentosLlamados, Integer idPuestoAsignado) {
        this.dni = dni;
        this.intentosLlamados = intentosLlamados;
        this.idPuestoAsignado = idPuestoAsignado;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public int getIntentosLlamados() {
        return intentosLlamados;
    }

    public void setIntentosLlamados(int intentosLlamados) {
        this.intentosLlamados = intentosLlamados;
    }

    public Integer getIdPuestoAsignado() {
        return idPuestoAsignado;
    }

    public void setIdPuestoAsignado(Integer idPuestoAsignado) {
        this.idPuestoAsignado = idPuestoAsignado;
    }

    @Override
    public String toString() {
        return "Turno{dni=" + dni + ", intentos=" + intentosLlamados + ", puesto=" + idPuestoAsignado + "}";
    }
}
