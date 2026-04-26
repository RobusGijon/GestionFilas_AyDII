package servidor_central;

import servidor_central.conexion.GestorConexion;
import servidor_central.conexion.PublicadorPantalla;
import servidor_central.gestores.CoordinadorServidor;
import servidor_central.gestores.GestorClientesLlamados;
import servidor_central.gestores.GestorFilaEspera;
import servidor_central.gestores.GestorPuestos;
import servidor_central.logging.ILogger;
import servidor_central.logging.LoggerServidor;

public class Main {
    
    private static final int PUERTO_POR_DEFECTO = 8080;
    
    public static void main(String[] args) {
        int puerto = PUERTO_POR_DEFECTO;
        if (args.length > 0) {
            try {
                puerto = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Puerto invalido: " + args[0] + ". Usando " + PUERTO_POR_DEFECTO);
                puerto = PUERTO_POR_DEFECTO;
            }
        }

        ILogger log = new LoggerServidor();
        log.logInfo("Arrancando servidor central en puerto " + puerto);

        GestorFilaEspera gfe = new GestorFilaEspera();
        GestorClientesLlamados gcl = new GestorClientesLlamados();
        GestorPuestos gp = new GestorPuestos();
        PublicadorPantalla pub = new PublicadorPantalla(log);

        CoordinadorServidor coord = new CoordinadorServidor(gfe, gcl, gp, pub, log);

        GestorConexion srv = new GestorConexion(puerto, coord, pub, log);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.logInfo("Signal de shutdown recibido, deteniendo servidor...");
            srv.detener();
        }));

        srv.iniciar();
    }
}