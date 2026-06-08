package servidor_central.interfaces;

import servidor_central.persistencia.dto.ServerDTO;

/**
 * Contrato de persistencia que usa el {@code CoordinadorServidor}: guardar y
 * leer el estado del servidor. Lo implementa {@code PersistenciaService}
 * (Facade), que oculta el escritor/lector concretos creados por la
 * {@code IFabricaPersistencia}.
 */
public interface IPersistencia {

    void guardar(String ruta, ServerDTO estado);

    ServerDTO leer(String ruta);
}
