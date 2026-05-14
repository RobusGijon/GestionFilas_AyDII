package pantalla;

import java.awt.EventQueue;

import pantalla.controlador.ControladorConexionPantalla;
import pantalla.vistas.ConectarIP;

public class Main {

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new ControladorConexionPantalla(new ConectarIP()).iniciar());
    }
}
