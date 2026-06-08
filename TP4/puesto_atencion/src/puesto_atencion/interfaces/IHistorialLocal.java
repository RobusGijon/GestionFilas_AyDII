package puesto_atencion.interfaces;

import java.util.List;

import puesto_atencion.dominio.ClienteAtendido;

public interface IHistorialLocal {
    void agregar(ClienteAtendido c);

    /**
     * Reemplaza el historial con el recuperado del servidor al reconectar. La
     * lista llega en orden cronologico (mas antiguo primero); queda almacenada en
     * el orden de visualizacion del puesto (mas reciente primero).
     */
    void inicializar(List<ClienteAtendido> historial);

    List<ClienteAtendido> snapshot();
}
