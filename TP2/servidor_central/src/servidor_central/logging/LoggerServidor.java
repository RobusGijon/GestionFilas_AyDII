package servidor_central.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import servidor_central.datos.PuestoInfo;
import servidor_central.datos.Turno;
import servidor_central.interfaces.IGestorClientesLlamados;
import servidor_central.interfaces.IGestorFilaEspera;
import servidor_central.interfaces.IGestorPuestos;

public class LoggerServidor implements ILogger {

    private static final int INNER_WIDTH = 39;
    private static final int CONTENT_WIDTH = INNER_WIDTH - 2;

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    private String ts() {
        return "[" + LocalTime.now().format(fmt) + "]";
    }

    @Override
    public synchronized void logInfo(String mensaje) {
        System.out.println(ts() + " INFO  " + mensaje);
    }

    @Override
    public synchronized void logOperacion(String operacion, String detalle, boolean ok) {
        System.out.println(ts() + " " + operacion + " " + detalle + " \u2192 " + (ok ? "OK" : "FAIL"));
    }

    @Override
    public synchronized void logEvento(String tipo, String detalle) {
        System.out.println(ts() + " EVENTO " + tipo + " " + detalle);
    }

    @Override
    public synchronized void logError(String operacion, Throwable e) {
        String msg = (e.getMessage() != null) ? e.getMessage() : e.getClass().getSimpleName();
        System.err.println(ts() + " ERROR en " + operacion + ": " + msg);
        if (esErrorDeNegocio(e)) {
            return;
        }
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        System.err.println(sw.toString());
    }

    private boolean esErrorDeNegocio(Throwable e) {
        return e instanceof IllegalStateException
                || e instanceof IllegalArgumentException
                || e instanceof NoSuchElementException;
    }

    @Override
    public synchronized void logEstado(IGestorFilaEspera gfe, IGestorClientesLlamados gcl, IGestorPuestos gp) {
        StringBuilder out = new StringBuilder();

        List<Turno> cola = gfe.snapshot();
        out.append(topBorder("FILA DE ESPERA (" + cola.size() + ")")).append("\n");
        if (cola.isEmpty()) {
            out.append(linea("(vacia)")).append("\n");
        } else {
            int i = 1;
            for (Turno t : cola) {
                out.append(linea(i + ". DNI " + t.getDni())).append("\n");
                i++;
            }
        }
        out.append(bottomBorder()).append("\n");

        Collection<Turno> enAtencion = gcl.snapshot();
        out.append(topBorder("EN ATENCION (" + enAtencion.size() + ")")).append("\n");
        if (enAtencion.isEmpty()) {
            out.append(linea("(vacia)")).append("\n");
        } else {
            List<Turno> ordenados = new ArrayList<>(enAtencion);
            ordenados.sort(Comparator.comparingInt(t -> t.getIdPuestoAsignado() != null ? t.getIdPuestoAsignado() : 0));
            for (Turno t : ordenados) {
                out.append(linea("Puesto " + t.getIdPuestoAsignado()
                        + " \u2192 DNI " + t.getDni()
                        + " (intento " + t.getIntentosLlamados() + "/3)")).append("\n");
            }
        }
        out.append(bottomBorder()).append("\n");

        List<PuestoInfo> puestos = gp.snapshot();
        out.append(topBorder("PUESTOS ACTIVOS (" + puestos.size() + ")")).append("\n");
        if (puestos.isEmpty()) {
            out.append(linea("(vacia)")).append("\n");
        } else {
            for (PuestoInfo p : puestos) {
                out.append(linea("Puesto " + p.getIdPuesto() + " \u2192 " + p.getHostRemoto())).append("\n");
            }
        }
        out.append(bottomBorder()).append("\n");

        System.out.print(out);
    }

    private String linea(String texto) {
        String t = truncar(texto, CONTENT_WIDTH);
        StringBuilder sb = new StringBuilder("\u2502 ");
        sb.append(t);
        for (int i = t.length(); i < CONTENT_WIDTH; i++) sb.append(' ');
        sb.append(" \u2502");
        return sb.toString();
    }

    private String topBorder(String titulo) {
        String cabecera = "\u2500 " + titulo + " ";
        StringBuilder sb = new StringBuilder("\u250C");
        sb.append(cabecera);
        int dashes = INNER_WIDTH - cabecera.length();
        if (dashes < 0) dashes = 0;
        for (int i = 0; i < dashes; i++) sb.append("\u2500");
        sb.append("\u2510");
        return sb.toString();
    }

    private String bottomBorder() {
        StringBuilder sb = new StringBuilder("\u2514");
        for (int i = 0; i < INNER_WIDTH; i++) sb.append("\u2500");
        sb.append("\u2518");
        return sb.toString();
    }

    private String truncar(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "\u2026";
    }
}
