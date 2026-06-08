package servidor_central.interfaces.factory;

import servidor_central.persistencia.dto.ServerDTO;

public interface IPersistenciaEscritor {

    public void guardar(String ruta, ServerDTO estado);
}
