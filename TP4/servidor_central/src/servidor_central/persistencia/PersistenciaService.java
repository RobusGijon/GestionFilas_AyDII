package servidor_central.persistencia;

import servidor_central.interfaces.IEstrategiaEncriptacion;
import servidor_central.interfaces.IPersistencia;
import servidor_central.interfaces.factory.IFabricaPersistencia;
import servidor_central.interfaces.factory.IPersistenciaEscritor;
import servidor_central.interfaces.factory.IPersistenciaLector;
import servidor_central.persistencia.dto.ServerDTO;

public class PersistenciaService implements IPersistencia {

    IPersistenciaEscritor escritor;
    IPersistenciaLector lector;

    public PersistenciaService (IFabricaPersistencia fabrica, IEstrategiaEncriptacion encriptacion) {
        this.escritor = fabrica.crearEscritor(encriptacion);
        this.lector = fabrica.crearLector(encriptacion);
    }

    @Override
    public void guardar(String ruta, ServerDTO estado) {
        this.escritor.guardar(ruta, estado);
    }

    @Override
    public ServerDTO leer(String ruta) {
        return this.lector.Leer(ruta);
    }

}
