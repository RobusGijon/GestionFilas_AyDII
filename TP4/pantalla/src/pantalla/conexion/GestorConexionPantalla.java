package pantalla.conexion;

import java.io.IOException;

import pantalla.interfaces.ICanalMensaje;
import pantalla.interfaces.IConexionPantalla;
import pantalla.interfaces.IEstrategiaEncriptacion;
import pantalla.protocolo.CanalMensajes;
import pantalla.protocolo.CanalMensajesEncriptados;
import pantalla.protocolo.mensajes.Mensaje;
import pantalla.protocolo.mensajes.TipoMensaje;
import pantalla.protocolo.mensajes.Direccion;

public class GestorConexionPantalla extends Thread {

    private final IConexionPantalla listener;
    private final Direccion direccion;
    private final IEstrategiaEncriptacion encriptacion;
    private ICanalMensaje canalMensaje;
    private volatile boolean hiloSuscriptoCorriendo = false;

    public GestorConexionPantalla(Direccion direccion, IConexionPantalla listener, IEstrategiaEncriptacion encriptacion) {
        this.direccion = direccion;
        this.listener = listener;
        this.encriptacion = encriptacion;
    }

    public void conectar() throws IOException {
        this.canalMensaje = encriptacion == null
                ? new CanalMensajes(direccion)
                : new CanalMensajesEncriptados(direccion, encriptacion);

        canalMensaje.enviar(new Mensaje(TipoMensaje.SUSCRIBIR_PANTALLA, ""));

        Mensaje respuesta = canalMensaje.recibir();
        Mensaje suscripto = new Mensaje(TipoMensaje.OK, TipoMensaje.SUSCRITO.name());

        if (respuesta != null && respuesta.serializar().equals(suscripto.serializar())) {
            log("Pantalla suscripta al servidor");
        } else {
            canalMensaje.cerrar();
            log("No se entablo la conexion; El servidor respondio con error");
            throw new IOException("[Gestor conexion] El servidor respondio con error");
        }
    }

    @Override
    public void run() {
        hiloSuscriptoCorriendo = true;
        try {
            while (hiloSuscriptoCorriendo) {
                Mensaje eventoServidor = canalMensaje.recibir();
                if (eventoServidor == null) {
                    break; // el servidor cerro la conexion
                }
                procesarEvento(eventoServidor);
            }
        } catch (IOException e) {
            if (hiloSuscriptoCorriendo) {
                log("Se cerro la conexion con el servidor");
            }
        } finally {
            hiloSuscriptoCorriendo = false;
            if (canalMensaje != null) {
                canalMensaje.cerrar();
            }
        }
    }

    public void detener() {
        hiloSuscriptoCorriendo = false;
        if (canalMensaje != null) {
            canalMensaje.cerrar(); // desbloquea el recibir() que esta esperando
        }
        interrupt();
    }

    private void procesarEvento(Mensaje evento) {
        try {
            switch (evento.getTipo()) {
                case EVENTO_LLAMADO:
                    if (evento.getArgs().size() >= 2)
                        listener.actualizarLlamado(evento);
                    break;
                case EVENTO_RENOTIFICACION:
                    if (evento.getArgs().size() >= 2)
                        listener.actualizarRenotificacion(evento);
                    break;
                case EVENTO_AUSENTE:
                    if (!evento.getArgs().isEmpty())
                        listener.actualizarAusente(evento);
                    break;
                case EVENTO_HISTORIAL:
                    listener.actualizarHistorial(evento);
                    break;
                default:
                    break;
            }
        } catch (NumberFormatException e) {
            log("Evento con formato numerico invalido: " + e.getMessage());
        }
    }

    private void log(String mensaje) {
        System.out.println("[Gestor Conexion]: " + mensaje);
    }

}
