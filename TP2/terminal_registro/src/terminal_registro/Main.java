package terminal_registro;

import java.awt.EventQueue;
import javax.swing.JOptionPane;

import terminal_registro.conexion.GestorConexionRegistro;
import terminal_registro.interfaces.IConexionRegistro;
import terminal_registro.util.ValidadorConexion;
import terminal_registro.validacion.ValidadorRegistro;
import terminal_registro.vistas.ConectarIP;
import terminal_registro.vistas.TerminalAutoservicio;

public class Main {

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            ConectarIP vistaConexion = new ConectarIP();
            vistaConexion.setVisible(true);

            vistaConexion.addConectarListener(e -> {
                String textoIP = vistaConexion.getIPServidor();
                if (!ValidadorConexion.isValidIPv4(textoIP)) {
                    JOptionPane.showMessageDialog(vistaConexion.getFrame(),
                            "Ingrese una IP de Servidor valida.");
                    return;
                }

                String textoPuerto = vistaConexion.getPuertoServidor();
                if (!ValidadorConexion.isValidPuerto(textoPuerto)) {
                    JOptionPane.showMessageDialog(vistaConexion.getFrame(),
                            "Ingrese un puerto de Servidor valido (0 - 65535).");
                    return;
                }

                String ipServidor = textoIP;
                int puertoServidor = Integer.parseInt(textoPuerto);

                IConexionRegistro gestor = new GestorConexionRegistro(ipServidor, puertoServidor);
                TerminalAutoservicio vistaTerminal = new TerminalAutoservicio();

                vistaTerminal.addEnviarListener(ev -> {
                    String dni = vistaTerminal.getDNI();

                    if (!ValidadorRegistro.validaDNI(dni)) {
                        JOptionPane.showMessageDialog(vistaTerminal.getFrame(),
                                "DNI invalido. Ingrese 8 digitos numericos.");
                        return;
                    }

                    IConexionRegistro.ResultadoRegistro resultado = gestor.registrar(dni);
                    switch (resultado) {
                        case REGISTRADO:
                            JOptionPane.showMessageDialog(vistaTerminal.getFrame(),
                                    "Registro exitoso.");
                            vistaTerminal.limpiarDNI();
                            break;
                        case DUPLICADO:
                            JOptionPane.showMessageDialog(vistaTerminal.getFrame(),
                                    "DNI ya registrado en la cola.");
                            break;
                        case ERROR_CONEXION:
                        default:
                            JOptionPane.showMessageDialog(vistaTerminal.getFrame(),
                                    "Error de conexion con el servidor.");
                            break;
                    }
                });

                vistaConexion.setVisible(false);
                vistaTerminal.setVisible(true);
            });
        });
    }
}
