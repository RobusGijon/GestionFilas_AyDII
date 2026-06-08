package servidor_central.interfaces;

import servidor_central.datos.EstadoReconexionPuesto;
import servidor_central.datos.Turno;
import servidor_central.disponibilidad.SnapshotEstado;
import servidor_central.protocolo.mensajes.Mensaje;

public interface ICoordinadorServidor {

    boolean registrarCliente(String dni);

    /**
     * Conecta un puesto. Si {@code idDeseado > 0} intenta reclamar ese slot exacto
     * (puesto vivo que reconecta); si no, reparte un slot DESASIGNADO (con turno
     * primero) o crea uno nuevo. Devuelve el id asignado mas el estado a recuperar
     * (turno en atencion + historial).
     */
    EstadoReconexionPuesto conectarPuesto(String host, int idDeseado);

    /**
     * Estado actual de un puesto ya conectado (sin alterar el conteo de
     * conexiones): se usa cuando llega un re-CONECTAR sobre una conexion que ya
     * estaba registrada.
     */
    EstadoReconexionPuesto estadoPuesto(int idPuesto);

    /** Da de baja una conexion del puesto; si era la ultima, queda DESASIGNADO. */
    void notificarDesconexionPuesto(int idPuesto);

    Turno llamarSiguiente(int idPuesto);

    int reNotificar(int idPuesto);

    Turno eliminarCliente(int idPuesto);

    boolean suscribirPuestoAEventos(int idPuesto, ICanalMensaje canal);

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

    /**
     * Activa o desactiva la persistencia a disco. Solo el PRIMARIO/STANDALONE
     * persiste; el SECUNDARIO no (la maneja el primario). Lo controla el
     * {@code SupervisorRol} en las transiciones de rol.
     */
    void setPersistenciaActiva(boolean activa);

    /** Fuerza un guardado del estado actual (p. ej. al promocionar a primario). */
    void persistirAhora();
}
