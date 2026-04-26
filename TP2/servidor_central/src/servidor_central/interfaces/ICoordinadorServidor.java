package servidor_central.interfaces;

import servidor_central.datos.Turno;

public interface ICoordinadorServidor {

    boolean registrarCliente(String dni);

    int conectarPuesto(String host);

    Turno llamarSiguiente(int idPuesto);

    int reNotificar(int idPuesto);

    Turno eliminarCliente(int idPuesto);
}
