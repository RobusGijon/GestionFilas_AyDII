package puesto_atencion.conexion;

/**
 * El servidor respondio con un mensaje ERROR de nivel aplicacion: una regla de
 * negocio rechazo el pedido (p.ej. turno sin cliente en atencion, maximo de
 * reintentos alcanzado). A diferencia de una falla de transporte, el nodo esta
 * sano y respondio, asi que no tiene sentido reintentar ni hacer failover: el
 * gestor redundante la propaga de inmediato al operador.
 */
public class ErrorServidorException extends Exception {

    public ErrorServidorException(String mensaje) {
        super(mensaje);
    }
}
