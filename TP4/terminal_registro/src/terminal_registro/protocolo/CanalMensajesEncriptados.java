package terminal_registro.protocolo;

import java.io.IOException;

import terminal_registro.interfaces.IEstrategiaEncriptacion;
import terminal_registro.protocolo.mensajes.Direccion;

/**
 * Canal con cifrado simetrico. Solo aporta la transformacion de cada linea;
 * toda la gestion de conexion/reconexion vive en {@link CanalMensajes}.
 */
public class CanalMensajesEncriptados extends CanalMensajes {

    private final IEstrategiaEncriptacion encriptador;

    public CanalMensajesEncriptados(Direccion direccion, IEstrategiaEncriptacion encriptador) throws IOException {
        super(direccion);
        this.encriptador = encriptador;
    }

    @Override
    protected String alCable(String linea) {
        return encriptador != null ? encriptador.encriptar(linea) : linea;
    }

    @Override
    protected String delCable(String linea) {
        return encriptador != null ? encriptador.desencriptar(linea) : linea;
    }
}
