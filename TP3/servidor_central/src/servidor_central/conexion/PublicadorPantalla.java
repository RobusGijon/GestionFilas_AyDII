package servidor_central.conexion;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import servidor_central.datos.Turno;
import servidor_central.interfaces.IPantalla;
import servidor_central.logging.ILogger;
import servidor_central.protocolo.Mensaje;
import servidor_central.protocolo.TipoMensaje;

public class PublicadorPantalla implements IPantalla {

    private Socket socketPantalla;
    private PrintWriter out;
    private final ILogger log;

    public PublicadorPantalla(ILogger log) {
        this.log = log;
    }

    @Override
    public synchronized boolean suscribir(Socket socket) {
        cerrarActual();
        if (socket == null || socket.isClosed()) {
            return false;
        }
        try {
            this.socketPantalla = socket;
            this.out = new PrintWriter(socket.getOutputStream(), true);
            return true;
        } catch (IOException e) {
            log.logError("SUSCRIBIR_PANTALLA", e);
            limpiar();
            return false;
        }
    }

    @Override
    public synchronized void emitirLlamado(Turno t) {
        if (!estaConectadaInterno()) return;
        Mensaje m = new Mensaje(TipoMensaje.EVENTO_LLAMADO,
                t.getDni(),
                String.valueOf(t.getIdPuestoAsignado()));
        enviar(m, "EVENTO_LLAMADO",
                "dni=" + t.getDni() + " idPuesto=" + t.getIdPuestoAsignado());
    }

    @Override
    public synchronized void emitirRenotificacion(Turno t) {
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
    public synchronized void emitirAusente(int idPuesto) {
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
        return socketPantalla != null
                && !socketPantalla.isClosed()
                && out != null
                && !out.checkError();
    }

    private void enviar(Mensaje m, String tipoLog, String detalleLog) {
        out.println(m.serializar());
        if (out.checkError()) {
            log.logError("EMITIR_" + tipoLog,
                    new IOException("Fallo al escribir al socket de Pantalla"));
            limpiar();
            return;
        }
        log.logEvento(tipoLog, detalleLog);
    }

    private void cerrarActual() {
        if (socketPantalla != null) {
            try {
                socketPantalla.close();
            } catch (IOException e) {
                // ignorar
            }
        }
        limpiar();
    }

    private void limpiar() {
        socketPantalla = null;
        out = null;
    }
}
