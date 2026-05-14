package servidor_central.datos;

public class PuestoInfo {

    private int idPuesto;
    private String hostRemoto;

    public PuestoInfo(int idPuesto, String hostRemoto) {
        this.idPuesto = idPuesto;
        this.hostRemoto = hostRemoto;
    }

    public int getIdPuesto() {
        return idPuesto;
    }

    public void setIdPuesto(int idPuesto) {
        this.idPuesto = idPuesto;
    }

    public String getHostRemoto() {
        return hostRemoto;
    }

    public void setHostRemoto(String hostRemoto) {
        this.hostRemoto = hostRemoto;
    }

    @Override
    public String toString() {
        return "PuestoInfo{id=" + idPuesto + ", host=" + hostRemoto + "}";
    }
}
