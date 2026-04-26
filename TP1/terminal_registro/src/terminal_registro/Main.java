package terminal_registro;

import java.awt.EventQueue;
import javax.swing.JOptionPane;
import terminal_registro.conexion.GestorConexionesRegistro;
import terminal_registro.interfaces.IConexionRegistro;
import terminal_registro.util.ValidadorConexion;
import terminal_registro.validacion.ValidadorRegistro;
import terminal_registro.vistas.ConectarIP;
import terminal_registro.vistas.TerminalAutoservicio;

public class Main {

    private static String ipAtencion;
    private static int puertoAtencion;
    private static String ipPantalla;
    private static int puertoPantalla;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            ConectarIP vistaConexion = new ConectarIP();
            vistaConexion.setVisible(true);

            vistaConexion.addConectarListener(e -> {
                String textoIPAtencion = vistaConexion.getIPAtencion();
                if (!ValidadorConexion.isValidIPv4(textoIPAtencion)) {
                    JOptionPane.showMessageDialog(vistaConexion.getFrame(), "Ingrese una IP de Atencion valida.");
                    return;
                }

                String textoPuertoAtencion = vistaConexion.getPuertoAtencion();
                if (!ValidadorConexion.isValidPuerto(textoPuertoAtencion)) {
                    JOptionPane.showMessageDialog(vistaConexion.getFrame(), "Ingrese un puerto de Atencion valido (0 - 65535).");
                    return;
                }

                String textoIPPantalla = vistaConexion.getIPPantalla();
                if (!ValidadorConexion.isValidIPv4(textoIPPantalla)) {
                    JOptionPane.showMessageDialog(vistaConexion.getFrame(), "Ingrese una IP de Pantalla valida.");
                    return;
                }

                String textoPuertoPantalla = vistaConexion.getPuertoPantalla();
                if (!ValidadorConexion.isValidPuerto(textoPuertoPantalla)) {
                    JOptionPane.showMessageDialog(vistaConexion.getFrame(), "Ingrese un puerto de Pantalla valido (0 - 65535).");
                    return;
                }

                ipAtencion = textoIPAtencion;
                puertoAtencion = Integer.parseInt(textoPuertoAtencion);
                ipPantalla = textoIPPantalla;
                puertoPantalla = Integer.parseInt(textoPuertoPantalla);

                TerminalAutoservicio vistaTerminal = new TerminalAutoservicio();
                IConexionRegistro gestor = new GestorConexionesRegistro();

                vistaTerminal.addEnviarListener(ev -> {
                    String dni = vistaTerminal.getDNI();

                    if (!ValidadorRegistro.validaDNI(dni)) {
                        JOptionPane.showMessageDialog(vistaTerminal.getFrame(), "DNI invalido. Ingrese solo numeros.");
                        return;
                    }

                    try {
                        gestor.enviarMensaje(ipAtencion, puertoAtencion, dni);
                        gestor.enviarMensaje(ipPantalla, puertoPantalla, "REGISTRO:" + dni);
                        JOptionPane.showMessageDialog(vistaTerminal.getFrame(), "Agregado a la fila exitosamente.");
                        vistaTerminal.limpiarDNI();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(vistaTerminal.getFrame(), "Error de conexion: no se pudo conectar.");
                    }
                });

                vistaConexion.setVisible(false);
                vistaTerminal.setVisible(true);
            });
        });
    }
}
