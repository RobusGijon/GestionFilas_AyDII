package terminal_registro.interfaces;

public interface IConexionRegistro {

    enum ResultadoRegistro {
        REGISTRADO,
        DUPLICADO,
        ERROR_CONEXION
    }

    ResultadoRegistro registrar(String dni);
}