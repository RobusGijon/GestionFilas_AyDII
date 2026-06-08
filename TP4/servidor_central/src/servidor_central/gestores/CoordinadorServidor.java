package servidor_central.gestores;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import servidor_central.datos.ClienteAtendido;
import servidor_central.datos.EstadoReconexionPuesto;
import servidor_central.datos.PuestoInfo;
import servidor_central.datos.Turno;
import servidor_central.disponibilidad.SnapshotEstado;
import servidor_central.interfaces.ICanalMensaje;
import servidor_central.interfaces.ICoordinadorServidor;
import servidor_central.interfaces.IDisponibilidad;
import servidor_central.interfaces.IGestorClientesLlamados;
import servidor_central.interfaces.IGestorFilaEspera;
import servidor_central.interfaces.IGestorPuestos;
import servidor_central.interfaces.IPantalla;
import servidor_central.interfaces.IPublicadorPuestos;
import servidor_central.logging.ILogger;
import servidor_central.interfaces.IPersistencia;
import servidor_central.persistencia.dto.ServerDTO;
import servidor_central.protocolo.mensajes.Mensaje;
import servidor_central.protocolo.mensajes.TipoMensaje;

public class CoordinadorServidor implements ICoordinadorServidor {

    private final IGestorFilaEspera gestorFilaEspera;
    private final IGestorClientesLlamados gestorClientesLlamados;
    private final IGestorPuestos gestorPuestos;
    private final IPantalla pantalla;
    private final IPublicadorPuestos publicadorPuestos;
    private final ILogger log;
    private final IPersistencia persistencia;
    private final String archivoEstado;

    private volatile IDisponibilidad disponibilidad;

    /**
     * Persistir a disco es responsabilidad del rol PRIMARIO (o STANDALONE). El
     * SECUNDARIO solo espeja en RAM vía replicación y, al reconectar, recibe un
     * snapshot fresco del primario; por eso no persiste. Se activa al promocionar
     * y se desactiva al demover.
     */
    private volatile boolean persistenciaActiva = true;

    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public CoordinadorServidor(IGestorFilaEspera gfe,
            IGestorClientesLlamados gcl,
            IGestorPuestos gp,
            IPantalla pub,
            IPublicadorPuestos pubPuestos,
            ILogger log,
            IPersistencia persistencia,
            String archivoEstado) {
        this.gestorFilaEspera = gfe;
        this.gestorClientesLlamados = gcl;
        this.gestorPuestos = gp;
        this.pantalla = pub;
        this.publicadorPuestos = pubPuestos;
        this.log = log;
        this.persistencia = persistencia;
        this.archivoEstado = archivoEstado;
    }

    @Override
    public synchronized boolean suscribirPuestoAEventos(int idPuesto, ICanalMensaje canal) {
        boolean ok = publicadorPuestos.agregarObservador(idPuesto, canal);
        if (ok) {
            // La suscripcion es una conexion viva mas del puesto: cuenta para
            // mantenerlo ASIGNADO mientras este suscripto.
            if (idPuesto > 0) {
                gestorPuestos.marcarConexion(idPuesto);
            }
            publicadorPuestos.notificarObservadores(gestorFilaEspera.tamano());
        }
        return ok;
    }

    @Override
    public synchronized void replicarEstadoAPantalla() {
        // El Monitor separa "Atendiendo ahora" del "Historial de llamados": el
        // cliente en atencion se reenvia como llamado activo (notificarLlamado),
        // no como historial. Como el historial del servidor incluye al cliente
        // en atencion (se agrega al llamarlo), lo excluimos del replay para que
        // tras un reinicio/reconexion no aparezca duplicado en ambas tablas.
        Set<String> activos = new HashSet<>();
        for (Turno t : gestorClientesLlamados.snapshot()) {
            if (t.getIdPuestoAsignado() != null) {
                activos.add(t.getIdPuestoAsignado() + "|" + t.getDni());
            }
        }
        List<Turno> historialPrevios = new java.util.ArrayList<>();
        for (Turno t : gestorClientesLlamados.historial()) {
            if (!activos.contains(t.getIdPuestoAsignado() + "|" + t.getDni())) {
                historialPrevios.add(t);
            }
        }
        pantalla.notificarHistorial(historialPrevios);
        for (Turno t : gestorClientesLlamados.snapshot()) {
            pantalla.notificarLlamado(t);
            if (t.getIntentosLlamados() > 1) {
                pantalla.notificarRenotificacion(t);
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

            persistir();
            publicadorPuestos.notificarObservadores(gestorFilaEspera.tamano());
            return true;
        } catch (RuntimeException e) {
            log.logError("REGISTRAR_CLIENTE", e);
            throw e;
        }
    }

    @Override
    public synchronized EstadoReconexionPuesto conectarPuesto(String host, int idDeseado) {
        try {
            PuestoInfo info = null;
            String modo;
            // 1) Reclamo por id exacto (puesto vivo que reconecta con su id en RAM).
            if (idDeseado > 0) {
                info = gestorPuestos.reclamarPorId(idDeseado);
            }
            // 2) Reparto de un slot DESASIGNADO (con turno en atencion primero).
            if (info == null) {
                info = gestorPuestos.reclamarDesasignado(idsConTurno());
            }
            boolean nuevo = false;
            if (info == null) {
                // 3) No hay slots libres: se crea uno nuevo.
                info = gestorPuestos.registrarPuesto(host);
                nuevo = true;
                modo = "NUEVO";
            } else {
                info.setHostRemoto(host);
                modo = (idDeseado > 0 && info.getIdPuesto() == idDeseado) ? "RECLAMO_POR_ID" : "REPARTO";
            }
            int idPuesto = info.getIdPuesto();
            gestorPuestos.marcarConexion(idPuesto);

            if (nuevo) {
                emitirReplicacion(new Mensaje(TipoMensaje.REPLICAR, "PUESTO_REGISTRADO",
                        String.valueOf(idPuesto), host == null ? "" : host));
            }

            Turno turno = gestorClientesLlamados.obtenerTurno(idPuesto);
            List<ClienteAtendido> historial = gestorPuestos.historialDe(idPuesto);

            log.logOperacion("CONECTAR_PUESTO",
                    "host=" + host + " idDeseado=" + idDeseado + " -> idPuesto=" + idPuesto
                            + " (" + modo + (turno != null ? ", recupera dni=" + turno.getDni() : "") + ")",
                    true);
            log.logEstado(gestorFilaEspera, gestorClientesLlamados, gestorPuestos);
            persistir();
            return new EstadoReconexionPuesto(idPuesto, turno, historial);
        } catch (RuntimeException e) {
            log.logError("CONECTAR_PUESTO", e);
            throw e;
        }
    }

    @Override
    public synchronized EstadoReconexionPuesto estadoPuesto(int idPuesto) {
        Turno turno = gestorClientesLlamados.obtenerTurno(idPuesto);
        List<ClienteAtendido> historial = gestorPuestos.historialDe(idPuesto);
        return new EstadoReconexionPuesto(idPuesto, turno, historial);
    }

    @Override
    public synchronized void notificarDesconexionPuesto(int idPuesto) {
        if (idPuesto <= 0) {
            return;
        }
        boolean estabaAsignado = gestorPuestos.estaAsignado(idPuesto);
        gestorPuestos.marcarDesconexion(idPuesto);
        // Solo cuando se cae la ULTIMA conexion el puesto pasa a DESASIGNADO. El
        // turno NO se borra (decision E1: se retiene para que lo reclame quien
        // vuelva); pero avisamos a la pantalla para que no lo siga mostrando como
        // activo mientras no haya operador.
        if (estabaAsignado && !gestorPuestos.estaAsignado(idPuesto)) {
            pantalla.notificarAusente(idPuesto);
            log.logInfo("[COORD] puesto " + idPuesto + " DESASIGNADO (sin conexiones vivas)");
            log.logEstado(gestorFilaEspera, gestorClientesLlamados, gestorPuestos);
        }
    }

    /** Ids de puestos que actualmente tienen un turno en atencion. */
    private Set<Integer> idsConTurno() {
        Set<Integer> s = new HashSet<>();
        for (Turno t : gestorClientesLlamados.snapshot()) {
            if (t.getIdPuestoAsignado() != null) {
                s.add(t.getIdPuestoAsignado());
            }
        }
        return s;
    }

    private static String horaActual() {
        return LocalTime.now().format(HORA_FMT);
    }

    @Override
    public synchronized Turno llamarSiguiente(int idPuesto) {
        try {
            if (!gestorPuestos.existe(idPuesto)) {
                throw new IllegalStateException("El puesto " + idPuesto + " no esta registrado");
            }
            Turno previo = gestorClientesLlamados.eliminarTurno(idPuesto);
            // (F) Al reemplazar al cliente anterior, ese previo se da por atendido:
            // se agrega al historial del puesto con marca de hora. El cliente
            // marcado ausente via eliminarCliente NO pasa por aca.
            if (previo != null) {
                ClienteAtendido at = new ClienteAtendido(previo.getDni(), horaActual());
                gestorPuestos.agregarAtendido(idPuesto, at);
                emitirReplicacion(new Mensaje(TipoMensaje.REPLICAR, "ATENDIDO",
                        String.valueOf(idPuesto), previo.getDni(), at.getHora()));
            }
            if (gestorFilaEspera.estaVacia()) {
                if (previo != null) {
                    pantalla.notificarLlamado(new Turno("", 1, idPuesto));
                }
                emitirReplicacion(new Mensaje(TipoMensaje.REPLICAR, "LLAMADO_VACIA",
                        String.valueOf(idPuesto)));
                log.logOperacion("LLAMAR_SIGUIENTE", "idPuesto=" + idPuesto + " (fila vacia)", true);
                log.logEstado(gestorFilaEspera, gestorClientesLlamados, gestorPuestos);
                persistir();
                return null;
            }
            Turno nuevo = gestorFilaEspera.extraerPrimero();
            nuevo.setIdPuestoAsignado(idPuesto);
            nuevo.setIntentosLlamados(1);
            gestorClientesLlamados.asignarTurnoAPuesto(idPuesto, nuevo);
            gestorClientesLlamados.agregarAHistorial(nuevo);
            pantalla.notificarLlamado(nuevo);
            emitirReplicacion(new Mensaje(TipoMensaje.REPLICAR, "LLAMADO",
                    String.valueOf(idPuesto), nuevo.getDni(),
                    String.valueOf(nuevo.getIntentosLlamados())));
            log.logOperacion("LLAMAR_SIGUIENTE", "idPuesto=" + idPuesto + " dni=" + nuevo.getDni(), true);
            log.logEstado(gestorFilaEspera, gestorClientesLlamados, gestorPuestos);
            persistir();
            publicadorPuestos.notificarObservadores(gestorFilaEspera.tamano());
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
            pantalla.notificarRenotificacion(t);
            emitirReplicacion(new Mensaje(TipoMensaje.REPLICAR, "RENOTIFICADO",
                    String.valueOf(idPuesto), String.valueOf(n)));
            log.logOperacion("RENOTIFICAR", "idPuesto=" + idPuesto + " dni=" + t.getDni() + " intento=" + n + "/3",
                    true);
            log.logEstado(gestorFilaEspera, gestorClientesLlamados, gestorPuestos);
            persistir();
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
            pantalla.notificarAusente(idPuesto);
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
            // (F-HA) el historial por puesto viaja en el snapshot del primario.
            for (ClienteAtendido c : p.getHistorialAtendidos()) {
                gestorPuestos.agregarAtendido(p.getIdPuesto(), c);
            }
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
        persistir();
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
                case "ATENDIDO": {
                    int idPuesto = Integer.parseInt(m.getArg(1));
                    String dni = m.getArg(2);
                    String hora = m.getArg(3);
                    gestorPuestos.agregarAtendido(idPuesto, new ClienteAtendido(dni, hora));
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
            persistir();
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

    // -------- Persistencia en disco --------

    /** Vuelca el estado actual de los gestores a un {@link ServerDTO}. */
    private ServerDTO construirDTO() {
        ServerDTO dto = new ServerDTO();
        for (PuestoInfo p : gestorPuestos.snapshot()) {
            dto.agregarPuesto(p);
        }
        for (Turno t : gestorFilaEspera.snapshot()) {
            dto.agregarTurnoEnEspera(t);
        }
        for (Turno t : gestorClientesLlamados.snapshot()) {
            if (t.getIdPuestoAsignado() != null) {
                dto.asignarTurnoAPuesto(t.getIdPuestoAsignado(), t);
            }
        }
        for (Turno t : gestorClientesLlamados.historial()) {
            dto.agregarAlHistorial(t);
        }
        return dto;
    }

    /**
     * Guarda el estado actual en disco. Un fallo de E/S no debe interrumpir la
     * atencion: se registra y se sigue.
     */
    private void persistir() {
        if (persistencia == null || !persistenciaActiva) {
            return;
        }
        try {
            persistencia.guardar(archivoEstado, construirDTO());
        } catch (RuntimeException e) {
            log.logInfo("[PERSIST] error guardando estado en " + archivoEstado + ": " + e.getMessage());
        }
    }

    @Override
    public void setPersistenciaActiva(boolean activa) {
        if (this.persistenciaActiva != activa) {
            log.logInfo("[PERSIST] persistencia " + (activa
                    ? "ACTIVADA (rol primario/standalone)"
                    : "DESACTIVADA (rol secundario: la persistencia la maneja el primario)"));
        }
        this.persistenciaActiva = activa;
    }

    @Override
    public synchronized void persistirAhora() {
        persistir();
    }

    /**
     * Restaura el estado persistido (si lo hay) sobre los gestores. Se llama al
     * arrancar; el {@code proximoId} de puestos se reconstruye solo via
     * {@code registrarPuestoConId}.
     */
    public synchronized void restaurarEstado() {
        if (persistencia == null) {
            return;
        }
        ServerDTO dto;
        try {
            dto = persistencia.leer(archivoEstado);
        } catch (servidor_central.persistencia.ClaveInvalidaException e) {
            // Clave/cifrado incorrecto sobre un archivo existente: NO arrancar con
            // estado vacio (lo sobrescribiria). Propagar para abortar el arranque.
            throw e;
        } catch (RuntimeException e) {
            log.logInfo("[PERSIST] error leyendo estado de " + archivoEstado + ": " + e.getMessage());
            return;
        }
        if (dto == null) {
            return;
        }
        gestorFilaEspera.limpiar();
        gestorClientesLlamados.limpiar();
        gestorPuestos.limpiar();
        for (PuestoInfo p : dto.getPuestosActivos()) {
            gestorPuestos.registrarPuestoConId(p.getIdPuesto(), p.getHostRemoto());
            // (F) recuperar el historial de atendidos del puesto desde disco.
            for (ClienteAtendido c : p.getHistorialAtendidos()) {
                gestorPuestos.agregarAtendido(p.getIdPuesto(), c);
            }
        }
        for (Turno t : dto.getColaEspera()) {
            gestorFilaEspera.agregarTurno(t);
        }
        for (Map.Entry<Integer, Turno> e : dto.getTurnosEnPuesto().entrySet()) {
            Turno t = e.getValue();
            t.setIdPuestoAsignado(e.getKey());
            gestorClientesLlamados.asignarTurnoAPuesto(e.getKey(), t);
        }
        for (Turno t : dto.getHistorialLlamados()) {
            gestorClientesLlamados.agregarAHistorial(t);
        }
        if (!dto.getPuestosActivos().isEmpty() || !dto.getColaEspera().isEmpty()
                || !dto.getTurnosEnPuesto().isEmpty() || !dto.getHistorialLlamados().isEmpty()) {
            log.logInfo("[PERSIST] estado restaurado desde " + archivoEstado);
            log.logEstado(gestorFilaEspera, gestorClientesLlamados, gestorPuestos);
        }
    }
}
