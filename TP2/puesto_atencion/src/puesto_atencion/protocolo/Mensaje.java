package puesto_atencion.protocolo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Mensaje {

    private final TipoMensaje tipo;
    private final List<String> args;

    public Mensaje(TipoMensaje tipo, List<String> args) {
        this.tipo = tipo;
        this.args = args;
    }

    public Mensaje(TipoMensaje tipo, String... args) {
        this(tipo, Arrays.asList(args));
    }

    public static Mensaje parse(String linea) {
        if (linea == null || linea.isEmpty()) {
            throw new IllegalArgumentException("Linea vacia");
        }
        String[] partes = linea.split("\\|", -1);
        TipoMensaje tipo;
        try {
            tipo = TipoMensaje.valueOf(partes[0]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de mensaje desconocido: " + partes[0]);
        }
        List<String> args;
        if (partes.length > 1) {
            args = Arrays.asList(Arrays.copyOfRange(partes, 1, partes.length));
        } else {
            args = Collections.emptyList();
        }
        return new Mensaje(tipo, args);
    }

    public String serializar() {
        StringBuilder sb = new StringBuilder(tipo.name());
        for (String a : args) {
            sb.append("|").append(a);
        }
        return sb.toString();
    }

    public TipoMensaje getTipo() {
        return tipo;
    }

    public List<String> getArgs() {
        return args;
    }

    public String getArg(int i) {
        if (i < 0 || i >= args.size()) {
            throw new IndexOutOfBoundsException("Arg " + i + " no existe en mensaje " + tipo);
        }
        return args.get(i);
    }

    @Override
    public String toString() {
        return serializar();
    }
}
