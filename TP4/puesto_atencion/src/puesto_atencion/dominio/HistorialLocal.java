package puesto_atencion.dominio;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import puesto_atencion.interfaces.IHistorialLocal;

public class HistorialLocal implements IHistorialLocal {

    private final LinkedList<ClienteAtendido> atendidos;

    public HistorialLocal() {
        this.atendidos = new LinkedList<>();
    }

    @Override
    public synchronized void agregar(ClienteAtendido c) {
        atendidos.addFirst(c);
    }

    @Override
    public synchronized void inicializar(List<ClienteAtendido> historial) {
        atendidos.clear();
        // La lista llega cronologica (mas antiguo primero); addFirst de cada uno
        // la deja con el mas reciente al frente, igual que agregar().
        for (ClienteAtendido c : historial) {
            atendidos.addFirst(c);
        }
    }

    @Override
    public synchronized List<ClienteAtendido> snapshot() {
        return new ArrayList<>(atendidos);
    }
}
