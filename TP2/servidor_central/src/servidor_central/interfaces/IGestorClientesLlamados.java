package servidor_central.interfaces;

import java.util.Collection;

import servidor_central.datos.Turno;

public interface IGestorClientesLlamados {

    void asignarTurnoAPuesto(int idPuesto, Turno t);

    Turno obtenerTurno(int idPuesto);

    Turno eliminarTurno(int idPuesto);

    boolean contieneDni(String dni);

    int incrementarIntentos(int idPuesto);

    Collection<Turno> snapshot();
}
