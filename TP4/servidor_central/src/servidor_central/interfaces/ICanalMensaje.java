package servidor_central.interfaces;

import java.io.IOException;
import java.net.Socket;

import servidor_central.protocolo.mensajes.Mensaje;

public interface ICanalMensaje {

    public boolean enviar(Mensaje mensaje);

    public Mensaje recibir() throws IOException;

    public boolean estaRespondiendo();

    public boolean estaVivo();

    public Socket getSocket();

    public void cerrar();

}
