package servidor_central.logging;

import servidor_central.interfaces.IGestorClientesLlamados;
import servidor_central.interfaces.IGestorFilaEspera;
import servidor_central.interfaces.IGestorPuestos;

public interface ILogger {

    void logOperacion(String operacion, String detalle, boolean ok);

    void logEstado(IGestorFilaEspera gfe, IGestorClientesLlamados gcl, IGestorPuestos gp);

    void logEvento(String tipo, String detalle);

    void logError(String operacion, Throwable e);

    void logInfo(String mensaje);
}
