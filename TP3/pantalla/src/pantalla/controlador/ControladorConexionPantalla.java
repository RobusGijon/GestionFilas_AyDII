package pantalla.controlador;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.swing.JOptionPane;

import pantalla.conexion.GestorConexionRedundantePantalla;
import pantalla.disponibilidad.ConfigHA;
import pantalla.disponibilidad.Direccion;
import pantalla.interfaces.IConexionPantalla;
import pantalla.util.ValidadorConexion;
import pantalla.visualizacion.PanelVisualizacion;
import pantalla.vistas.ConectarIP;
import pantalla.vistas.SalaEspera;

public class ControladorConexionPantalla {

    private final ConectarIP vista;

    public ControladorConexionPantalla(ConectarIP vista) {
        this.vista = vista;
    }

    public void iniciar() {
        vista.addConectarListener(e -> manejarConectar());
        vista.setVisible(true);
    }

    private void manejarConectar() {
        String ipPrimaria = vista.getIP();
        if (!ValidadorConexion.isValidIPv4(ipPrimaria)) {
            JOptionPane.showMessageDialog(vista.getFrame(), "Ingrese una IP valida (ej: 192.168.0.1).","IP invalida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String textoPuertoPrimario = vista.getPuerto();
        if (!ValidadorConexion.isValidPuerto(textoPuertoPrimario)) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                    "Ingrese un puerto valido (0 - 65535).",
                    "Puerto invalido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int puertoPrimario = Integer.parseInt(textoPuertoPrimario);
        Direccion direccionPrimaria = new Direccion(ipPrimaria, puertoPrimario);

        String ipSecundaria = vista.getIPSecundario();
        String textoPuertoSecundario = vista.getPuertoSecundario();

        Direccion direccionSecundaria = null;
        if (!ipSecundaria.isEmpty() || !textoPuertoSecundario.isEmpty()) {
            direccionSecundaria = obtenerDireccionSecundaria(ipSecundaria, textoPuertoSecundario, direccionPrimaria);
            if (direccionSecundaria == null) {
                return;
            }
        }

        SalaEspera vistaSala = new SalaEspera();
        IConexionPantalla panelVisualizacion = new PanelVisualizacion(vistaSala);
        GestorConexionRedundantePantalla cliente = new GestorConexionRedundantePantalla(direccionPrimaria,
                direccionSecundaria, panelVisualizacion);

        try {
            cliente.conectar();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                    "No se pudo conectar a ningun servidor configurado."
                            + "\nVerifique que el servidor este activo e intente nuevamente.",
                    "Error de conexion", JOptionPane.ERROR_MESSAGE);
            return;
        }

        cliente.start();
        vista.setVisible(false);
        vistaSala.setVisible(true);
    }

    private Direccion obtenerDireccionSecundaria(String ipSecundaria, String textoPuertoSecundario,
            Direccion direccionPrimaria) {

        if (ipSecundaria.isEmpty() && !textoPuertoSecundario.isEmpty()) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                    "Ingrese la IP secundaria o elimine el puerto secundario.",
                    "IP invalida", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        if (!ipSecundaria.isEmpty() && textoPuertoSecundario.isEmpty()) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                    "Ingrese el puerto de la IP secundaria o elimine la IP secundaria.",
                    "Puerto invalido", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        if (!ValidadorConexion.isValidIPv4(ipSecundaria)) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                    "IP secundaria invalida.",
                    "IP invalida", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        if (!ValidadorConexion.isValidPuerto(textoPuertoSecundario)) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                    "Puerto secundario invalido (0 - 65535).",
                    "Puerto invalido", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int puertoSecundario = Integer.parseInt(textoPuertoSecundario);

        Direccion direccionSecundaria = new Direccion(ipSecundaria, puertoSecundario);

        if (direccionSecundaria.equals(direccionPrimaria)) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                    "La direccion secundaria debe ser distinta de la primaria.",
                    "Direcciones duplicadas", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        return direccionSecundaria;
    }
}
