package servidor_central.interfaces;

import servidor_central.protocolo.Mensaje;

public interface IGestorReplicacion {

    void replicar(Mensaje m);

    void cambiarRolAPrimario();

    void cambiarRolASecundario();
}
