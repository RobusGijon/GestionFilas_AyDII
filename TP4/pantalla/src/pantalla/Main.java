package pantalla;

import java.awt.EventQueue;

import pantalla.controlador.ControladorConexionPantalla;
import pantalla.interfaces.IEstrategiaEncriptacion;
import pantalla.protocolo.encriptacion.Estrategias;
import pantalla.vistas.ConectarIP;

public class Main {

    public static void main(String[] args) {

        IEstrategiaEncriptacion encriptacion = Estrategias.getEstrategia(args);

        ControladorConexionPantalla controlador = new ControladorConexionPantalla(new ConectarIP(), encriptacion);
        EventQueue.invokeLater(() -> controlador.iniciar());
    }
}
