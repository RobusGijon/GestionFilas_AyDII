package servidor_central.gestores;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import servidor_central.datos.Turno;
import servidor_central.interfaces.IGestorClientesLlamados;

public class GestorClientesLlamados implements IGestorClientesLlamados {

    private final Map<Integer, Turno> turnosEnPuesto = new HashMap<>();

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
}
