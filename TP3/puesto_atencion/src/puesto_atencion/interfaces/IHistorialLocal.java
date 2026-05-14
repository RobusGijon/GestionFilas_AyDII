package puesto_atencion.interfaces;

import java.util.List;
import puesto_atencion.datos.ClienteAtendido;

public interface IHistorialLocal {
    void agregar(ClienteAtendido c);

    List<ClienteAtendido> snapshot();
}
