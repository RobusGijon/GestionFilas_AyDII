package servidor_central.interfaces;

import java.util.function.IntConsumer;

public interface IPublicadorPuestos {

    boolean agregarObservador(int idPuesto, ICanalMensaje canal);

    void notificarObservadores(int tamanio);

    int cantidadObservadores();

    /** Callback que se invoca cuando se cae el canal de suscripcion de un puesto. */
    void setOnPuestoCaido(IntConsumer cb);
}
