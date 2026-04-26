package puesto_atencion.dominio;

import java.time.LocalTime;
import java.util.List;
import puesto_atencion.datos.ClienteAtendido;
import puesto_atencion.interfaces.IGestorConexionServidor;
import puesto_atencion.interfaces.IHistorialLocal;
import puesto_atencion.interfaces.IManejadorDeFila;

public class ManejadorDeFila implements IManejadorDeFila {

    private final IGestorConexionServidor conexion;
    private final IHistorialLocal historial;

    private int idPuesto;
    private String dniActual;
    private int intentosActual;

    public ManejadorDeFila(IGestorConexionServidor conexion, IHistorialLocal historial) {
        this.conexion = conexion;
        this.historial = historial;
        this.idPuesto = -1;
        this.dniActual = null;
        this.intentosActual = 0;
    }

    @Override
    public synchronized int conectar() throws Exception {
        this.idPuesto = conexion.conectarPuesto();
        return this.idPuesto;
    }

    @Override
    public synchronized int getIdPuesto() {
        return idPuesto;
    }

    @Override
    public synchronized String getDniActual() {
        return dniActual;
    }

    @Override
    public synchronized int getIntentosActual() {
        return intentosActual;
    }

    @Override
    public synchronized boolean hayClienteActual() {
        return dniActual != null;
    }

    @Override
    public synchronized String llamarSiguiente() throws Exception {
        if (dniActual != null) {
            historial.agregar(new ClienteAtendido(dniActual, LocalTime.now()));
        }
        String nuevo = conexion.llamarSiguiente(idPuesto);
        if (nuevo == null) {
            dniActual = null;
            intentosActual = 0;
        } else {
            dniActual = nuevo;
            intentosActual = 1;
        }
        return nuevo;
    }

    @Override
    public synchronized int reNotificar() throws Exception {
        intentosActual = conexion.reNotificar(idPuesto);
        return intentosActual;
    }

    @Override
    public synchronized String eliminarCliente() throws Exception {
        String nuevo = conexion.eliminarCliente(idPuesto);
        if (nuevo == null) {
            dniActual = null;
            intentosActual = 0;
        } else {
            dniActual = nuevo;
            intentosActual = 1;
        }
        return nuevo;
    }

    @Override
    public List<ClienteAtendido> historial() {
        return historial.snapshot();
    }
}
