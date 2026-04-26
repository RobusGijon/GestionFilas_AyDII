package terminal_registro.interfaces;

public interface IConexionRegistro {
    void enviarMensaje(String ip, int puerto, String dni) throws Exception;
}
