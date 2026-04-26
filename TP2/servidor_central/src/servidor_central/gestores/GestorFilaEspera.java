package servidor_central.gestores;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import servidor_central.datos.Turno;
import servidor_central.interfaces.IGestorFilaEspera;

public class GestorFilaEspera implements IGestorFilaEspera {

    private final LinkedList<Turno> cola = new LinkedList<>();

    @Override
    public void agregarTurno(Turno t) {
        cola.addLast(t);
    }

    @Override
    public Turno extraerPrimero() {
        if (cola.isEmpty()) {
            throw new NoSuchElementException("La fila de espera esta vacia");
        }
        return cola.removeFirst();
    }

    @Override
    public boolean contieneDni(String dni) {
        for (Turno t : cola) {
            if (t.getDni().equals(dni)) return true;
        }
        return false;
    }

    @Override
    public boolean estaVacia() {
        return cola.isEmpty();
    }

    @Override
    public int tamano() {
        return cola.size();
    }

    @Override
    public List<Turno> snapshot() {
        return new ArrayList<>(cola);
    }
}
