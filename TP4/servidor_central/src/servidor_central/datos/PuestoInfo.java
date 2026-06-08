package servidor_central.datos;

import java.util.ArrayList;
import java.util.List;

public class PuestoInfo {

    /** Tope de atendidos que se conservan por puesto (los mas recientes). */
    public static final int MAX_HISTORIAL = 5;

    private int idPuesto;
    private String hostRemoto;

    /** Historial de atendidos del puesto, cronologico (mas antiguo primero). */
    private final List<ClienteAtendido> historialAtendidos = new ArrayList<>();

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

    /**
     * Agrega un atendido al historial del puesto y descarta el mas antiguo si se
     * supera {@link #MAX_HISTORIAL}.
     */
    public void agregarAtendido(ClienteAtendido c) {
        historialAtendidos.add(c);
        while (historialAtendidos.size() > MAX_HISTORIAL) {
            historialAtendidos.remove(0);
        }
    }

    /** Historial de atendidos (mas antiguo primero). */
    public List<ClienteAtendido> getHistorialAtendidos() {
        return historialAtendidos;
    }

    @Override
    public String toString() {
        return "PuestoInfo{id=" + idPuesto + ", host=" + hostRemoto
                + ", atendidos=" + historialAtendidos.size() + "}";
    }
}
