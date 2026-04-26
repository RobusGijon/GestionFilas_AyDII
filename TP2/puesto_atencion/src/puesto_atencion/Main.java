package puesto_atencion;

import java.awt.EventQueue;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import puesto_atencion.conexion.GestorConexionServidor;
import puesto_atencion.dominio.HistorialLocal;
import puesto_atencion.dominio.ManejadorDeFila;
import puesto_atencion.interfaces.IGestorConexionServidor;
import puesto_atencion.interfaces.IHistorialLocal;
import puesto_atencion.interfaces.IManejadorDeFila;
import puesto_atencion.util.ValidadorConexion;
import puesto_atencion.vistas.ConectarServidor;
import puesto_atencion.vistas.PanelOperador;
import puesto_atencion.vistas.PanelOperador.EstadoBotones;

public class Main {

    private static final int COOLDOWN_SEGUNDOS = 30;
    private static final int MAX_INTENTOS = 3;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            ConectarServidor vistaConexion = new ConectarServidor();
            vistaConexion.setVisible(true);

            vistaConexion.addConectarListener(e -> {
                String ip = vistaConexion.getIPServidor();
                if (!ValidadorConexion.isValidIPv4(ip)) {
                    JOptionPane.showMessageDialog(vistaConexion.getFrame(),
                        "Ingrese una IP de servidor valida (ej: 192.168.0.1).");
                    return;
                }
                String puertoTxt = vistaConexion.getPuertoServidor();
                if (!ValidadorConexion.isValidPuerto(puertoTxt)) {
                    JOptionPane.showMessageDialog(vistaConexion.getFrame(),
                        "Ingrese un puerto valido (0 - 65535).");
                    return;
                }
                int puerto = Integer.parseInt(puertoTxt);

                IGestorConexionServidor conexion = new GestorConexionServidor(ip, puerto);
                IHistorialLocal historial = new HistorialLocal();
                IManejadorDeFila manejador = new ManejadorDeFila(conexion, historial);

                vistaConexion.setBotonHabilitado(false);
                new Thread(() -> {
                    try {
                        int idPuesto = manejador.conectar();
                        SwingUtilities.invokeLater(() -> {
                            vistaConexion.cerrar();
                            iniciarPanelOperador(manejador, idPuesto);
                        });
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(vistaConexion.getFrame(),
                                "No se pudo conectar al Servidor Central en " + ip + ":" + puerto
                                    + ".\nVerifique que el servidor este activo e intente nuevamente.",
                                "Error de conexion", JOptionPane.ERROR_MESSAGE);
                            vistaConexion.setBotonHabilitado(true);
                        });
                    }
                }).start();
            });
        });
    }

    private static void iniciarPanelOperador(IManejadorDeFila manejador, int idPuesto) {
        PanelOperador vista = new PanelOperador();
        vista.setIdPuesto(idPuesto);
        vista.actualizarHistorial(manejador.historial());
        vista.mostrarFilaVacia();
        vista.setEstadoBotones(EstadoBotones.SIN_CLIENTE);
        vista.setVisible(true);

        ControladorCooldown cooldown = new ControladorCooldown(vista, manejador);

        vista.addLlamarSiguienteListener(e -> {
            cooldown.detener();
            new Thread(() -> {
                try {
                    String dni = manejador.llamarSiguiente();
                    SwingUtilities.invokeLater(() -> {
                        vista.actualizarHistorial(manejador.historial());
                        if (dni == null) {
                            vista.mostrarFilaVacia();
                            vista.setEstadoBotones(EstadoBotones.SIN_CLIENTE);
                        } else {
                            vista.mostrarClienteActual(dni, manejador.getIntentosActual());
                            cooldown.iniciar();
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(vista.getFrame(),
                            "Error de comunicacion con el servidor: " + ex.getMessage()));
                }
            }).start();
        });

        vista.addRellamarListener(e -> {
            new Thread(() -> {
                try {
                    int nuevoIntento = manejador.reNotificar();
                    SwingUtilities.invokeLater(() -> {
                        vista.mostrarClienteActual(manejador.getDniActual(), nuevoIntento);
                        cooldown.iniciar();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(vista.getFrame(),
                            "Error de comunicacion con el servidor: " + ex.getMessage()));
                }
            }).start();
        });

        vista.addEliminarListener(e -> {
            cooldown.detener();
            new Thread(() -> {
                try {
                    String dni = manejador.eliminarCliente();
                    SwingUtilities.invokeLater(() -> {
                        if (dni == null) {
                            vista.mostrarFilaVacia();
                            vista.setEstadoBotones(EstadoBotones.SIN_CLIENTE);
                        } else {
                            vista.mostrarClienteActual(dni, manejador.getIntentosActual());
                            cooldown.iniciar();
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(vista.getFrame(),
                            "Error de comunicacion con el servidor: " + ex.getMessage()));
                }
            }).start();
        });
    }

    private static class ControladorCooldown {
        private final PanelOperador vista;
        private final IManejadorDeFila manejador;
        private Timer timer;
        private int segundosRestantes;

        ControladorCooldown(PanelOperador vista, IManejadorDeFila manejador) {
            this.vista = vista;
            this.manejador = manejador;
        }

        void iniciar() {
            detener();
            segundosRestantes = COOLDOWN_SEGUNDOS;
            EstadoBotones estadoCooldown = (manejador.getIntentosActual() >= MAX_INTENTOS)
                ? EstadoBotones.TERCER_INTENTO_EN_COOLDOWN
                : EstadoBotones.CON_CLIENTE_EN_COOLDOWN;
            vista.setEstadoBotones(estadoCooldown);
            vista.setTextoRellamar("Rellamar (" + segundosRestantes + "s)");

            timer = new Timer(1000, ev -> {
                segundosRestantes--;
                if (segundosRestantes > 0) {
                    vista.setTextoRellamar("Rellamar (" + segundosRestantes + "s)");
                } else {
                    timer.stop();
                    timer = null;
                    if (manejador.getIntentosActual() >= MAX_INTENTOS) {
                        vista.setEstadoBotones(EstadoBotones.TERCER_INTENTO_VENCIDO);
                    } else {
                        vista.setEstadoBotones(EstadoBotones.CON_CLIENTE_PUEDE_RELLAMAR);
                    }
                }
            });
            timer.start();
        }

        void detener() {
            if (timer != null) {
                timer.stop();
                timer = null;
            }
        }
    }
}
