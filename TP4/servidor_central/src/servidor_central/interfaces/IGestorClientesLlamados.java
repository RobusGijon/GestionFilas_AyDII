package servidor_central.interfaces;

import java.util.Collection;
import java.util.List;

import servidor_central.datos.Turno;

public interface IGestorClientesLlamados {

    void asignarTurnoAPuesto(int idPuesto, Turno t);

    Turno obtenerTurno(int idPuesto);

    Turno eliminarTurno(int idPuesto);

    boolean contieneDni(String dni);

    int incrementarIntentos(int idPuesto);

    Collection<Turno> snapshot();

    /** Registra un llamado en el historial acotado (mas reciente al final). */
    void agregarAHistorial(Turno t);

    /** Historial cronologico de los ultimos llamados (mas antiguo primero). */
    List<Turno> historial();

    void limpiar();
}
