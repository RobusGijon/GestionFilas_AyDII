package puesto_atencion.interfaces;

import puesto_atencion.dominio.EstadoReconexion;

public interface IGestorConexionServidor {
    /**
     * Conecta el puesto. {@code idDeseado > 0} pide reclamar ese slot exacto
     * (reconexion de un puesto vivo); &lt;= 0 deja que el servidor reparta o cree
     * uno. Devuelve el id asignado + el estado a recuperar (turno + historial).
     */
    EstadoReconexion conectarPuesto(int idDeseado, IListenerEventosServidor listener) throws Exception;
    String llamarSiguiente(int idPuesto) throws Exception;
    int    reNotificar(int idPuesto)     throws Exception;
    String eliminarCliente(int idPuesto) throws Exception;
    void   suscribirEventos(int idPuesto, IListenerEventosServidor listener) throws Exception;
    void   cerrarSuscripcion();
}
