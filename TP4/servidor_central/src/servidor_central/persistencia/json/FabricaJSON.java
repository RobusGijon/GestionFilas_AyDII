package servidor_central.persistencia.json;

import servidor_central.interfaces.IEstrategiaEncriptacion;
import servidor_central.interfaces.factory.IFabricaPersistencia;
import servidor_central.interfaces.factory.IPersistenciaEscritor;
import servidor_central.interfaces.factory.IPersistenciaLector;

public class FabricaJSON implements IFabricaPersistencia {

    @Override
    public IPersistenciaEscritor crearEscritor(IEstrategiaEncriptacion encriptacion) {
        return new EscritorJSON(encriptacion);
    }

    @Override
    public IPersistenciaLector crearLector(IEstrategiaEncriptacion encriptacion) {
        return new LectorJSON(encriptacion);
    }
}
