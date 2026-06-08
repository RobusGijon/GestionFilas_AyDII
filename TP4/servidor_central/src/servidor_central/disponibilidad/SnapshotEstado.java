package servidor_central.disponibilidad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import servidor_central.datos.ClienteAtendido;
import servidor_central.datos.PuestoInfo;
import servidor_central.datos.Turno;
import servidor_central.protocolo.mensajes.Mensaje;
import servidor_central.protocolo.mensajes.TipoMensaje;

public class SnapshotEstado {

    private final List<Turno> fila;
    private final Map<Integer, Turno> llamados;
    private final List<PuestoInfo> puestos;
    private final int proximoIdPuesto;

    public SnapshotEstado(List<Turno> fila,
                          Map<Integer, Turno> llamados,
                          List<PuestoInfo> puestos,
                          int proximoIdPuesto) {
        this.fila = fila;
        this.llamados = llamados;
        this.puestos = puestos;
        this.proximoIdPuesto = proximoIdPuesto;
    }

    public List<Turno>          getFila()             { return fila; }
    public Map<Integer, Turno>  getLlamados()         { return llamados; }
    public List<PuestoInfo>     getPuestos()          { return puestos; }
    public int                  getProximoIdPuesto()  { return proximoIdPuesto; }

    /**
     * Convierte el snapshot a una lista ordenada de mensajes que se envian
     * por el canal par-a-par. El receptor procesa los mensajes con un Builder
     * y, al recibir SNAPSHOT|END, invoca aplicar.
     *
     * Formato de cada Mensaje:
     *   SNAPSHOT|BEGIN
     *   SNAPSHOT|PROXIMO_ID|<n>
     *   SNAPSHOT|PUESTO|<idPuesto>|<host>
     *   SNAPSHOT|FILA|<dni>|<intentos>
     *   SNAPSHOT|LLAMADO|<idPuesto>|<dni>|<intentos>
     *   SNAPSHOT|END
     */
    public List<Mensaje> serializar() {
        List<Mensaje> out = new ArrayList<>();
        out.add(new Mensaje(TipoMensaje.SNAPSHOT, "BEGIN"));
        out.add(new Mensaje(TipoMensaje.SNAPSHOT, "PROXIMO_ID", String.valueOf(proximoIdPuesto)));
        for (PuestoInfo p : puestos) {
            out.add(new Mensaje(TipoMensaje.SNAPSHOT, "PUESTO",
                String.valueOf(p.getIdPuesto()),
                p.getHostRemoto() == null ? "" : p.getHostRemoto()));
            // (F-HA) historial de atendidos del puesto, cronologico.
            for (ClienteAtendido c : p.getHistorialAtendidos()) {
                out.add(new Mensaje(TipoMensaje.SNAPSHOT, "ATENDIDO",
                    String.valueOf(p.getIdPuesto()), c.getDni(), c.getHora()));
            }
        }
        for (Turno t : fila) {
            out.add(new Mensaje(TipoMensaje.SNAPSHOT, "FILA",
                t.getDni(), String.valueOf(t.getIntentosLlamados())));
        }
        for (Map.Entry<Integer, Turno> e : llamados.entrySet()) {
            Turno t = e.getValue();
            out.add(new Mensaje(TipoMensaje.SNAPSHOT, "LLAMADO",
                String.valueOf(e.getKey()),
                t.getDni(),
                String.valueOf(t.getIntentosLlamados())));
        }
        out.add(new Mensaje(TipoMensaje.SNAPSHOT, "END"));
        return out;
    }

    /**
     * Acumulador incremental usado por el secundario para reconstruir el
     * snapshot a medida que llegan los mensajes SNAPSHOT|...
     */
    public static final class Builder {
        private int proximoIdPuesto = 1;
        private final List<Turno> fila = new ArrayList<>();
        private final Map<Integer, Turno> llamados = new HashMap<>();
        private final List<PuestoInfo> puestos = new ArrayList<>();
        private boolean iniciado = false;

        /** Devuelve true si este mensaje es un END (snapshot listo para construir). */
        public boolean procesar(Mensaje m) {
            if (m.getTipo() != TipoMensaje.SNAPSHOT || m.getArgs().isEmpty()) {
                return false;
            }
            String sub = m.getArg(0);
            switch (sub) {
                case "BEGIN":
                    iniciado = true;
                    proximoIdPuesto = 1;
                    fila.clear();
                    llamados.clear();
                    puestos.clear();
                    return false;
                case "PROXIMO_ID":
                    proximoIdPuesto = Integer.parseInt(m.getArg(1));
                    return false;
                case "PUESTO":
                    puestos.add(new PuestoInfo(Integer.parseInt(m.getArg(1)), m.getArg(2)));
                    return false;
                case "ATENDIDO": {
                    int idPuesto = Integer.parseInt(m.getArg(1));
                    for (PuestoInfo p : puestos) {
                        if (p.getIdPuesto() == idPuesto) {
                            p.agregarAtendido(new ClienteAtendido(m.getArg(2), m.getArg(3)));
                            break;
                        }
                    }
                    return false;
                }
                case "FILA":
                    fila.add(new Turno(m.getArg(1), Integer.parseInt(m.getArg(2)), null));
                    return false;
                case "LLAMADO":
                    int idPuesto = Integer.parseInt(m.getArg(1));
                    Turno t = new Turno(m.getArg(2), Integer.parseInt(m.getArg(3)), idPuesto);
                    llamados.put(idPuesto, t);
                    return false;
                case "END":
                    return iniciado;
                default:
                    return false;
            }
        }

        public SnapshotEstado build() {
            return new SnapshotEstado(fila, llamados, puestos, proximoIdPuesto);
        }

        public void reset() {
            iniciado = false;
            proximoIdPuesto = 1;
            fila.clear();
            llamados.clear();
            puestos.clear();
        }
    }
}
