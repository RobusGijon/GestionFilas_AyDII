package servidor_central.interfaces;

import java.net.Socket;

import servidor_central.datos.Turno;
import servidor_central.disponibilidad.SnapshotEstado;
import servidor_central.protocolo.Mensaje;

public interface ICoordinadorServidor {

    boolean registrarCliente(String dni);

    int conectarPuesto(String host);

    Turno llamarSiguiente(int idPuesto);

    int reNotificar(int idPuesto);

    Turno eliminarCliente(int idPuesto);

    boolean suscribirPuestoAEventos(Socket socket);

    void replicarEstadoAPantalla();

    // --- Replicacion par-a-par ---

    /** Toma un snapshot atomico del estado actual. */
    SnapshotEstado tomarSnapshot();

    /**
     * Bajo el monitor del coordinador: toma snapshot, lo envia al par via la
     * fachada de disponibilidad recibida (sin mutaciones en curso) y luego
     * registra la fachada para que cada mutacion futura emita un mensaje
     * REPLICAR|... a traves de ella.
     */
    void enlazarDisponibilidad(IDisponibilidad disponibilidad);

    /** Desregistra la fachada (ej: la conexion al par se cayo). */
    void desenlazarDisponibilidad();

    /** Sobrescribe el estado local con el snapshot recibido del primario. */
    void aplicarSnapshot(SnapshotEstado snapshot);

    /**
     * Aplica un mensaje REPLICAR recibido del primario (sin emitir eventos a
     * clientes ni propagar la replicacion).
     */
    void aplicarReplicacion(Mensaje replicar);
}
