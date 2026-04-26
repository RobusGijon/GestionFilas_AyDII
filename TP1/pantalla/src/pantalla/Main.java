package pantalla;

import java.awt.EventQueue;
import javax.swing.JOptionPane;
import pantalla.util.ValidadorConexion;
import pantalla.conexion.GestorConexionesMonitorSala;
import pantalla.interfaces.IConexionPantalla;
import pantalla.visualizacion.PanelVisualizacion;
import pantalla.vistas.ConectarIP;
import pantalla.vistas.SalaEspera;

public class Main {

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            ConectarIP vistaConexion = new ConectarIP();
            vistaConexion.setVisible(true);

            vistaConexion.addConectarListener(e -> {
                String textoIP = vistaConexion.getIP();
                if (!ValidadorConexion.isValidIPv4(textoIP)) {
                    JOptionPane.showMessageDialog(vistaConexion.getFrame(), "Ingrese una IP valida (ej: 192.168.0.1).");
                    return;
                }

                String textoPort = vistaConexion.getPuerto();
                if (!ValidadorConexion.isValidPuerto(textoPort)) {
                    JOptionPane.showMessageDialog(vistaConexion.getFrame(), "Ingrese un puerto valido (0 - 65535).");
                    return;
                }
                int puerto = Integer.parseInt(textoPort);

                SalaEspera vistaSala = new SalaEspera();
                IConexionPantalla panelVisualizacion = new PanelVisualizacion(vistaSala);
                GestorConexionesMonitorSala gestor = new GestorConexionesMonitorSala(puerto, panelVisualizacion);
                gestor.start();

                vistaConexion.setVisible(false);
                vistaSala.setVisible(true);
            });
        });
    }
}
