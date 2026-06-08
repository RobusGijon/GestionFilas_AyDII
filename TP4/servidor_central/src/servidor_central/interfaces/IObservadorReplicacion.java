package servidor_central.interfaces;

import servidor_central.disponibilidad.SnapshotEstado;
import servidor_central.protocolo.mensajes.Mensaje;

public interface IObservadorReplicacion {

    void onParConectado();

    void onParDesconectado();

    void onSnapshotRecibido(SnapshotEstado snapshot);

    void onReplicarRecibido(Mensaje replicar);
}
