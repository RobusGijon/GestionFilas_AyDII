package servidor_central.persistencia.json;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;

import servidor_central.datos.ClienteAtendido;
import servidor_central.datos.PuestoInfo;
import servidor_central.datos.Turno;
import servidor_central.interfaces.IEstrategiaEncriptacion;
import servidor_central.interfaces.factory.IPersistenciaEscritor;
import servidor_central.persistencia.EscrituraAtomica;
import servidor_central.persistencia.SelloPersistencia;
import servidor_central.persistencia.dto.ServerDTO;

/**
 * Persiste el {@link ServerDTO} en JSON (serializado a mano, sin dependencias):
 * <pre>
 * {
 *   "puestosActivos": [ { "idPuesto": 1, "hostRemoto": "127.0.0.1:5000" } ],
 *   "colaEspera":     [ { "dni": "123", "intentosLlamados": 1 } ],
 *   "turnosEnPuesto": [ { "idPuesto": 2, "dni": "456", "intentosLlamados": 2 } ]
 * }
 * </pre>
 *
 * Con encriptacion el contenido se cifra y se guarda en {@code data/encriptada};
 * sin encriptacion se guarda en claro en {@code data/default}.
 */
public class EscritorJSON implements IPersistenciaEscritor {

    private final IEstrategiaEncriptacion encriptacion;

    public EscritorJSON() {
        this(null);
    }

    public EscritorJSON(IEstrategiaEncriptacion encriptacion) {
        this.encriptacion = encriptacion;
    }

    @Override
    public void guardar(String nombreArchivo, ServerDTO estado) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // Sello de integridad embebido como campo: el archivo sigue siendo JSON
        // valido y el lector lo ignora (solo verifica su presencia).
        sb.append("  \"_sello\": ").append(str(SelloPersistencia.SELLO)).append(",\n");

        sb.append("  \"puestosActivos\": [");
        boolean primero = true;
        for (PuestoInfo p : estado.getPuestosActivos()) {
            sb.append(primero ? "\n" : ",\n");
            primero = false;
            sb.append("    { \"idPuesto\": ").append(p.getIdPuesto())
              .append(", \"hostRemoto\": ").append(str(p.getHostRemoto() == null ? "" : p.getHostRemoto()))
              .append(", \"historialAtendidos\": [");
            boolean primerAt = true;
            for (ClienteAtendido c : p.getHistorialAtendidos()) {
                sb.append(primerAt ? "" : ", ");
                primerAt = false;
                sb.append("{ \"dni\": ").append(str(c.getDni()))
                  .append(", \"hora\": ").append(str(c.getHora()))
                  .append(" }");
            }
            sb.append("] }");
        }
        sb.append(primero ? "],\n" : "\n  ],\n");

        sb.append("  \"colaEspera\": [");
        primero = true;
        for (Turno t : estado.getColaEspera()) {
            sb.append(primero ? "\n" : ",\n");
            primero = false;
            sb.append("    { \"dni\": ").append(str(t.getDni()))
              .append(", \"intentosLlamados\": ").append(t.getIntentosLlamados())
              .append(" }");
        }
        sb.append(primero ? "],\n" : "\n  ],\n");

        sb.append("  \"turnosEnPuesto\": [");
        primero = true;
        for (Map.Entry<Integer, Turno> e : estado.getTurnosEnPuesto().entrySet()) {
            Turno t = e.getValue();
            sb.append(primero ? "\n" : ",\n");
            primero = false;
            sb.append("    { \"idPuesto\": ").append(e.getKey())
              .append(", \"dni\": ").append(str(t.getDni()))
              .append(", \"intentosLlamados\": ").append(t.getIntentosLlamados())
              .append(" }");
        }
        sb.append(primero ? "],\n" : "\n  ],\n");

        sb.append("  \"historialLlamados\": [");
        primero = true;
        for (Turno t : estado.getHistorialLlamados()) {
            sb.append(primero ? "\n" : ",\n");
            primero = false;
            sb.append("    { \"idPuesto\": ")
              .append(t.getIdPuestoAsignado() == null ? 0 : t.getIdPuestoAsignado())
              .append(", \"dni\": ").append(str(t.getDni()))
              .append(" }");
        }
        sb.append(primero ? "]\n" : "\n  ]\n");

        sb.append("}\n");

        String contenido = SelloPersistencia.cifrar(sb.toString(), encriptacion);

        try {
            EscrituraAtomica.escribir(Path.of("data", nombreArchivo), contenido);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar el estado en " + nombreArchivo, ex);
        }
    }

    /** Devuelve el valor como literal JSON string, escapando lo necesario. */
    private static String str(String v) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }
}
