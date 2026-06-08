package terminal_registro.interfaces;

import java.io.IOException;
import java.net.Socket;

import terminal_registro.protocolo.mensajes.Mensaje;

public interface ICanalMensaje {

    public boolean enviar(Mensaje mensaje);

    public Mensaje recibir() throws IOException;

    public boolean estaRespondiendo();

    public Socket getSocket();

    public void cerrar();

}
