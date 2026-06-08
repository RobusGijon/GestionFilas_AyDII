package servidor_central.persistencia;

import servidor_central.interfaces.IEstrategiaEncriptacion;

/**
 * Sello de integridad del estado persistido. Cada formato embebe una marca
 * conocida ({@link #SELLO}) dentro de su propia estructura (un campo en JSON, un
 * atributo en XML, una linea en TXT), de modo que el archivo en claro sigue siendo
 * JSON/XML/TXT valido. Al leer, tras descifrar, se verifica que la marca aparezca
 * en el contenido; si no aparece, la clave/cifrado configurado no corresponde al
 * archivo (o esta corrupto) y se lanza {@link ClaveInvalidaException}.
 *
 * Funciona para los tres formatos y los cuatro algoritmos:
 *  - AES con clave incorrecta lanza al descifrar (BadPadding) -> se captura.
 *  - XOR/Cesar/Vigenere con clave incorrecta producen basura que no contiene la
 *    marca -> falla el sello.
 *  - Sin cifrado, el sello detecta un archivo corrupto o ajeno.
 */
public final class SelloPersistencia {

    public static final String SELLO = "TP4-ESTADO-V1";

    private SelloPersistencia() {
    }

    /**
     * Cifra el contenido si hay estrategia. El sello ya viene embebido por el
     * escritor dentro de la estructura del formato, asi que aqui no se antepone nada.
     */
    public static String cifrar(String contenido, IEstrategiaEncriptacion encriptacion) {
        return (encriptacion == null) ? contenido : encriptacion.encriptar(contenido);
    }

    /**
     * Descifra (si hay estrategia), verifica que el sello este presente y devuelve
     * el contenido tal cual (el sello embebido es inofensivo para el parser del
     * formato). Lanza {@link ClaveInvalidaException} si no se puede descifrar o el
     * sello no aparece.
     */
    public static String descifrarYVerificar(String enDisco, IEstrategiaEncriptacion encriptacion) {
        String plano;
        try {
            plano = (encriptacion == null) ? enDisco : encriptacion.desencriptar(enDisco);
        } catch (RuntimeException ex) {
            throw new ClaveInvalidaException(
                    "no se pudo descifrar el estado persistido (clave incorrecta o archivo corrupto)", ex);
        }
        if (!plano.contains(SELLO)) {
            throw new ClaveInvalidaException(
                    "sello de integridad invalido: la clave o el metodo de cifrado no coincide con el archivo");
        }
        return plano;
    }
}
