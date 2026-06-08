package servidor_central.persistencia.xml;

import servidor_central.interfaces.IEstrategiaEncriptacion;
import servidor_central.interfaces.factory.IFabricaPersistencia;
import servidor_central.interfaces.factory.IPersistenciaEscritor;
import servidor_central.interfaces.factory.IPersistenciaLector;

public class FabricaXML implements IFabricaPersistencia{

    @Override
    public IPersistenciaEscritor crearEscritor(IEstrategiaEncriptacion encriptacion) {
        return new EscritorXML(encriptacion);
    }

    @Override
    public IPersistenciaLector crearLector(IEstrategiaEncriptacion encriptacion) {
        return new LectorXML(encriptacion);
    }

}
