package terminal_registro.interfaces;

public interface IEstrategiaEncriptacion {

    public String encriptar(String texto);

    public String desencriptar(String texto);

    public boolean esClaveValida(String clave) throws IllegalArgumentException;
}
