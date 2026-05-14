package pantalla.interfaces;

import pantalla.protocolo.Mensaje;

public interface IConexionPantalla {
    void recibirLlamado(Mensaje respuestaServidor);

    void recibirRenotificacion(Mensaje respuestaServidor);

    void recibirAusente(Mensaje respuestaServidor);
}
