package pantalla.visualizacion;

import javax.swing.SwingUtilities;
import pantalla.interfaces.IConexionPantalla;
import pantalla.protocolo.Mensaje;
import pantalla.vistas.SalaEspera;

public class PanelVisualizacion implements IConexionPantalla {

    private final SalaEspera salaEspera;

    public PanelVisualizacion(SalaEspera salaEspera) {
        this.salaEspera = salaEspera;
    }

    @Override
    public void recibirLlamado(Mensaje respuestaServidor) {
        SwingUtilities.invokeLater(() -> salaEspera.registrarLlamado(respuestaServidor));
    }

    @Override
    public void recibirRenotificacion(Mensaje respuestaServidor) {
        SwingUtilities.invokeLater(() -> salaEspera.destacarRenotificacion(respuestaServidor));
    }

    @Override
    public void recibirAusente(Mensaje respuestaServidor) {
        SwingUtilities.invokeLater(() -> salaEspera.quitarLlamado(respuestaServidor));
    }
}
