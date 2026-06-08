package puesto_atencion.protocolo.encriptacion.estrategias;
import puesto_atencion.interfaces.IEstrategiaEncriptacion;

public class EncriptacionXOR implements IEstrategiaEncriptacion {
    private final byte[] clave;

    public EncriptacionXOR(String clave) {
        this.clave = clave.getBytes();
    }

    public String encriptar(String texto) {
        byte[] datos = texto.getBytes();
        byte[] out = new byte[datos.length];
        for (int i = 0; i < datos.length; i++) {
            out[i] = (byte) (datos[i] ^ clave[i % clave.length]);
        }
        return java.util.Base64.getEncoder().encodeToString(out);
    }

    public String desencriptar(String textoBase64) {
        byte[] datos = java.util.Base64.getDecoder().decode(textoBase64);
        byte[] out = new byte[datos.length];
        for (int i = 0; i < datos.length; i++) {
            out[i] = (byte) (datos[i] ^ clave[i % clave.length]);
        }
        return new String(out);
    }

    @Override
    public boolean esClaveValida(String clave) throws IllegalArgumentException {

        if (clave == null || clave.isEmpty()) {
            throw new IllegalArgumentException("La clave XOR no puede ser vacia");
        }
        return true;
    }
}