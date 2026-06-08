package terminal_registro;

import java.awt.EventQueue;

import terminal_registro.protocolo.encriptacion.Estrategias;
import terminal_registro.controlador.ControladorConexionTerminal;
import terminal_registro.interfaces.IEstrategiaEncriptacion;
import terminal_registro.vistas.ConectarIP;


public class Main {

    public static void main(String[] args) {

        IEstrategiaEncriptacion encriptacion = Estrategias.getEstrategia(args);

        ControladorConexionTerminal controlador = new ControladorConexionTerminal(new ConectarIP(), encriptacion);
        EventQueue.invokeLater(() -> controlador.iniciar());
    }
}
