package pantalla.interfaces;

import java.io.IOException;
import java.net.Socket;

import pantalla.protocolo.mensajes.Mensaje;

public interface ICanalMensaje {

    public boolean enviar(Mensaje mensaje);

    public Mensaje recibir() throws IOException;

    public boolean estaRespondiendo();

    public Socket getSocket();

    public boolean estaVivo();

    public void cerrar();

}
