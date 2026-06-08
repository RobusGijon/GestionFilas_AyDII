package servidor_central.persistencia.txt;

import servidor_central.interfaces.IEstrategiaEncriptacion;
import servidor_central.interfaces.factory.IFabricaPersistencia;
import servidor_central.interfaces.factory.IPersistenciaEscritor;
import servidor_central.interfaces.factory.IPersistenciaLector;

public class FabricaTXT implements IFabricaPersistencia{

    @Override
    public IPersistenciaEscritor crearEscritor(IEstrategiaEncriptacion encriptacion) {
        return new EscritorTXT(encriptacion);
    }

    @Override
    public IPersistenciaLector crearLector(IEstrategiaEncriptacion encriptacion) {
        return new LectorTXT(encriptacion);
    }

}
