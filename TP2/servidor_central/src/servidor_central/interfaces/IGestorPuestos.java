package servidor_central.interfaces;

import java.util.List;

import servidor_central.datos.PuestoInfo;

public interface IGestorPuestos {

    PuestoInfo registrarPuesto(String hostRemoto);

    void eliminarPuesto(int idPuesto);

    boolean existe(int idPuesto);

    List<PuestoInfo> snapshot();

    int cantidad();
}
