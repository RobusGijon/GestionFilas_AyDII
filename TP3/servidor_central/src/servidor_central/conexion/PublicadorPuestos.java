package servidor_central.conexion;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import servidor_central.interfaces.IPublicadorPuestos;
import servidor_central.logging.ILogger;
import servidor_central.protocolo.Mensaje;
import servidor_central.protocolo.TipoMensaje;

public class PublicadorPuestos implements IPublicadorPuestos {

    private static class Suscriptor {
        final Socket socket;
        final PrintWriter out;

        Suscriptor(Socket socket, PrintWriter out) {
            this.socket = socket;
            this.out = out;
        }
    }

    private final List<Suscriptor> suscriptos = new ArrayList<>();
    private final ILogger log;

    public PublicadorPuestos(ILogger log) {
        this.log = log;
    }

    @Override
    public synchronized boolean suscribir(Socket socket) {
        if (socket == null || socket.isClosed()) {
            return false;
        }
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            suscriptos.add(new Suscriptor(socket, out));
            log.logInfo("Puesto suscripto a eventos desde " + socket.getInetAddress().getHostAddress()
                    + ":" + socket.getPort() + " (total=" + suscriptos.size() + ")");
            return true;
        } catch (IOException e) {
            log.logError("SUSCRIBIR_PUESTO", e);
            return false;
        }
    }

    @Override
    public synchronized void emitirActualizacionFila(int tamanio) {
        if (suscriptos.isEmpty()) {
            return;
        }
        Mensaje m = new Mensaje(TipoMensaje.EVENTO_FILA_ACTUALIZADA, String.valueOf(tamanio));
        String linea = m.serializar();
        Iterator<Suscriptor> it = suscriptos.iterator();
        int caidos = 0;
        while (it.hasNext()) {
            Suscriptor s = it.next();
            s.out.println(linea);
            if (s.out.checkError() || s.socket.isClosed()) {
                it.remove();
                cerrar(s.socket);
                caidos++;
            }
        }
        log.logEvento("EVENTO_FILA_ACTUALIZADA",
                "tamanio=" + tamanio + " entregados=" + suscriptos.size() + " caidos=" + caidos);
    }

    @Override
    public synchronized int cantidadSuscriptos() {
        return suscriptos.size();
    }

    private void cerrar(Socket s) {
        try {
            s.close();
        } catch (IOException e) {
            // ignorar
        }
    }
}
