package servidor_central.persistencia.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import servidor_central.datos.PuestoInfo;
import servidor_central.datos.Turno;

/**
 * DTO que captura el estado persistente del servidor central: los puestos
 * activos, la cola de espera y los turnos en atencion. Es el objeto que la capa
 * de persistencia (JSON / XML / TXT) serializa al guardar y reconstruye al leer,
 * permitiendo restaurar el estado al reiniciar.
 *
 * No incluye el proximoId de puestos porque
 * {@link servidor_central.gestores.GestorPuestos#registrarPuestoConId} lo
 * reconstruye solo (lo deja en max(idPuesto) + 1) al restaurar cada puesto.
 */
public class ServerDTO {

    private final List<PuestoInfo> puestosActivos = new ArrayList<>();
    private final LinkedList<Turno> colaEspera = new LinkedList<>();
    private final Map<Integer, Turno> turnosEnPuesto = new HashMap<>();
    private final List<Turno> historialLlamados = new ArrayList<>();

    public ServerDTO() {}

    /** Construye el DTO copiando las colecciones recibidas */
    public ServerDTO(List<PuestoInfo> puestosActivos,
                     List<Turno> colaEspera,
                     Map<Integer, Turno> turnosEnPuesto) {
        if (puestosActivos != null) this.puestosActivos.addAll(puestosActivos);
        if (colaEspera != null) this.colaEspera.addAll(colaEspera);
        if (turnosEnPuesto != null) this.turnosEnPuesto.putAll(turnosEnPuesto);
    }

    // -------- Puestos activos --------

    public List<PuestoInfo> getPuestosActivos() {
        return puestosActivos;
    }

    public void agregarPuesto(PuestoInfo puesto) {
        puestosActivos.add(puesto);
    }

    /** Devuelve el puesto ya agregado con ese id, o null (lo usan los lectores
     * para enganchar las lineas de historial por puesto a su {@link PuestoInfo}). */
    public PuestoInfo buscarPuesto(int idPuesto) {
        for (PuestoInfo p : puestosActivos) {
            if (p.getIdPuesto() == idPuesto) return p;
        }
        return null;
    }

    // -------- Cola de espera --------

    public LinkedList<Turno> getColaEspera() {
        return colaEspera;
    }

    public void agregarTurnoEnEspera(Turno turno) {
        colaEspera.addLast(turno);
    }

    // -------- Turnos en puesto --------

    public Map<Integer, Turno> getTurnosEnPuesto() {
        return turnosEnPuesto;
    }

    public void asignarTurnoAPuesto(int idPuesto, Turno turno) {
        turnosEnPuesto.put(idPuesto, turno);
    }

    // -------- Historial de ultimos llamados (Monitor de Sala) --------

    public List<Turno> getHistorialLlamados() {
        return historialLlamados;
    }

    public void agregarAlHistorial(Turno turno) {
        historialLlamados.add(turno);
    }

    @Override
    public String toString() {
        return "ServerDTO{"
                + "puestosActivos=" + puestosActivos
                + ", colaEspera=" + colaEspera
                + ", turnosEnPuesto=" + turnosEnPuesto
                + ", historialLlamados=" + historialLlamados
                + '}';
    }
}
