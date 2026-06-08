package puesto_atencion.controlador;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import puesto_atencion.interfaces.IGestorConexionServidor;
import puesto_atencion.interfaces.IGestorAtencion;
import puesto_atencion.vistas.PanelOperador;
import puesto_atencion.vistas.PanelOperador.EstadoBotones;

public class ControladorPanelOperador {

    private static final int COOLDOWN_SEGUNDOS = 30;
    private static final int MAX_INTENTOS = 3;

    private final PanelOperador vista;
    private final IGestorAtencion manejador;
    private final IGestorConexionServidor conexion;
    private final int idPuesto;

    public ControladorPanelOperador(PanelOperador vista,
                                    IGestorAtencion manejador,
                                    IGestorConexionServidor conexion,
                                    int idPuesto) {
        this.vista = vista;
        this.manejador = manejador;
        this.conexion = conexion;
        this.idPuesto = idPuesto;
    }

    public void iniciar() {
        vista.setIdPuesto(idPuesto);
        vista.actualizarHistorial(manejador.historial());
        // Si al (re)conectar el servidor reenvio un cliente en atencion, lo
        // mostramos recuperado; si no, arrancamos sin cliente.
        if (manejador.hayClienteActual()) {
            int intentos = manejador.getIntentosActual();
            vista.mostrarClienteActual(manejador.getDniActual(), intentos);
            vista.setEstadoBotones(intentos >= MAX_INTENTOS
                    ? EstadoBotones.TERCER_INTENTO_VENCIDO
                    : EstadoBotones.CON_CLIENTE_PUEDE_RELLAMAR);
        } else {
            vista.mostrarFilaVacia();
            vista.setEstadoBotones(EstadoBotones.SIN_CLIENTE);
        }
        vista.setClientesEnEspera(0);
        vista.setVisible(true);

        try {
            conexion.suscribirEventos(idPuesto, tamanio -> SwingUtilities.invokeLater(() -> vista.setClientesEnEspera(tamanio)));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                "No se pudo suscribir a eventos del servidor: " + ex.getMessage()
                    + "\nEl contador de clientes en espera no se actualizara.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
        }

        vista.getFrame().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                conexion.cerrarSuscripcion();
            }
        });

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
        private final IGestorAtencion manejador;
        private Timer timer;
        private int segundosRestantes;

        ControladorCooldown(PanelOperador vista, IGestorAtencion manejador) {
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
