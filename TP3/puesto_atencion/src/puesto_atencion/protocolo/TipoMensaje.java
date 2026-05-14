package puesto_atencion.protocolo;

public enum TipoMensaje {

    // Pedidos operador 
    REGISTRAR_CLIENTE,
    CONECTAR_PUESTO,
    LLAMAR_SIGUIENTE,
    RENOTIFICAR,
    ELIMINAR_CLIENTE,
    SUSCRIBIR_PANTALLA,
    SUSCRIBIR_PUESTO,

    // Respuestas servidor 
    OK,
    ERROR,

    // Eventos servidor -> pantalla (push)
    EVENTO_LLAMADO,
    EVENTO_RENOTIFICACION,
    EVENTO_AUSENTE,

    // Eventos servidor -> puesto (push)
    EVENTO_FILA_ACTUALIZADA
}
