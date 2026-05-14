package pantalla.conexion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pantalla.disponibilidad.ConfigHA;
import pantalla.disponibilidad.Direccion;
import pantalla.interfaces.IConexionPantalla;

public class GestorConexionRedundantePantalla extends Thread {

    private final List<Direccion> direcciones;
    private final IConexionPantalla panelVisualizacion;

    private volatile int indiceActivo = 0;
    private volatile GestorConexionPantalla gestorActivo;
    private volatile boolean corriendo;

    public GestorConexionRedundantePantalla(Direccion dirPrimaria,
            Direccion dirSecundaria,
            IConexionPantalla panelVisualizacion) {
        if (dirPrimaria == null)
            throw new IllegalArgumentException("direccion primaria requerida");

        this.direcciones = new ArrayList<>();
        this.direcciones.add(dirPrimaria);

        if (dirSecundaria != null)
            this.direcciones.add(dirSecundaria);
        this.panelVisualizacion = panelVisualizacion;

        setDaemon(true);
        setName("pantalla-supervisor");
    }

    public void conectar() throws IOException {
        IOException ultima = null;
        for (int i = 0; i < direcciones.size(); i++) {
            GestorConexionPantalla gestorCandidato = new GestorConexionPantalla(direcciones.get(i), panelVisualizacion);
            try {
                gestorCandidato.conectar();
                indiceActivo = i;
                gestorActivo = gestorCandidato;
                return;
            } catch (IOException e) {
                ultima = e;
            }
        }
        if (ultima != null)
            throw new IOException("sin nodos para conectar");
    }

    @Override
    public void run() {
        corriendo = true;

        if (gestorActivo == null) {
            try {
                conectar();
            } catch (IOException e) {
                log("no se pudo conectar inicialmente: " + e.getMessage());
                return;
            }
        }

        gestorActivo.start();
        while (corriendo) {
            try {
                gestorActivo.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (!corriendo) break;
            log("conexion con " + direcciones.get(indiceActivo) + " perdida, intentando failover");
            reconectar();
        }
    }

    private void reconectar() {
        while (corriendo) {
            for (int i = 1; i <= direcciones.size(); i++) {
                if (!corriendo)
                    return;
                int indiceCandidato = (indiceActivo + i) % direcciones.size();

                for (int intentoReconectar = 0; intentoReconectar < ConfigHA.RETRY_BACKOFF_MS.length; intentoReconectar++) {
                    if (!corriendo)
                        return;
                    GestorConexionPantalla gestorCandidato = new GestorConexionPantalla(direcciones.get(indiceCandidato), panelVisualizacion);
                    try {
                        gestorCandidato.conectar();
                        if (indiceCandidato != indiceActivo) {
                            log("failover " + direcciones.get(indiceActivo) + " -> "+ direcciones.get(indiceCandidato));
                            indiceActivo = indiceCandidato;
                        } else {
                            log("reconectado a " + direcciones.get(indiceActivo));
                        }
                        gestorActivo = gestorCandidato;
                        gestorActivo.start();
                        return;
                    } catch (IOException e) {
                        int pausaMs = ConfigHA.RETRY_BACKOFF_MS[intentoReconectar];
                        log("intento " + (intentoReconectar + 1) + " fallido contra "+ direcciones.get(indiceCandidato) + ", esperando " + pausaMs + "ms");
                        try {
                            Thread.sleep(pausaMs);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
                log("agotados los reintentos contra " + direcciones.get(indiceCandidato));
            }
            log("ronda completa fallida contra todas las direcciones, reiniciando ciclo");
        }
    }

    public void detener() {
        corriendo = false;
        if (gestorActivo != null)
            gestorActivo.detener();
        interrupt();
    }

    private void log(String msg) {
        System.out.println("[PANTALLA] " + msg);
    }
}
