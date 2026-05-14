package puesto_atencion.dominio;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import puesto_atencion.datos.ClienteAtendido;
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
    public synchronized List<ClienteAtendido> snapshot() {
        return new ArrayList<>(atendidos);
    }
}
