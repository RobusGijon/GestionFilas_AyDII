package terminal_registro.protocolo.encriptacion;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import terminal_registro.interfaces.IEstrategiaEncriptacion;
import terminal_registro.protocolo.encriptacion.estrategias.EncriptacionAES;
import terminal_registro.protocolo.encriptacion.estrategias.EncriptacionCesar;
import terminal_registro.protocolo.encriptacion.estrategias.EncriptacionVigenere;
import terminal_registro.protocolo.encriptacion.estrategias.EncriptacionXOR;

public final class Estrategias {

    private static final String RUTA_ENV = "../shared/.env";

    private Estrategias() {}

    public static IEstrategiaEncriptacion getEstrategia(String[] args) throws IllegalArgumentException {

        // Sin encriptacion
        if (args.length == 0) return null;

        // Pasa el nombre de la encriptacion, usa la key en shared
        String nombreEstrategia = args[0].trim().toUpperCase();

        Map<String, String> env;
        try {
            env = leerEnv(RUTA_ENV);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "[Encriptacion] No se pudo leer la configuracion en " + RUTA_ENV + ": " + e.getMessage());
        }

        String claveEstrategia = obtenerClave(env, nombreEstrategia + "_KEY");
        IEstrategiaEncriptacion estrategia = crear(nombreEstrategia, claveEstrategia);

        if (!estrategia.esClaveValida(claveEstrategia)) {
            throw new IllegalArgumentException("[Encriptacion] Clave invalida para el metodo");
        }

        return estrategia;
    }

    private static IEstrategiaEncriptacion crear(String nombre, String clave) {
        switch (nombre) {
            case "AES":
                return new EncriptacionAES(clave);
            case "XOR":
                return new EncriptacionXOR(clave);
            case "CESAR":
                return new EncriptacionCesar(Integer.parseInt(clave));
            case "VIGENERE":
                return new EncriptacionVigenere(clave);
            default:
                throw new IllegalArgumentException("[Encriptacion] Estrategia de encriptacion desconocida: " + nombre);
        }
    }

    // Devuelve el valor de una variable del .env; lanza excepcion si falta o esta vacia
    private static String obtenerClave(Map<String, String> env, String nombreVariable) {
        String valor = env.get(nombreVariable);
        if (valor == null || valor.isEmpty()) {
            throw new IllegalStateException("Variable " + nombreVariable + " no configurada en " + RUTA_ENV);
        }
        return valor;
    }

    private static Map<String, String> leerEnv(String ruta) throws IOException {
        Map<String, String> env = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) {
                    continue;
                }
                int eq = linea.indexOf('=');
                if (eq < 0) {
                    continue;
                }
                String k = linea.substring(0, eq).trim();
                String v = linea.substring(eq + 1).trim();
                env.put(k, v);
            }
        }
        return env;
    }
}
