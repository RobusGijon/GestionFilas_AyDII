package terminal_registro;

import java.awt.EventQueue;

import terminal_registro.controlador.ControladorConexionTerminal;
import terminal_registro.vistas.ConectarIP;

public class Main {

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new ControladorConexionTerminal(new ConectarIP()).iniciar());
    }
}
