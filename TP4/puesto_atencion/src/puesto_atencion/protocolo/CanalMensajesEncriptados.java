package puesto_atencion.protocolo;

import java.io.IOException;

import puesto_atencion.disponibilidad.Direccion;
import puesto_atencion.interfaces.IEstrategiaEncriptacion;

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
