package puesto_atencion.interfaces;

import java.util.List;
import puesto_atencion.datos.ClienteAtendido;

public interface IManejadorDeFila {
    int     conectar()           throws Exception;
    int     getIdPuesto();
    String  getDniActual();
    int     getIntentosActual();
    boolean hayClienteActual();

    String  llamarSiguiente()    throws Exception;
    int     reNotificar()        throws Exception;
    String  eliminarCliente()    throws Exception;

    List<ClienteAtendido> historial();
}
