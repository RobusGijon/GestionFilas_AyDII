package servidor_central.interfaces;

import java.util.List;

import servidor_central.datos.Turno;

public interface IGestorFilaEspera {

    void agregarTurno(Turno t);

    Turno extraerPrimero();

    boolean contieneDni(String dni);

    boolean estaVacia();

    int tamano();

    List<Turno> snapshot();
}
