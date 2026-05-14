package puesto_atencion.interfaces;

public interface IGestorConexionServidor {
    int    conectarPuesto()              throws Exception;
    String llamarSiguiente(int idPuesto) throws Exception;
    int    reNotificar(int idPuesto)     throws Exception;
    String eliminarCliente(int idPuesto) throws Exception;
    void   suscribirEventos(IListenerEventosServidor listener) throws Exception;
    void   cerrarSuscripcion();
}
