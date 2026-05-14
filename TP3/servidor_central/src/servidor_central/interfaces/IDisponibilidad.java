package servidor_central.interfaces;

import servidor_central.protocolo.Mensaje;

public interface IDisponibilidad {

    void publicarMutacion(Mensaje mensaje);
}
