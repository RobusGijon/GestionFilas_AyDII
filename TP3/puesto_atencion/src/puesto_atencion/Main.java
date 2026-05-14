package puesto_atencion;

import java.awt.EventQueue;
import puesto_atencion.controlador.ControladorConexionPuesto;
import puesto_atencion.vistas.ConectarServidor;

public class Main {
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new ControladorConexionPuesto(new ConectarServidor()).iniciar());
    }
}
