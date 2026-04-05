package puesto_atencion.interfaces;

import java.util.List;

public interface IManejoFila {
    void agregarCliente(String dni);
    String retiraPrimerDNI();
    List<String> obtenerFila();
    boolean filaVacia();
}
