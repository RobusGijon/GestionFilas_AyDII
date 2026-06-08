package servidor_central.interfaces.factory;

import servidor_central.interfaces.IEstrategiaEncriptacion;

public interface IFabricaPersistencia {
    public IPersistenciaEscritor crearEscritor(IEstrategiaEncriptacion encriptacion);
    public IPersistenciaLector crearLector(IEstrategiaEncriptacion encriptacion);
}
