package puesto_atencion;

import java.awt.EventQueue;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import puesto_atencion.conexion.GestorConexionesAtencion;
import puesto_atencion.logica.ManejadorDeFila;
import puesto_atencion.util.ValidadorConexion;
import puesto_atencion.vistas.ConectarIP;
import puesto_atencion.vistas.PersonalAtencion;

public class Main {

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            ConectarIP vistaConexion = new ConectarIP();
            vistaConexion.setVisible(true);

            vistaConexion.addConectarListener(e -> {
                String textoPuertoEscucha = vistaConexion.getPuertoEscucha();
                if (!ValidadorConexion.isValidPuerto(textoPuertoEscucha)) {
                    JOptionPane.showMessageDialog(vistaConexion.getFrame(), "Ingrese un puerto de escucha valido (0 - 65535).");
                    return;
                }

                String ipMonitor = vistaConexion.getIPMonitor();
                if (!ValidadorConexion.isValidIPv4(ipMonitor)) {
                    JOptionPane.showMessageDialog(vistaConexion.getFrame(), "Ingrese una IP de Pantalla valida (ej: 192.168.0.1).");
                    return;
                }

                String textoPuertoMonitor = vistaConexion.getPuertoMonitor();
                if (!ValidadorConexion.isValidPuerto(textoPuertoMonitor)) {
                    JOptionPane.showMessageDialog(vistaConexion.getFrame(), "Ingrese un puerto de Pantalla valido (0 - 65535).");
                    return;
                }

                int puertoEscucha = Integer.parseInt(textoPuertoEscucha);
                int puertoMonitor = Integer.parseInt(textoPuertoMonitor);

                ManejadorDeFila manejador = new ManejadorDeFila();
                PersonalAtencion vistaAtencion = new PersonalAtencion();

                GestorConexionesAtencion gestor = new GestorConexionesAtencion(
                    puertoEscucha,
                    ipMonitor,
                    puertoMonitor,
                    manejador,
                    (dni) -> {
                        SwingUtilities.invokeLater(() -> {
                            vistaAtencion.agregarDNI(dni);
                        });
                    }
                );
                gestor.start();

                vistaAtencion.addLlamarListener(ev -> {
                    if (manejador.filaVacia()) {
                        JOptionPane.showMessageDialog(vistaAtencion.getFrame(), "No hay clientes en la fila.");
                        return;
                    }

                    String dni = manejador.retiraPrimerDNI();
                    vistaAtencion.removerPrimero();
                    vistaAtencion.mostrarClienteLlamado(dni);

                    new Thread(() -> {
                        try {
                            gestor.enviarDNI(dni);
                        } catch (Exception ex) {
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(vistaAtencion.getFrame(), "Error al enviar al Monitor de Sala.");
                            });
                        }
                    }).start();
                });

                vistaConexion.setVisible(false);
                vistaAtencion.setVisible(true);
            });
        });
    }
}
