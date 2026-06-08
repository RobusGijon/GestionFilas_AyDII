package pantalla.protocolo.encriptacion.estrategias;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import pantalla.interfaces.IEstrategiaEncriptacion;

import java.util.Base64;

public class EncriptacionAES implements IEstrategiaEncriptacion {
    private final SecretKeySpec key;

    public EncriptacionAES(String claveDe16Bytes) {
        this.key = new SecretKeySpec(claveDe16Bytes.getBytes(), "AES");
    }

    public String encriptar(String texto) {
        try {
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(c.doFinal(texto.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Error al encriptar con AES", e);
        }
    }

    public String desencriptar(String textoBase64) {
        try {
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, key);
            return new String(c.doFinal(Base64.getDecoder().decode(textoBase64)));
        } catch (Exception e) {
            throw new RuntimeException("Error al desencriptar con AES", e);
        }
    }

    @Override
    public boolean esClaveValida(String clave) throws IllegalArgumentException {

        if (clave == null || clave.isEmpty()) {
            throw new IllegalArgumentException("La clave AES no puede ser vacia");
        }
        int largo = clave.getBytes().length;
        if (largo != 16 && largo != 24 && largo != 32) {
            throw new IllegalArgumentException("La clave AES debe tener 16, 24 o 32 bytes");
        }
        return true;
    }


}