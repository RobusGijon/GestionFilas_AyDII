package servidor_central.interfaces;

import java.util.List;
import java.util.Set;

import servidor_central.datos.ClienteAtendido;
import servidor_central.datos.PuestoInfo;

public interface IGestorPuestos {

    PuestoInfo registrarPuesto(String hostRemoto);

    void eliminarPuesto(int idPuesto);

    boolean existe(int idPuesto);

    /** Devuelve el puesto con ese id, o null si no existe. */
    PuestoInfo obtener(int idPuesto);

    List<PuestoInfo> snapshot();

    int cantidad();

    void registrarPuestoConId(int idPuesto, String hostRemoto);

    // -------- Estado de conexion (ASIGNADO / DESASIGNADO) --------

    /** Registra una conexion viva del puesto (lo deja ASIGNADO). */
    void marcarConexion(int idPuesto);

    /** Da de baja una conexion; si era la ultima, el puesto queda DESASIGNADO. */
    void marcarDesconexion(int idPuesto);

    /** true si el puesto tiene al menos una conexion viva. */
    boolean estaAsignado(int idPuesto);

    /**
     * Devuelve el puesto con ese id si existe y esta DESASIGNADO (apto para que un
     * operador lo reclame por id), o null.
     */
    PuestoInfo reclamarPorId(int idPuesto);

    /**
     * Elige un puesto DESASIGNADO para entregar a un operador que conecta sin id:
     * primero los que tienen un turno en atencion pendiente (menor id entre esos),
     * luego los vacios (menor id). Devuelve null si no hay ninguno libre.
     */
    PuestoInfo reclamarDesasignado(Set<Integer> idsConTurno);

    // -------- Historial de atendidos por puesto --------

    void agregarAtendido(int idPuesto, ClienteAtendido c);

    List<ClienteAtendido> historialDe(int idPuesto);

    int getProximoId();

    void setProximoId(int proximoId);

    void limpiar();
}
