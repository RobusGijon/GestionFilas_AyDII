package servidor_central.persistencia;

/**
 * Se lanza al leer un archivo de estado existente cuyo sello de integridad no
 * valida: la clave o el metodo de cifrado configurado no coinciden con los que
 * se usaron para escribirlo (o el archivo esta corrupto). El servidor aborta el
 * arranque en vez de continuar con estado vacio y sobrescribirlo.
 */
public class ClaveInvalidaException extends RuntimeException {

    public ClaveInvalidaException(String mensaje) {
        super(mensaje);
    }

    public ClaveInvalidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
