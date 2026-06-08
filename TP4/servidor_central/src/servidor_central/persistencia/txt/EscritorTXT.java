package servidor_central.persistencia.txt;

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
 * Persiste el {@link ServerDTO} en texto plano, una entidad por linea:
 *   PUESTO|&lt;id&gt;|&lt;host&gt;
 *   FILA|&lt;dni&gt;|&lt;intentos&gt;
 *   LLAMADO|&lt;idPuesto&gt;|&lt;dni&gt;|&lt;intentos&gt;
 *
 * Con encriptacion el contenido se cifra y se guarda en {@code data/encriptada};
 * sin encriptacion se guarda en claro en {@code data/default}.
 */
public class EscritorTXT implements IPersistenciaEscritor {

    private final IEstrategiaEncriptacion encriptacion;

    public EscritorTXT() {
        this(null);
    }

    public EscritorTXT(IEstrategiaEncriptacion encriptacion) {
        this.encriptacion = encriptacion;
    }

    @Override
    public void guardar(String nombreArchivo, ServerDTO estado) {
        StringBuilder sb = new StringBuilder();

        // Sello de integridad embebido como primera linea: el lector ignora las
        // lineas que no reconoce, solo se verifica su presencia.
        sb.append(SelloPersistencia.SELLO).append('\n');

        for (PuestoInfo p : estado.getPuestosActivos()) {
            sb.append("PUESTO|")
              .append(p.getIdPuesto()).append('|')
              .append(p.getHostRemoto() == null ? "" : p.getHostRemoto())
              .append('\n');
            // Historial de atendidos del puesto, justo despues de su PUESTO para
            // que el lector lo enganche al PuestoInfo ya creado.
            for (ClienteAtendido c : p.getHistorialAtendidos()) {
                sb.append("ATENDIDO|")
                  .append(p.getIdPuesto()).append('|')
                  .append(c.getDni()).append('|')
                  .append(c.getHora())
                  .append('\n');
            }
        }
        for (Turno t : estado.getColaEspera()) {
            sb.append("FILA|")
              .append(t.getDni()).append('|')
              .append(t.getIntentosLlamados())
              .append('\n');
        }
        for (Map.Entry<Integer, Turno> e : estado.getTurnosEnPuesto().entrySet()) {
            Turno t = e.getValue();
            sb.append("LLAMADO|")
              .append(e.getKey()).append('|')
              .append(t.getDni()).append('|')
              .append(t.getIntentosLlamados())
              .append('\n');
        }
        for (Turno t : estado.getHistorialLlamados()) {
            sb.append("HIST|")
              .append(t.getIdPuestoAsignado() == null ? 0 : t.getIdPuestoAsignado()).append('|')
              .append(t.getDni())
              .append('\n');
        }

        String contenido = SelloPersistencia.cifrar(sb.toString(), encriptacion);

        try {
            EscrituraAtomica.escribir(Path.of("data", nombreArchivo), contenido);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar el estado en " + nombreArchivo, ex);
        }
    }
}
