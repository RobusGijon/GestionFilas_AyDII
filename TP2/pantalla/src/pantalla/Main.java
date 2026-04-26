package pantalla;

import java.awt.EventQueue;
import java.io.IOException;
import javax.swing.JOptionPane;
import pantalla.conexion.GestorConexionPantalla;
import pantalla.interfaces.IConexionPantalla;
import pantalla.util.ValidadorConexion;
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
                    JOptionPane.showMessageDialog(vistaConexion.getFrame(),
                            "Ingrese una IP valida (ej: 192.168.0.1).",
                            "IP invalida", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String textoPuerto = vistaConexion.getPuerto();
                if (!ValidadorConexion.isValidPuerto(textoPuerto)) {
                    JOptionPane.showMessageDialog(vistaConexion.getFrame(),
                            "Ingrese un puerto valido (0 - 65535).",
                            "Puerto invalido", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int puerto = Integer.parseInt(textoPuerto);

                SalaEspera vistaSala = new SalaEspera();
                IConexionPantalla panelVisualizacion = new PanelVisualizacion(vistaSala);
                GestorConexionPantalla cliente =
                        new GestorConexionPantalla(textoIP, puerto, panelVisualizacion);

                try {
                    cliente.conectar();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(vistaConexion.getFrame(),
                            "No se pudo conectar al Servidor Central en " + textoIP + ":" + puerto
                                    + ".\nVerifique que el servidor este activo e intente nuevamente.",
                            "Error de conexion", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                cliente.start();
                vistaConexion.setVisible(false);
                vistaSala.setVisible(true);
            });
        });
    }
}
