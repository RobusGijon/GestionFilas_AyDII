package servidor_central.gestores;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import servidor_central.datos.Turno;
import servidor_central.interfaces.IGestorClientesLlamados;

public class GestorClientesLlamados implements IGestorClientesLlamados {

    /** Cantidad de llamados recientes que se conservan para el Monitor de Sala. */
    private static final int MAX_HISTORIAL = 5;

    private final Map<Integer, Turno> turnosEnPuesto = new HashMap<>();

    /** Historial cronologico (mas antiguo primero) de los ultimos llamados. */
    private final LinkedList<Turno> historial = new LinkedList<>();

    @Override
    public void asignarTurnoAPuesto(int idPuesto, Turno t) {
        turnosEnPuesto.put(idPuesto, t);
    }

    @Override
    public Turno obtenerTurno(int idPuesto) {
        return turnosEnPuesto.get(idPuesto);
    }

    @Override
    public Turno eliminarTurno(int idPuesto) {
        return turnosEnPuesto.remove(idPuesto);
    }

    @Override
    public boolean contieneDni(String dni) {
        for (Turno t : turnosEnPuesto.values()) {
            if (t.getDni().equals(dni)) return true;
        }
        return false;
    }

    @Override
    public int incrementarIntentos(int idPuesto) {
        Turno t = turnosEnPuesto.get(idPuesto);
        if (t == null) {
            throw new IllegalStateException("No hay turno asignado al puesto " + idPuesto);
        }
        if (t.getIntentosLlamados() >= 3) {
            throw new IllegalStateException("El turno del puesto " + idPuesto + " ya alcanzo el maximo de intentos (3)");
        }
        t.setIntentosLlamados(t.getIntentosLlamados() + 1);
        return t.getIntentosLlamados();
    }

    @Override
    public Collection<Turno> snapshot() {
        return new ArrayList<>(turnosEnPuesto.values());
    }

    @Override
    public void agregarAHistorial(Turno t) {
        // Se guarda una copia (dni + puesto) para que el historial no se vea
        // afectado si el turno sigue mutando mientras esta en atencion.
        historial.addLast(new Turno(t.getDni(), t.getIntentosLlamados(), t.getIdPuestoAsignado()));
        while (historial.size() > MAX_HISTORIAL) {
            historial.removeFirst();
        }
    }

    @Override
    public List<Turno> historial() {
        return new ArrayList<>(historial);
    }

    @Override
    public void limpiar() {
        turnosEnPuesto.clear();
        historial.clear();
    }
}
