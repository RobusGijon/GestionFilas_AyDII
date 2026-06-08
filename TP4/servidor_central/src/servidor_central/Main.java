package servidor_central;

import java.io.IOException;
import java.net.SocketTimeoutException;

import servidor_central.conexion.GestorConexion;
import servidor_central.conexion.GestorHeartbeat;
import servidor_central.conexion.GestorReplicacion;
import servidor_central.conexion.PublicadorPantalla;
import servidor_central.conexion.PublicadorPuestos;
import servidor_central.gestores.CoordinadorServidor;
import servidor_central.gestores.GestorClientesLlamados;
import servidor_central.gestores.GestorFilaEspera;
import servidor_central.gestores.GestorPuestos;
import servidor_central.disponibilidad.ConfigHA;
import servidor_central.disponibilidad.ListenerPar;
import servidor_central.disponibilidad.RolNodo;
import servidor_central.disponibilidad.SupervisorRol;
import servidor_central.interfaces.ICanalMensaje;
import servidor_central.interfaces.IEstrategiaEncriptacion;
import servidor_central.interfaces.factory.IFabricaPersistencia;
import servidor_central.logging.ILogger;
import servidor_central.logging.LoggerServidor;
import servidor_central.persistencia.ClaveInvalidaException;
import servidor_central.persistencia.FabricaPersistenciaFactory;
import servidor_central.persistencia.PersistenciaService;
import servidor_central.protocolo.CanalMensajes;
import servidor_central.protocolo.CanalMensajesEncriptados;
import servidor_central.protocolo.encriptacion.Estrategias;
import servidor_central.protocolo.mensajes.Direccion;
import servidor_central.protocolo.mensajes.Mensaje;
import servidor_central.protocolo.mensajes.TipoMensaje;

public class Main {

    private static final int PUERTO_POR_DEFECTO = 8080;

    public static void main(String[] args) {
        ArgsServidor parsed;
        try {
            parsed = ArgsServidor.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Argumentos invalidos: " + e.getMessage());
            System.err.println("Uso: java -cp bin servidor_central.Main "
                + "[--puerto N] [--rol primario|secundario] [--par ip:puerto] "
                + "[--encrip AES|XOR|CESAR|VIGENERE] [--formato json|xml|txt]");
            System.exit(2);
            return;
        }

        ILogger log = new LoggerServidor();
        log.logInfo("Arrancando servidor central en puerto " + parsed.puerto);

        IEstrategiaEncriptacion encriptacion;
        try {
            encriptacion = parsed.encrip == null
                    ? null
                    : Estrategias.getEstrategia(new String[] { parsed.encrip });
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println("Configuracion de encriptacion invalida: " + e.getMessage());
            System.exit(2);
            return;
        }
        if (encriptacion != null) {
            log.logInfo("Encriptacion habilitada: " + parsed.encrip);
        }

        GestorFilaEspera gfe = new GestorFilaEspera();
        GestorClientesLlamados gcl = new GestorClientesLlamados();
        GestorPuestos gp = new GestorPuestos();
        PublicadorPantalla pub = new PublicadorPantalla(log);
        PublicadorPuestos pubPuestos = new PublicadorPuestos(log);

        // Persistencia: cifrada si hay metodo, en claro si no.
        // El formato (json|xml|txt) se elige por flag --formato; la fabrica concreta
        // (Abstract Factory) la resuelve FabricaPersistenciaFactory (Factory Method).
        IFabricaPersistencia fabricaPersistencia = FabricaPersistenciaFactory.crear(parsed.formato);
        log.logInfo("Formato de persistencia: " + parsed.formato);
        PersistenciaService persistencia = new PersistenciaService(fabricaPersistencia, encriptacion);
        // Un UNICO archivo por combinacion cifrado+formato, p.ej. data/estado.aes.json
        // (o .plano. si no hay cifrado). Primario y secundario lo COMPARTEN a proposito:
        // asi el estado sobrevive al failover (el nodo promovido continua el mismo archivo
        // en vez de crear un fork por puerto). Solo el primario/standalone escribe, y con
        // escritura atomica (ver EscrituraAtomica) para evitar lecturas parciales.
        String archivoEstado = nombreArchivoEstado(parsed.encrip, parsed.formato);
        log.logInfo("Archivo de estado: data/" + archivoEstado);

        CoordinadorServidor coord = new CoordinadorServidor(gfe, gcl, gp, pub, pubPuestos, log,
                persistencia, archivoEstado);
        // (B2) Si se cae el canal de suscripcion de un puesto, dar de baja esa
        // conexion viva (el coordinador deja el puesto DESASIGNADO si era la ultima).
        pubPuestos.setOnPuestoCaido(coord::notificarDesconexionPuesto);

        // Sondeo al arranque: un nodo lanzado como PRIMARIO confirma con el par que
        // no haya ya OTRO primario activo ANTES de leer el archivo o servir. Como el
        // archivo de estado es unico y compartido, un primario reiniciado/tardio que
        // arrancara a ciegas pisaria el estado del primario legitimo durante la breve
        // ventana hasta auto-demoverse. Si el par ya es primario, arrancamos
        // directamente como SECUNDARIO (esperaremos su snapshot).
        boolean parYaEsPrimario = parsed.haHabilitada()
                && parsed.rol == RolNodo.PRIMARIO && parsed.par != null
                && hayPrimarioActivoEnPar(parsed.par, encriptacion, log);
        RolNodo rolEfectivo = rolEfectivoArranque(parsed.rol, parYaEsPrimario);
        if (rolEfectivo != parsed.rol) {
            log.logInfo("El par " + parsed.par + " ya es PRIMARIO activo: arranco como "
                + "SECUNDARIO (no leo el archivo, espero el snapshot del primario).");
        }

        // El secundario no persiste: la persistencia es responsabilidad del
        // primario. Se reactivará si este nodo es promovido (ver SupervisorRol).
        if (rolEfectivo == RolNodo.SECUNDARIO) {
            coord.setPersistenciaActiva(false);
        }
        // Leemos el archivo solo si somos el primario confirmado, o si fuimos lanzados
        // como secundario (red de seguridad: si luego promocionamos sin haber recibido
        // snapshot, necesitamos el ultimo estado en RAM para no volcar estado vacio). Un
        // primario auto-demovido al arranque NO lee: recibira el snapshot del primario.
        boolean debeRestaurar = debeRestaurarArchivo(parsed.rol, rolEfectivo);
        if (debeRestaurar) {
            try {
                coord.restaurarEstado();
            } catch (ClaveInvalidaException e) {
                System.err.println("No se puede arrancar el servidor: " + e.getMessage());
                System.err.println("El estado persistido en data/" + archivoEstado
                        + " no coincide con la clave/cifrado configurado. "
                        + "Verifique --encrip y la clave en shared/.env, o borre el archivo para arrancar limpio.");
                System.exit(3);
                return;
            }
        }

        GestorConexion srv = new GestorConexion(parsed.puerto, coord, pub, encriptacion, log);

        SupervisorRol supervisor = null;
        GestorHeartbeat heartbeat = null;
        GestorReplicacion replicacion = null;
        ListenerPar listenerPar = null;
        if (parsed.haHabilitada()) {
            supervisor  = new SupervisorRol(rolEfectivo, parsed.par, log);
            log.setProveedorRol(supervisor::getRol);
            heartbeat   = new GestorHeartbeat(rolEfectivo, parsed.par, encriptacion, log);
            // SupervisorRol es el observador: GestorReplicacion ya no conoce
            // a CoordinadorServidor directamente.
            replicacion = new GestorReplicacion(rolEfectivo, parsed.par, encriptacion, supervisor, log);
            listenerPar = new ListenerPar(parsed.puerto, encriptacion, log);
            listenerPar.setHandlerHeartbeat(heartbeat::manejarMensajeRecibido);
            listenerPar.setHandlerReplicacion(replicacion::manejarMensajeRecibido);
            if (rolEfectivo == RolNodo.SECUNDARIO) {
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

        if (rolEfectivo == RolNodo.SECUNDARIO) {
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

    /**
     * Nombre del archivo de estado: un UNICO archivo por combinacion cifrado+formato,
     * sin puerto, p.ej. {@code estado.aes.json} o {@code estado.plano.txt}. Compartido
     * a proposito entre primario y secundario para que el estado sobreviva al failover.
     */
    public static String nombreArchivoEstado(String encrip, String formato) {
        String encripNombre = (encrip == null) ? "plano" : encrip.toLowerCase();
        return "estado." + encripNombre + "." + formato;
    }

    /**
     * Rol efectivo al arranque: si fui lanzado como PRIMARIO pero el par ya es un
     * primario activo, arranco como SECUNDARIO (evita pisar el archivo compartido).
     * En cualquier otro caso conservo el rol lanzado.
     */
    public static RolNodo rolEfectivoArranque(RolNodo lanzado, boolean parYaEsPrimario) {
        return (lanzado == RolNodo.PRIMARIO && parYaEsPrimario) ? RolNodo.SECUNDARIO : lanzado;
    }

    /**
     * ¿Debe este nodo leer el archivo al arrancar? Lee el primario confirmado, y tambien
     * el nodo lanzado como secundario (red de seguridad para una promocion sin snapshot).
     * El unico que NO lee es un primario auto-demovido al arranque: recibira el snapshot.
     */
    public static boolean debeRestaurarArchivo(RolNodo lanzado, RolNodo efectivo) {
        return efectivo == RolNodo.PRIMARIO || lanzado == RolNodo.SECUNDARIO;
    }

    /**
     * Sondeo sincrono al arranque: ¿hay ya un PRIMARIO activo en el par? Se conecta
     * al puerto principal del par y envia {@code HOLA_PAR|PRIMARIO}. Solo un primario
     * activo (que tiene su {@code GestorConexion} sirviendo) responde
     * {@code ERROR|YA_HAY_PRIMARIO} (ver {@code ManejadorConexion}); un secundario en
     * espera lo recibe por su {@code ListenerPar} y no contesta (timeout), y un par
     * caido ni siquiera acepta la conexion. En esos casos devolvemos {@code false}.
     */
    public static boolean hayPrimarioActivoEnPar(Direccion par, IEstrategiaEncriptacion enc, ILogger log) {
        ICanalMensaje c = null;
        try {
            c = (enc == null) ? new CanalMensajes(par) : new CanalMensajesEncriptados(par, enc);
            c.getSocket().setSoTimeout(ConfigHA.SOCKET_TIMEOUT_MS);
            c.enviar(new Mensaje(TipoMensaje.HOLA_PAR, "PRIMARIO"));
            Mensaje rm = c.recibir();
            return rm != null
                    && rm.getTipo() == TipoMensaje.ERROR
                    && !rm.getArgs().isEmpty()
                    && "YA_HAY_PRIMARIO".equals(rm.getArg(0));
        } catch (SocketTimeoutException e) {
            return false; // el par no respondio al HOLA_PAR: no es un primario activo
        } catch (IOException | IllegalArgumentException e) {
            log.logInfo("[HA] sondeo al par " + par + " sin primario activo (" + e.getMessage() + ")");
            return false; // par caido / inalcanzable / respuesta no parseable
        } finally {
            if (c != null) {
                c.cerrar();
            }
        }
    }

    private static final class ArgsServidor {
        int puerto = PUERTO_POR_DEFECTO;
        RolNodo rol = null;
        Direccion par = null;
        String encrip = null;
        String formato = FabricaPersistenciaFactory.FORMATO_POR_DEFECTO;

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
                        r.par = Direccion.parse(valor(args, ++i, "--par"));
                        i++;
                        break;
                    case "--encrip":
                        r.encrip = valor(args, ++i, "--encrip");
                        i++;
                        break;
                    case "--formato":
                        r.formato = parseFormato(valor(args, ++i, "--formato"));
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

        private static String parseFormato(String s) {
            String f = s.toLowerCase();
            switch (f) {
                case "json":
                case "xml":
                case "txt":
                    return f;
                default:
                    throw new IllegalArgumentException(
                            "--formato debe ser json|xml|txt, recibido: " + s);
            }
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
