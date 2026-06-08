package puesto_atencion.interfaces;

import java.io.IOException;
import java.net.Socket;

import puesto_atencion.protocolo.mensajes.Mensaje;

public interface ICanalMensaje {
    
    public boolean enviar(Mensaje mensaje);

    public Mensaje recibir() throws IOException;

    public boolean estaRespondiendo();

    public void cerrar();

}
