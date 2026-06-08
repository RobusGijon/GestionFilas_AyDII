package pantalla.protocolo.encriptacion.estrategias;

import pantalla.interfaces.IEstrategiaEncriptacion;

public class EncriptacionVigenere implements IEstrategiaEncriptacion {
    private final String clave;

    public EncriptacionVigenere(String clave) {
        this.clave = clave.toLowerCase();
    }

    public String encriptar(String texto) {
        StringBuilder sb = new StringBuilder();
        int j = 0;
        for (char c : texto.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                int k = clave.charAt(j % clave.length()) - 'a';
                sb.append((char) ((c - base + k) % 26 + base));
                j++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public String desencriptar(String texto) {
        StringBuilder sb = new StringBuilder();
        int j = 0;
        for (char c : texto.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                int k = clave.charAt(j % clave.length()) - 'a';
                sb.append((char) ((c - base + 26 - k) % 26 + base));
                j++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public boolean esClaveValida(String clave) throws IllegalArgumentException {

        if (clave == null || clave.isEmpty()) {
            throw new IllegalArgumentException("La clave de Vigenere no puede ser vacia");
        }
        for (char c : clave.toCharArray()) {
            if (!Character.isLetter(c)) {
                throw new IllegalArgumentException("La clave de Vigenere debe contener solo letras");
            }
        }
        return true;
    }
}