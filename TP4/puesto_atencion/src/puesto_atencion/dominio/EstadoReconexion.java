package puesto_atencion.dominio;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado que el servidor le reenvia al puesto al (re)conectar: el id asignado,
 * el cliente que tenia en atencion (si lo habia) con su numero de intento, y el
 * historial de atendidos recuperado. Lo arma {@code GestorConexionServidor} al
 * leer el bloque enmarcado (snapshot) de la respuesta de CONECTAR_PUESTO.
 */
public class EstadoReconexion {

    private final int idPuesto;
    private final String dniActual;   // null si no atendia a nadie
    private final int intentosActual; // 0 si no hay cliente
    private final List<ClienteAtendido> historial;

    public EstadoReconexion(int idPuesto, String dniActual, int intentosActual,
                            List<ClienteAtendido> historial) {
        this.idPuesto = idPuesto;
        this.dniActual = dniActual;
        this.intentosActual = intentosActual;
        this.historial = historial == null ? new ArrayList<>() : historial;
    }

    public int getIdPuesto() {
        return idPuesto;
    }

    public String getDniActual() {
        return dniActual;
    }

    public int getIntentosActual() {
        return intentosActual;
    }

    public List<ClienteAtendido> getHistorial() {
        return historial;
    }
}
