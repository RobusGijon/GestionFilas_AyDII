package servidor_central.protocolo.encriptacion.estrategias;

import servidor_central.interfaces.IEstrategiaEncriptacion;

public class EncriptacionCesar implements IEstrategiaEncriptacion {
    private static final int BASE = 32;
    private static final int RANGO = 95;

    private final int desplazamiento;

    public EncriptacionCesar(int desplazamiento) {
        this.desplazamiento = desplazamiento;
    }

    public String encriptar(String texto) {
        return desplazar(texto, desplazamiento);
    }

    public String desencriptar(String texto) {
        return desplazar(texto, RANGO - (desplazamiento % RANGO));
    }

    private static String desplazar(String texto, int corrimiento) {
        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            if (c >= BASE && c < BASE + RANGO) {
                sb.append((char) ((c - BASE + corrimiento) % RANGO + BASE));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public boolean esClaveValida(String clave) throws IllegalArgumentException {
        
        try{
            int claveMetodo = Integer.parseInt(clave);
            if (claveMetodo >= 0 ) {
                return true;
            }
            throw new IllegalArgumentException("La clave del metodo de Cesar no puede ser negativa");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La clave del metodo de Cesar no es un numero");
        }
    }
}
