package servidor_central.conexion;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import servidor_central.interfaces.ICanalMensaje;
import servidor_central.interfaces.IPublicadorPuestos;
import servidor_central.logging.ILogger;
import servidor_central.protocolo.mensajes.Mensaje;
import servidor_central.protocolo.mensajes.TipoMensaje;

public class PublicadorPuestos implements IPublicadorPuestos {

    private final List<ICanalMensaje> observadores = new ArrayList<>();
    private final List<Integer> idsPorCanal = new ArrayList<>();
    private final ILogger log;
    private volatile IntConsumer onPuestoCaido = id -> { };

    public PublicadorPuestos(ILogger log) {
        this.log = log;
    }

    @Override
    public void setOnPuestoCaido(IntConsumer cb) {
        this.onPuestoCaido = (cb == null) ? id -> { } : cb;
    }

    @Override
    public synchronized boolean agregarObservador(int idPuesto, ICanalMensaje canal) {
        if (canal == null || !canal.estaVivo()) {
            return false;
        }
        observadores.add(canal);
        idsPorCanal.add(idPuesto);
        log.logInfo("Puesto " + idPuesto + " suscripto a eventos desde "
                + canal.getSocket().getInetAddress().getHostAddress()
                + ":" + canal.getSocket().getPort() + " (total=" + observadores.size() + ")");
        return true;
    }

    @Override
    public void notificarObservadores(int tamanio) {
        List<Integer> caidos = new ArrayList<>();
        synchronized (this) {
            if (observadores.isEmpty()) {
                return;
            }
            Mensaje m = new Mensaje(TipoMensaje.EVENTO_FILA_ACTUALIZADA, String.valueOf(tamanio));
            for (int i = observadores.size() - 1; i >= 0; i--) {
                ICanalMensaje canal = observadores.get(i);
                if (!canal.enviar(m)) {
                    caidos.add(idsPorCanal.get(i));
                    canal.cerrar();
                    observadores.remove(i);
                    idsPorCanal.remove(i);
                }
            }
            log.logEvento("EVENTO_FILA_ACTUALIZADA",
                    "tamanio=" + tamanio + " entregados=" + observadores.size() + " caidos=" + caidos.size());
        }
        
        for (int id : caidos) {
            final int fid = id;
            new Thread(() -> onPuestoCaido.accept(fid), "puesto-caido-" + fid).start();
        }
    }

    @Override
    public synchronized int cantidadObservadores() {
        return observadores.size();
    }
}
