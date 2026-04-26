package servidor_central.interfaces;

import java.net.Socket;

import servidor_central.datos.Turno;

public interface IPantalla {

    boolean suscribir(Socket socket);

    void emitirLlamado(Turno t);

    void emitirRenotificacion(Turno t);

    void emitirAusente(int idPuesto);

    boolean estaConectada();
}
