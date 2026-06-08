package pantalla.visualizacion;

import javax.swing.SwingUtilities;
import pantalla.interfaces.IConexionPantalla;
import pantalla.protocolo.mensajes.Mensaje;
import pantalla.vistas.SalaEspera;

public class PanelVisualizacion implements IConexionPantalla {

    private final SalaEspera salaEspera;

    public PanelVisualizacion(SalaEspera salaEspera) {
        this.salaEspera = salaEspera;
    }

    @Override
    public void actualizarLlamado(Mensaje respuestaServidor) {
        SwingUtilities.invokeLater(() -> salaEspera.registrarLlamado(respuestaServidor));
    }

    @Override
    public void actualizarRenotificacion(Mensaje respuestaServidor) {
        SwingUtilities.invokeLater(() -> salaEspera.destacarRenotificacion(respuestaServidor));
    }

    @Override
    public void actualizarAusente(Mensaje respuestaServidor) {
        SwingUtilities.invokeLater(() -> salaEspera.quitarLlamado(respuestaServidor));
    }

    @Override
    public void actualizarHistorial(Mensaje respuestaServidor) {
        SwingUtilities.invokeLater(() -> salaEspera.restaurarHistorial(respuestaServidor));
    }
}
