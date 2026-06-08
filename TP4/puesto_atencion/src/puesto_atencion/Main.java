package puesto_atencion;

import java.awt.EventQueue;
import javax.swing.JOptionPane;
import puesto_atencion.controlador.ControladorConexionPuesto;
import puesto_atencion.interfaces.IEstrategiaEncriptacion;
import puesto_atencion.protocolo.encriptacion.Estrategias;
import puesto_atencion.vistas.ConectarServidor;

public class Main {
    public static void main(String[] args) {
        // El metodo de encriptacion llega por args (lo pasa el make run); sin args
        // = sin encriptacion. La clave se lee de shared/.env.
        final IEstrategiaEncriptacion encriptacion;
        try {
            encriptacion = Estrategias.getEstrategia(args);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(null,
                    "No se pudo inicializar la encriptacion: " + e.getMessage(),
                    "Error de configuracion", JOptionPane.ERROR_MESSAGE);
            return;
        }
        EventQueue.invokeLater(() ->
                new ControladorConexionPuesto(new ConectarServidor(), encriptacion).iniciar());
    }
}
