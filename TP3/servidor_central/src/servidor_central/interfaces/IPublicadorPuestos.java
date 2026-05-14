package servidor_central.interfaces;

import java.net.Socket;

public interface IPublicadorPuestos {

    boolean suscribir(Socket socket);

    void emitirActualizacionFila(int tamanio);

    int cantidadSuscriptos();
}
