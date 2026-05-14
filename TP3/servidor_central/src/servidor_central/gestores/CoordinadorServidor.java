package servidor_central.gestores;

import java.net.Socket;
import java.util.Map;

import servidor_central.datos.PuestoInfo;
import servidor_central.datos.Turno;
import servidor_central.disponibilidad.SnapshotEstado;
import servidor_central.interfaces.ICoordinadorServidor;
import servidor_central.interfaces.IDisponibilidad;
import servidor_central.interfaces.IGestorClientesLlamados;
import servidor_central.interfaces.IGestorFilaEspera;
import servidor_central.interfaces.IGestorPuestos;
import servidor_central.interfaces.IPantalla;
import servidor_central.interfaces.IPublicadorPuestos;
import servidor_central.logging.ILogger;
import servidor_central.protocolo.Mensaje;
import servidor_central.protocolo.TipoMensaje;

public class CoordinadorServidor implements ICoordinadorServidor {

    private final IGestorFilaEspera gestorFilaEspera;
    private final IGestorClientesLlamados gestorClientesLlamados;
    private final IGestorPuestos gestorPuestos;
    private final IPantalla pantalla;
    private final IPublicadorPuestos publicadorPuestos;
    private final ILogger log;

    private volatile IDisponibilidad disponibilidad;

    public CoordinadorServidor(IGestorFilaEspera gfe,
            IGestorClientesLlamados gcl,
            IGestorPuestos gp,
            IPantalla pub,
            IPublicadorPuestos pubPuestos,
            ILogger log) {
        this.gestorFilaEspera = gfe;
        this.gestorClientesLlamados = gcl;
        this.gestorPuestos = gp;
        this.pantalla = pub;
        this.publicadorPuestos = pubPuestos;
        this.log = log;
    }

    @Override
    public synchronized boolean suscribirPuestoAEventos(Socket socket) {
        boolean ok = publicadorPuestos.suscribir(socket);
        if (ok) {
            publicadorPuestos.emitirActualizacionFila(gestorFilaEspera.tamano());
        }
        return ok;
    }

    @Override
    public synchronized void replicarEstadoAPantalla() {
        for (Turno t : gestorClientesLlamados.snapshot()) {
            pantalla.emitirLlamado(t);
            if (t.getIntentosLlamados() > 1) {
                pantalla.emitirRenotificacion(t);
            }
        }
    }

    @Override
    public synchronized boolean registrarCliente(String dni) {
        try {
            if (dni == null || dni.isEmpty()) {
                throw new IllegalArgumentException("DNI vacio");
            }
            if (gestorFilaEspera.contieneDni(dni) || gestorClientesLlamados.contieneDni(dni)) {
                log.logOperacion("REGISTRAR_CLIENTE", "dni=" + dni + " (duplicado)", false);
                log.logEstado(gestorFilaEspera, gestorClientesLlamados, gestorPuestos);
                return false;
            }
            Turno t = new Turno(dni, 1, null);
            gestorFilaEspera.agregarTurno(t);
            emitirReplicacion(new Mensaje(TipoMensaje.REPLICAR, "REGISTRADO", dni));
            log.logOperacion("REGISTRAR_CLIENTE", "dni=" + dni, true);
            log.logEstado(gestorFilaEspera, gestorClientesLlamados, gestorPuestos);
            publicadorPuestos.emitirActualizacionFila(gestorFilaEspera.tamano());
            return true;
        } catch (RuntimeException e) {
            log.logError("REGISTRAR_CLIENTE", e);
            throw e;
        }
    }

    @Override
    public synchronized int conectarPuesto(String host) {
        try {
            PuestoInfo info = gestorPuestos.registrarPuesto(host);
            emitirReplicacion(new Mensaje(TipoMensaje.REPLICAR, "PUESTO_REGISTRADO",
                    String.valueOf(info.getIdPuesto()), host == null ? "" : host));
            log.logOperacion("CONECTAR_PUESTO", "host=" + host + " idPuesto=" + info.getIdPuesto(), true);
            log.logEstado(gestorFilaEspera, gestorClientesLlamados, gestorPuestos);
            return info.getIdPuesto();
        } catch (RuntimeException e) {
            log.logError("CONECTAR_PUESTO", e);
            throw e;
        }
    }

    @Override
    public synchronized Turno llamarSiguiente(int idPuesto) {
        try {
            if (!gestorPuestos.existe(idPuesto)) {
                throw new IllegalStateException("El puesto " + idPuesto + " no esta registrado");
            }
            Turno previo = gestorClientesLlamados.eliminarTurno(idPuesto);
            if (gestorFilaEspera.estaVacia()) {
                if (previo != null) {
                    pantalla.emitirLlamado(new Turno("", 1, idPuesto));
                }
                emitirReplicacion(new Mensaje(TipoMensaje.REPLICAR, "LLAMADO_VACIA",
                        String.valueOf(idPuesto)));
                log.logOperacion("LLAMAR_SIGUIENTE", "idPuesto=" + idPuesto + " (fila vacia)", true);
                log.logEstado(gestorFilaEspera, gestorClientesLlamados, gestorPuestos);
                return null;
            }
            Turno nuevo = gestorFilaEspera.extraerPrimero();
            nuevo.setIdPuestoAsignado(idPuesto);
            nuevo.setIntentosLlamados(1);
            gestorClientesLlamados.asignarTurnoAPuesto(idPuesto, nuevo);
            pantalla.emitirLlamado(nuevo);
            emitirReplicacion(new Mensaje(TipoMensaje.REPLICAR, "LLAMADO",
                    String.valueOf(idPuesto), nuevo.getDni(),
                    String.valueOf(nuevo.getIntentosLlamados())));
            log.logOperacion("LLAMAR_SIGUIENTE", "idPuesto=" + idPuesto + " dni=" + nuevo.getDni(), true);
            log.logEstado(gestorFilaEspera, gestorClientesLlamados, gestorPuestos);
            publicadorPuestos.emitirActualizacionFila(gestorFilaEspera.tamano());
            return nuevo;
        } catch (RuntimeException e) {
            log.logError("LLAMAR_SIGUIENTE", e);
            throw e;
        }
    }

    @Override
    public synchronized int reNotificar(int idPuesto) {
        try {
            Turno t = gestorClientesLlamados.obtenerTurno(idPuesto);
            if (t == null) {
                throw new IllegalStateException("No hay turno en atencion para el puesto " + idPuesto);
            }
            if (t.getIntentosLlamados() >= 3) {
                throw new IllegalStateException("El turno ya alcanzo el maximo de intentos (3)");
            }
            int n = gestorClientesLlamados.incrementarIntentos(idPuesto);
            pantalla.emitirRenotificacion(t);
            emitirReplicacion(new Mensaje(TipoMensaje.REPLICAR, "RENOTIFICADO",
                    String.valueOf(idPuesto), String.valueOf(n)));
            log.logOperacion("RENOTIFICAR", "idPuesto=" + idPuesto + " dni=" + t.getDni() + " intento=" + n + "/3",
                    true);
            log.logEstado(gestorFilaEspera, gestorClientesLlamados, gestorPuestos);
            return n;
        } catch (RuntimeException e) {
            log.logError("RENOTIFICAR", e);
            throw e;
        }
    }

    @Override
    public synchronized Turno eliminarCliente(int idPuesto) {
        try {
            Turno removido = gestorClientesLlamados.eliminarTurno(idPuesto);
            if (removido == null) {
                throw new IllegalStateException("No hay turno en atencion para el puesto " + idPuesto);
            }
            pantalla.emitirAusente(idPuesto);
            emitirReplicacion(new Mensaje(TipoMensaje.REPLICAR, "ELIMINADO",
                    String.valueOf(idPuesto), removido.getDni()));
            log.logOperacion("ELIMINAR_CLIENTE", "idPuesto=" + idPuesto + " dni=" + removido.getDni(), true);
            return llamarSiguiente(idPuesto);
        } catch (RuntimeException e) {
            log.logError("ELIMINAR_CLIENTE", e);
            throw e;
        }
    }

    // -------- Replicacion par-a-par --------

    @Override
    public synchronized SnapshotEstado tomarSnapshot() {
        java.util.HashMap<Integer, Turno> llamados = new java.util.HashMap<>();
        for (Turno t : gestorClientesLlamados.snapshot()) {
            if (t.getIdPuestoAsignado() != null) {
                llamados.put(t.getIdPuestoAsignado(), copia(t));
            }
        }
        java.util.ArrayList<Turno> fila = new java.util.ArrayList<>();
        for (Turno t : gestorFilaEspera.snapshot())
            fila.add(copia(t));
        return new SnapshotEstado(fila, llamados, gestorPuestos.snapshot(), gestorPuestos.getProximoId());
    }

    @Override
    public synchronized void enlazarDisponibilidad(IDisponibilidad d) {
        SnapshotEstado snap = tomarSnapshot();
        for (Mensaje m : snap.serializar()) {
            try {
                d.publicarMutacion(m);
            } catch (RuntimeException e) {
                log.logInfo("[COORD] error enviando snapshot al par: " + e.getMessage());
                return;
            }
        }
        this.disponibilidad = d;
        log.logInfo("[COORD] disponibilidad enlazada, snapshot enviado al par");
    }

    @Override
    public void desenlazarDisponibilidad() {
        if (this.disponibilidad != null) {
            this.disponibilidad = null;
            log.logInfo("[COORD] disponibilidad desenlazada");
        }
    }

    @Override
    public synchronized void aplicarSnapshot(SnapshotEstado snap) {
        gestorFilaEspera.limpiar();
        gestorClientesLlamados.limpiar();
        gestorPuestos.limpiar();
        for (PuestoInfo p : snap.getPuestos()) {
            gestorPuestos.registrarPuestoConId(p.getIdPuesto(), p.getHostRemoto());
        }
        gestorPuestos.setProximoId(snap.getProximoIdPuesto());
        for (Turno t : snap.getFila()) {
            gestorFilaEspera.agregarTurno(copia(t));
        }
        for (Map.Entry<Integer, Turno> e : snap.getLlamados().entrySet()) {
            Turno t = copia(e.getValue());
            t.setIdPuestoAsignado(e.getKey());
            gestorClientesLlamados.asignarTurnoAPuesto(e.getKey(), t);
        }
        log.logInfo("[COORD] snapshot aplicado");
        log.logEstado(gestorFilaEspera, gestorClientesLlamados, gestorPuestos);
    }

    @Override
    public synchronized void aplicarReplicacion(Mensaje m) {
        if (m.getTipo() != TipoMensaje.REPLICAR || m.getArgs().isEmpty())
            return;
        String sub = m.getArg(0);
        try {
            switch (sub) {
                case "REGISTRADO": {
                    String dni = m.getArg(1);
                    gestorFilaEspera.agregarTurno(new Turno(dni, 1, null));
                    break;
                }
                case "PUESTO_REGISTRADO": {
                    int idPuesto = Integer.parseInt(m.getArg(1));
                    String host = m.getArg(2);
                    gestorPuestos.registrarPuestoConId(idPuesto, host);
                    break;
                }
                case "LLAMADO": {
                    int idPuesto = Integer.parseInt(m.getArg(1));
                    String dni = m.getArg(2);
                    int intentos = Integer.parseInt(m.getArg(3));
                    if (!gestorFilaEspera.estaVacia()) {
                        Turno frente = gestorFilaEspera.extraerPrimero();
                        if (!frente.getDni().equals(dni)) {
                            log.logInfo("[COORD] WARN replicacion LLAMADO: frente de fila ("
                                    + frente.getDni() + ") no coincide con dni replicado ("
                                    + dni + ")");
                        }
                    }
                    Turno nuevo = new Turno(dni, intentos, idPuesto);
                    gestorClientesLlamados.asignarTurnoAPuesto(idPuesto, nuevo);
                    break;
                }
                case "LLAMADO_VACIA": {
                    int idPuesto = Integer.parseInt(m.getArg(1));
                    gestorClientesLlamados.eliminarTurno(idPuesto);
                    break;
                }
                case "RENOTIFICADO": {
                    int idPuesto = Integer.parseInt(m.getArg(1));
                    int intentos = Integer.parseInt(m.getArg(2));
                    Turno t = gestorClientesLlamados.obtenerTurno(idPuesto);
                    if (t != null)
                        t.setIntentosLlamados(intentos);
                    break;
                }
                case "ELIMINADO": {
                    int idPuesto = Integer.parseInt(m.getArg(1));
                    gestorClientesLlamados.eliminarTurno(idPuesto);
                    break;
                }
                default:
                    log.logInfo("[COORD] sub-tipo REPLICAR desconocido: " + sub);
                    return;
            }
            log.logInfo("[COORD] aplicado " + m.serializar());
            log.logEstado(gestorFilaEspera, gestorClientesLlamados, gestorPuestos);
        } catch (RuntimeException e) {
            log.logError("APLICAR_REPLICACION", e);
        }
    }

    private void emitirReplicacion(Mensaje m) {
        IDisponibilidad d = this.disponibilidad;
        if (d == null)
            return;
        try {
            d.publicarMutacion(m);
        } catch (RuntimeException e) {
            log.logInfo("[COORD] error replicando " + m.serializar() + ": " + e.getMessage());
        }
    }

    private static Turno copia(Turno t) {
        return new Turno(t.getDni(), t.getIntentosLlamados(), t.getIdPuestoAsignado());
    }
}
