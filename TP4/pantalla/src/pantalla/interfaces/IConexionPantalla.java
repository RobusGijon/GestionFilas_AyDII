package pantalla.interfaces;

import pantalla.protocolo.mensajes.Mensaje;

public interface IConexionPantalla {
    void actualizarLlamado(Mensaje respuestaServidor);

    void actualizarRenotificacion(Mensaje respuestaServidor);

    void actualizarAusente(Mensaje respuestaServidor);

    void actualizarHistorial(Mensaje respuestaServidor);
}
