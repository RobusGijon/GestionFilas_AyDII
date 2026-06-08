package servidor_central.conexion;

import java.io.IOException;

import servidor_central.datos.Turno;
import servidor_central.interfaces.ICanalMensaje;
import servidor_central.interfaces.IPantalla;
import servidor_central.logging.ILogger;
import servidor_central.protocolo.mensajes.Mensaje;
import servidor_central.protocolo.mensajes.TipoMensaje;

public class PublicadorPantalla implements IPantalla {

    private ICanalMensaje canal;
    private final ILogger log;

    public PublicadorPantalla(ILogger log) {
        this.log = log;
    }

    @Override
    public synchronized boolean agregarObservador(ICanalMensaje canal) {
        cerrarActual();
        if (canal == null || !canal.estaVivo()) {
            return false;
        }
        this.canal = canal;
        return true;
    }

    @Override
    public synchronized void notificarLlamado(Turno t) {
        if (!estaConectadaInterno()) return;
        Mensaje m = new Mensaje(TipoMensaje.EVENTO_LLAMADO,
                t.getDni(),
                String.valueOf(t.getIdPuestoAsignado()));
        enviar(m, "EVENTO_LLAMADO",
                "dni=" + t.getDni() + " idPuesto=" + t.getIdPuestoAsignado());
    }

    @Override
    public synchronized void notificarRenotificacion(Turno t) {
        if (!estaConectadaInterno()) return;
        Mensaje m = new Mensaje(TipoMensaje.EVENTO_RENOTIFICACION,
                t.getDni(),
                String.valueOf(t.getIdPuestoAsignado()),
                String.valueOf(t.getIntentosLlamados()));
        enviar(m, "EVENTO_RENOTIFICACION",
                "dni=" + t.getDni()
                        + " idPuesto=" + t.getIdPuestoAsignado()
                        + " intento=" + t.getIntentosLlamados());
    }

    @Override
    public synchronized void notificarHistorial(java.util.List<Turno> historial) {
        if (!estaConectadaInterno() || historial == null || historial.isEmpty()) return;
        // Un unico mensaje con todos los pares dni|idPuesto (orden cronologico).
        // Asi el replay es atomico e idempotente: la pantalla limpia y reconstruye.
        java.util.List<String> args = new java.util.ArrayList<>();
        for (Turno t : historial) {
            args.add(t.getDni());
            args.add(String.valueOf(t.getIdPuestoAsignado()));
        }
        Mensaje m = new Mensaje(TipoMensaje.EVENTO_HISTORIAL, args);
        enviar(m, "EVENTO_HISTORIAL", "entradas=" + historial.size());
    }

    @Override
    public synchronized void notificarAusente(int idPuesto) {
        if (!estaConectadaInterno()) return;
        Mensaje m = new Mensaje(TipoMensaje.EVENTO_AUSENTE,
                String.valueOf(idPuesto));
        enviar(m, "EVENTO_AUSENTE", "idPuesto=" + idPuesto);
    }

    @Override
    public synchronized boolean estaConectada() {
        return estaConectadaInterno();
    }

    private boolean estaConectadaInterno() {
        return canal != null && canal.estaVivo();
    }

    private void enviar(Mensaje m, String tipoLog, String detalleLog) {
        if (!canal.enviar(m)) {
            log.logError("EMITIR_" + tipoLog,
                    new IOException("Fallo al escribir al canal de Pantalla"));
            limpiar();
            return;
        }
        log.logEvento(tipoLog, detalleLog);
    }

    private void cerrarActual() {
        if (canal != null) {
            canal.cerrar();
        }
        limpiar();
    }

    private void limpiar() {
        canal = null;
    }
}
