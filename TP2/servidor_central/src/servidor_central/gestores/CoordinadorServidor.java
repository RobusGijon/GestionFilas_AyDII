package servidor_central.gestores;

import servidor_central.datos.PuestoInfo;
import servidor_central.datos.Turno;
import servidor_central.interfaces.ICoordinadorServidor;
import servidor_central.interfaces.IGestorClientesLlamados;
import servidor_central.interfaces.IGestorFilaEspera;
import servidor_central.interfaces.IGestorPuestos;
import servidor_central.interfaces.IPantalla;
import servidor_central.logging.ILogger;

public class CoordinadorServidor implements ICoordinadorServidor {

    private final IGestorFilaEspera gfe;
    private final IGestorClientesLlamados gcl;
    private final IGestorPuestos gp;
    private final IPantalla pub;
    private final ILogger log;

    public CoordinadorServidor(IGestorFilaEspera gfe,
                                IGestorClientesLlamados gcl,
                                IGestorPuestos gp,
                                IPantalla pub,
                                ILogger log) {
        this.gfe = gfe;
        this.gcl = gcl;
        this.gp = gp;
        this.pub = pub;
        this.log = log;
    }

    @Override
    public synchronized boolean registrarCliente(String dni) {
        try {
            if (dni == null || dni.isEmpty()) {
                throw new IllegalArgumentException("DNI vacio");
            }
            if (gfe.contieneDni(dni) || gcl.contieneDni(dni)) {
                log.logOperacion("REGISTRAR_CLIENTE", "dni=" + dni + " (duplicado)", false);
                log.logEstado(gfe, gcl, gp);
                return false;
            }
            Turno t = new Turno(dni, 1, null);
            gfe.agregarTurno(t);
            log.logOperacion("REGISTRAR_CLIENTE", "dni=" + dni, true);
            log.logEstado(gfe, gcl, gp);
            return true;
        } catch (RuntimeException e) {
            log.logError("REGISTRAR_CLIENTE", e);
            throw e;
        }
    }

    @Override
    public synchronized int conectarPuesto(String host) {
        try {
            PuestoInfo info = gp.registrarPuesto(host);
            log.logOperacion("CONECTAR_PUESTO", "host=" + host + " idPuesto=" + info.getIdPuesto(), true);
            log.logEstado(gfe, gcl, gp);
            return info.getIdPuesto();
        } catch (RuntimeException e) {
            log.logError("CONECTAR_PUESTO", e);
            throw e;
        }
    }

    @Override
    public synchronized Turno llamarSiguiente(int idPuesto) {
        try {
            if (!gp.existe(idPuesto)) {
                throw new IllegalStateException("El puesto " + idPuesto + " no esta registrado");
            }
            Turno previo = gcl.eliminarTurno(idPuesto);
            if (gfe.estaVacia()) {
                if (previo != null) {
                    // Dni vacio = senal a la pantalla para archivar al previo sin setear nuevo activo.
                    pub.emitirLlamado(new Turno("", 1, idPuesto));
                }
                log.logOperacion("LLAMAR_SIGUIENTE", "idPuesto=" + idPuesto + " (fila vacia)", true);
                log.logEstado(gfe, gcl, gp);
                return null;
            }
            Turno nuevo = gfe.extraerPrimero();
            nuevo.setIdPuestoAsignado(idPuesto);
            nuevo.setIntentosLlamados(1);
            gcl.asignarTurnoAPuesto(idPuesto, nuevo);
            pub.emitirLlamado(nuevo);
            log.logOperacion("LLAMAR_SIGUIENTE", "idPuesto=" + idPuesto + " dni=" + nuevo.getDni(), true);
            log.logEstado(gfe, gcl, gp);
            return nuevo;
        } catch (RuntimeException e) {
            log.logError("LLAMAR_SIGUIENTE", e);
            throw e;
        }
    }

    @Override
    public synchronized int reNotificar(int idPuesto) {
        try {
            Turno t = gcl.obtenerTurno(idPuesto);
            if (t == null) {
                throw new IllegalStateException("No hay turno en atencion para el puesto " + idPuesto);
            }
            if (t.getIntentosLlamados() >= 3) {
                throw new IllegalStateException("El turno ya alcanzo el maximo de intentos (3)");
            }
            int n = gcl.incrementarIntentos(idPuesto);
            pub.emitirRenotificacion(t);
            log.logOperacion("RENOTIFICAR", "idPuesto=" + idPuesto + " dni=" + t.getDni() + " intento=" + n + "/3", true);
            log.logEstado(gfe, gcl, gp);
            return n;
        } catch (RuntimeException e) {
            log.logError("RENOTIFICAR", e);
            throw e;
        }
    }

    @Override
    public synchronized Turno eliminarCliente(int idPuesto) {
        try {
            Turno removido = gcl.eliminarTurno(idPuesto);
            if (removido == null) {
                throw new IllegalStateException("No hay turno en atencion para el puesto " + idPuesto);
            }
            pub.emitirAusente(idPuesto);
            log.logOperacion("ELIMINAR_CLIENTE", "idPuesto=" + idPuesto + " dni=" + removido.getDni(), true);
            return llamarSiguiente(idPuesto);
        } catch (RuntimeException e) {
            log.logError("ELIMINAR_CLIENTE", e);
            throw e;
        }
    }
}
