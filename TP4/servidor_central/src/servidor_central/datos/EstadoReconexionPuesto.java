package servidor_central.datos;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de {@code conectarPuesto}: el id que el servidor asigna o reclama
 * para el puesto, mas el estado que hay que reenviarle para que recupere su
 * sesion al (re)conectar: el turno que tenia en atencion (si lo habia) y su
 * historial de atendidos. Lo consume {@code ManejadorConexion} para armar la
 * respuesta enmarcada (snapshot) hacia el puesto.
 */
public class EstadoReconexionPuesto {

    private final int idPuesto;
    private final Turno turnoEnAtencion;          // null si el puesto no atendia a nadie
    private final List<ClienteAtendido> historial;

    public EstadoReconexionPuesto(int idPuesto, Turno turnoEnAtencion, List<ClienteAtendido> historial) {
        this.idPuesto = idPuesto;
        this.turnoEnAtencion = turnoEnAtencion;
        this.historial = historial == null ? new ArrayList<>() : historial;
    }

    public int getIdPuesto() {
        return idPuesto;
    }

    public Turno getTurnoEnAtencion() {
        return turnoEnAtencion;
    }

    public List<ClienteAtendido> getHistorial() {
        return historial;
    }
}
