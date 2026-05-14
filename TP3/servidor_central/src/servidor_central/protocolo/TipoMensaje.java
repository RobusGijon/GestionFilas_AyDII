package servidor_central.protocolo;

public enum TipoMensaje {

    // Requests cliente -> servidor
    REGISTRAR_CLIENTE,
    CONECTAR_PUESTO,
    LLAMAR_SIGUIENTE,
    RENOTIFICAR,
    ELIMINAR_CLIENTE,
    SUSCRIBIR_PANTALLA,
    SUSCRIBIR_PUESTO,

    // Responses servidor -> cliente
    OK,
    ERROR,

    // Events servidor -> pantalla (push)
    EVENTO_LLAMADO,
    EVENTO_RENOTIFICACION,
    EVENTO_AUSENTE,

    // Events servidor -> puesto (push)
    EVENTO_FILA_ACTUALIZADA,

    // Mensajes par-a-par (alta disponibilidad)
    HEARTBEAT,
    HOLA_PAR,
    SNAPSHOT,
    REPLICAR
}