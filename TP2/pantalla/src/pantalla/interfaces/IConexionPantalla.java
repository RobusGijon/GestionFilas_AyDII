package pantalla.interfaces;

public interface IConexionPantalla {
    void recibirLlamado(String dni, int idPuesto);
    void recibirRenotificacion(String dni, int idPuesto);
    void recibirAusente(int idPuesto);
}
