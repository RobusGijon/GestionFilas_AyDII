package servidor_central;

import servidor_central.conexion.GestorConexion;
import servidor_central.conexion.GestorHeartbeat;
import servidor_central.conexion.GestorReplicacion;
import servidor_central.conexion.PublicadorPantalla;
import servidor_central.conexion.PublicadorPuestos;
import servidor_central.gestores.CoordinadorServidor;
import servidor_central.gestores.GestorClientesLlamados;
import servidor_central.gestores.GestorFilaEspera;
import servidor_central.gestores.GestorPuestos;
import servidor_central.disponibilidad.DireccionPar;
import servidor_central.disponibilidad.ListenerPar;
import servidor_central.disponibilidad.RolNodo;
import servidor_central.disponibilidad.SupervisorRol;
import servidor_central.logging.ILogger;
import servidor_central.logging.LoggerServidor;

public class Main {

    private static final int PUERTO_POR_DEFECTO = 8080;

    public static void main(String[] args) {
        ArgsServidor parsed;
        try {
            parsed = ArgsServidor.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Argumentos invalidos: " + e.getMessage());
            System.err.println("Uso: java -cp bin servidor_central.Main "
                + "[--puerto N] [--rol primario|secundario] [--par ip:puerto]");
            System.exit(2);
            return;
        }

        ILogger log = new LoggerServidor();
        log.logInfo("Arrancando servidor central en puerto " + parsed.puerto);

        GestorFilaEspera gfe = new GestorFilaEspera();
        GestorClientesLlamados gcl = new GestorClientesLlamados();
        GestorPuestos gp = new GestorPuestos();
        PublicadorPantalla pub = new PublicadorPantalla(log);
        PublicadorPuestos pubPuestos = new PublicadorPuestos(log);

        CoordinadorServidor coord = new CoordinadorServidor(gfe, gcl, gp, pub, pubPuestos, log);

        GestorConexion srv = new GestorConexion(parsed.puerto, coord, pub, log);

        SupervisorRol supervisor = null;
        GestorHeartbeat heartbeat = null;
        GestorReplicacion replicacion = null;
        ListenerPar listenerPar = null;
        if (parsed.haHabilitada()) {
            supervisor  = new SupervisorRol(parsed.rol, parsed.par, log);
            log.setProveedorRol(supervisor::getRol);
            heartbeat   = new GestorHeartbeat(parsed.rol, parsed.par, log);
            // SupervisorRol es el observador: GestorReplicacion ya no conoce
            // a CoordinadorServidor directamente.
            replicacion = new GestorReplicacion(parsed.rol, parsed.par, supervisor, log);
            listenerPar = new ListenerPar(parsed.puerto, log);
            listenerPar.setHandlerHeartbeat(heartbeat::manejarMensajeRecibido);
            listenerPar.setHandlerReplicacion(replicacion::manejarMensajeRecibido);
            if (parsed.rol == RolNodo.SECUNDARIO) {
                listenerPar.iniciar();
            }
            supervisor.setCoord(coord);
            supervisor.setHeartbeat(heartbeat);
            supervisor.setReplicacion(replicacion);
            supervisor.setListenerPar(listenerPar);
            supervisor.setGestorConexion(srv);
            heartbeat.setCallbackCaida(supervisor::promoverAPrimario);
            replicacion.setCallbackSplitBrain(supervisor::demoverASecundario);

            supervisor.iniciar();
            heartbeat.iniciar();
            replicacion.iniciar();
        }

        final SupervisorRol  fSupervisor  = supervisor;
        final GestorHeartbeat fHeartbeat  = heartbeat;
        final GestorReplicacion fReplica  = replicacion;
        final ListenerPar fListener = listenerPar;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.logInfo("Signal de shutdown recibido, deteniendo servidor...");
            if (fReplica != null)    fReplica.detener();
            if (fHeartbeat != null)  fHeartbeat.detener();
            if (fListener != null)   fListener.detener();
            if (fSupervisor != null) fSupervisor.detener();
            srv.detener();
        }));

        if (parsed.rol == RolNodo.SECUNDARIO) {
            log.logInfo("Modo secundario activo: el servidor queda en espera. "
                + "No se aceptan conexiones de clientes hasta promocion.");
            sleepForever();
        } else {
            srv.iniciar();
            // srv.iniciar() retorna sólo cuando se cerró el ServerSocket. Si la
            // razón fue una auto-democión, mantenemos el proceso vivo como
            // secundario para que reciba el snapshot del primario activo.
            if (supervisor != null && supervisor.getRol() == RolNodo.SECUNDARIO) {
                log.logInfo("Tras democion, nodo activo en modo secundario");
                sleepForever();
            }
        }
    }

    private static void sleepForever() {
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class ArgsServidor {
        int puerto = PUERTO_POR_DEFECTO;
        RolNodo rol = null;
        DireccionPar par = null;

        boolean haHabilitada() {
            return rol != null;
        }

        static ArgsServidor parse(String[] args) {
            ArgsServidor r = new ArgsServidor();
            int i = 0;
            while (i < args.length) {
                String a = args[i];
                switch (a) {
                    case "--puerto":
                        r.puerto = parsePuerto(valor(args, ++i, "--puerto"));
                        i++;
                        break;
                    case "--rol":
                        r.rol = parseRol(valor(args, ++i, "--rol"));
                        i++;
                        break;
                    case "--par":
                        r.par = DireccionPar.parse(valor(args, ++i, "--par"));
                        i++;
                        break;
                    default:
                        if (a.startsWith("--")) {
                            throw new IllegalArgumentException("flag desconocido: " + a);
                        }
                        if (i == 0) {
                            r.puerto = parsePuerto(a);
                            i++;
                            break;
                        }
                        throw new IllegalArgumentException("argumento posicional inesperado: " + a);
                }
            }
            if (r.rol != null && r.par == null) {
                throw new IllegalArgumentException("--rol requiere --par ip:puerto");
            }
            if (r.rol == null && r.par != null) {
                throw new IllegalArgumentException("--par requiere --rol primario|secundario");
            }
            return r;
        }

        private static String valor(String[] args, int idx, String flag) {
            if (idx >= args.length) {
                throw new IllegalArgumentException(flag + " requiere un valor");
            }
            return args[idx];
        }

        private static int parsePuerto(String s) {
            int p;
            try {
                p = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("puerto invalido: " + s);
            }
            if (p < 0 || p > 65535) {
                throw new IllegalArgumentException("puerto fuera de rango: " + p);
            }
            return p;
        }

        private static RolNodo parseRol(String s) {
            switch (s.toLowerCase()) {
                case "primario":   return RolNodo.PRIMARIO;
                case "secundario": return RolNodo.SECUNDARIO;
                default:
                    throw new IllegalArgumentException("--rol debe ser primario|secundario, recibido: " + s);
            }
        }
    }
}
