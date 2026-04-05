package puesto_atencion.logica;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import puesto_atencion.interfaces.IManejoFila;

public class ManejadorDeFila implements IManejoFila {

    private Queue<String> fila;

    public ManejadorDeFila() {
        this.fila = new LinkedList<>();
    }

    @Override
    public synchronized void agregarCliente(String dni) {
        fila.add(dni);
    }

    @Override
    public synchronized String retiraPrimerDNI() {
        return fila.poll();
    }

    public synchronized String retirarYObtenerDNI(List<String> filaRestante) {
        String dni = fila.poll();
        filaRestante.addAll(fila);
        return dni;
    }

    @Override
    public synchronized List<String> obtenerFila() {
        return new ArrayList<>(fila);
    }

    @Override
    public synchronized boolean filaVacia() {
        return fila.isEmpty();
    }
}
