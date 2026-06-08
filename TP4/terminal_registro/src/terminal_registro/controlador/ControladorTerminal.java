package terminal_registro.controlador;

import javax.swing.JOptionPane;

import terminal_registro.util.Validador;
import terminal_registro.interfaces.IConexionRegistro;
import terminal_registro.interfaces.IConexionRegistro.ResultadoRegistro;
import terminal_registro.vistas.TerminalAutoservicio;

public class ControladorTerminal {

    private final TerminalAutoservicio vista;
    private final IConexionRegistro gestorConexion;

    public ControladorTerminal(TerminalAutoservicio vista, IConexionRegistro gestor) {
        this.vista = vista;
        this.gestorConexion = gestor;
    }

    public void iniciar() {
        vista.addEnviarListener(ev -> manejarEnviar());
        vista.setVisible(true);
    }

    private void manejarEnviar() {
        String dni = vista.getDNI();

        if (!Validador.isValidDNI(dni)) {
            JOptionPane.showMessageDialog(vista.getFrame(), "DNI invalido. Ingrese 8 digitos numericos.");
            return;
        }

        ResultadoRegistro resultado = gestorConexion.registrar(dni);
        switch (resultado) {
            case REGISTRADO:
                JOptionPane.showMessageDialog(vista.getFrame(), "Registro exitoso.");
                vista.limpiarDNI();
                break;
            case DUPLICADO:
                JOptionPane.showMessageDialog(vista.getFrame(), "DNI ya registrado en la cola.");
                break;
            case ERROR_CONEXION:
            default:
                JOptionPane.showMessageDialog(vista.getFrame(), "Error de conexion con el servidor.");
                break;
        }
    }
}
