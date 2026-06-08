package servidor_central.interfaces.factory;

import servidor_central.persistencia.dto.ServerDTO;

public interface IPersistenciaLector {

    public ServerDTO Leer(String ruta);
}
