package pantalla.visualizacion;

import javax.swing.SwingUtilities;
import pantalla.interfaces.IConexionPantalla;
import pantalla.vistas.SalaEspera;

public class PanelVisualizacion implements IConexionPantalla {

    private final SalaEspera salaEspera;

    public PanelVisualizacion(SalaEspera salaEspera) {
        this.salaEspera = salaEspera;
    }

    @Override
    public void recibirLlamado(String dni, int idPuesto) {
        SwingUtilities.invokeLater(() -> salaEspera.registrarLlamado(dni, idPuesto));
    }

    @Override
    public void recibirRenotificacion(String dni, int idPuesto) {
        SwingUtilities.invokeLater(() -> salaEspera.destacarRenotificacion(idPuesto));
    }

    @Override
    public void recibirAusente(int idPuesto) {
        SwingUtilities.invokeLater(() -> salaEspera.quitarLlamado(idPuesto));
    }
}
