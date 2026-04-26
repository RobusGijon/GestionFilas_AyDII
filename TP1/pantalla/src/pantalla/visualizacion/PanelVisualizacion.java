package pantalla.visualizacion;

import javax.swing.SwingUtilities;
import pantalla.interfaces.IConexionPantalla;
import pantalla.vistas.SalaEspera;

public class PanelVisualizacion implements IConexionPantalla {

    private SalaEspera salaEspera;

    public PanelVisualizacion(SalaEspera salaEspera) {
        this.salaEspera = salaEspera;
    }

    @Override
    public void enviarDNI(String dni) {
        SwingUtilities.invokeLater(() -> {
            String turnoAnterior = salaEspera.getTurnoActual();
            salaEspera.actualizarClientesAnteriores(turnoAnterior);
            salaEspera.actualizarPantalla(dni);
        });
    }
}
