package puesto_atencion.protocolo;

public enum TipoMensaje {

    // Requests cliente -> servidor
    REGISTRAR_CLIENTE,
    CONECTAR_PUESTO,
    LLAMAR_SIGUIENTE,
    RENOTIFICAR,
    ELIMINAR_CLIENTE,
    SUSCRIBIR_PANTALLA,

    // Responses servidor -> cliente
    OK,
    ERROR,

    // Events servidor -> pantalla (push)
    EVENTO_LLAMADO,
    EVENTO_RENOTIFICACION,
    EVENTO_AUSENTE
}
