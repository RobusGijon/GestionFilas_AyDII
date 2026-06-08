package servidor_central.interfaces;

import java.util.List;

import servidor_central.datos.Turno;

public interface IPantalla {

    boolean agregarObservador(ICanalMensaje canal);

    void notificarLlamado(Turno t);

    void notificarRenotificacion(Turno t);

    void notificarAusente(int idPuesto);

    /**
     * Reenvia el historial de ultimos llamados (orden cronologico, mas antiguo
     * primero) para que el Monitor lo reconstruya tras un reinicio o reconexion.
     */
    void notificarHistorial(List<Turno> historial);

    boolean estaConectada();
}
