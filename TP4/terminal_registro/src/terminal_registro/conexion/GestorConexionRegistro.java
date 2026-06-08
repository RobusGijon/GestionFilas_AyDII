package terminal_registro.conexion;

import java.io.IOException;

import terminal_registro.interfaces.ICanalMensaje;
import terminal_registro.interfaces.IConexionRegistro;
import terminal_registro.interfaces.IEstrategiaEncriptacion;
import terminal_registro.protocolo.CanalMensajes;
import terminal_registro.protocolo.CanalMensajesEncriptados;
import terminal_registro.protocolo.mensajes.Direccion;
import terminal_registro.protocolo.mensajes.Mensaje;
import terminal_registro.protocolo.mensajes.TipoMensaje;

public class GestorConexionRegistro implements IConexionRegistro {

    private final ICanalMensaje canalMensaje;

    public GestorConexionRegistro(Direccion direccion, IEstrategiaEncriptacion tipoEncriptacion) throws IOException {
        if (tipoEncriptacion == null) {
            this.canalMensaje = new CanalMensajes(direccion);
        } else {
            this.canalMensaje = new CanalMensajesEncriptados(direccion, tipoEncriptacion);
        }

    }

    @Override
    public boolean elServerEstaRespondiendo() {
        return canalMensaje.estaRespondiendo();
    }

    @Override
    public ResultadoRegistro registrar(String dni) {
        try {
            if (canalMensaje == null || !canalMensaje.estaRespondiendo()) {
                return ResultadoRegistro.ERROR_CONEXION;
            }

            Mensaje pedido = new Mensaje(TipoMensaje.REGISTRAR_CLIENTE, dni);
            if (!canalMensaje.enviar(pedido)) {
                return ResultadoRegistro.ERROR_CONEXION;
            }

            Mensaje respuesta = canalMensaje.recibir();
            if (respuesta == null) {
                return ResultadoRegistro.ERROR_CONEXION;
            }

            if (respuesta.getTipo() == TipoMensaje.OK) {
                return ResultadoRegistro.REGISTRADO;
            }

            if (respuesta.getTipo() == TipoMensaje.ERROR
                    && !respuesta.getArgs().isEmpty()
                    && "DUPLICADO".equals(respuesta.getArg(0))) {
                return ResultadoRegistro.DUPLICADO;
            }
            return ResultadoRegistro.ERROR_CONEXION;
        } catch (Exception e) {
            return ResultadoRegistro.ERROR_CONEXION;
        }
    }

    public void cerrar() {
        if (canalMensaje != null) {
            canalMensaje.cerrar();
        }
    }

}
