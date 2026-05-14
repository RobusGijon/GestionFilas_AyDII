package puesto_atencion.controlador;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import puesto_atencion.conexion.GestorConexionRedundanteServidor;
import puesto_atencion.disponibilidad.ConfigHA;
import puesto_atencion.disponibilidad.Direccion;
import puesto_atencion.dominio.HistorialLocal;
import puesto_atencion.dominio.GestorAtencion;
import puesto_atencion.interfaces.IGestorConexionServidor;
import puesto_atencion.interfaces.IHistorialLocal;
import puesto_atencion.interfaces.IGestorAtencion;
import puesto_atencion.util.ValidadorConexion;
import puesto_atencion.vistas.ConectarServidor;
import puesto_atencion.vistas.PanelOperador;

public class ControladorConexionPuesto {

    private final ConectarServidor vista;

    public ControladorConexionPuesto(ConectarServidor vista) {
        this.vista = vista;
    }

    public void iniciar() {
        vista.addConectarListener(e -> manejarConectar());
        vista.setVisible(true);
    }

    private void manejarConectar() {
        String ipPrimaria = vista.getIPServidor();
        if (!ValidadorConexion.isValidIPv4(ipPrimaria)) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                "Ingrese una IP de servidor valida (ej: 192.168.0.1).");
            return;
        }

        String textoPuertoPrimario = vista.getPuertoServidor();
        if (!ValidadorConexion.isValidPuerto(textoPuertoPrimario)) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                "Ingrese un puerto valido (0 - 65535).");
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

        try (Socket prueba = new Socket()) {
            prueba.connect(new InetSocketAddress(ipPrimaria, puertoPrimario),
                ConfigHA.SOCKET_CONNECT_TIMEOUT_MS);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                "No se pudo conectar al servidor primario en " + ipPrimaria + ":" + puertoPrimario
                    + ".\nVerifique IP y puerto del primario antes de continuar.",
                "Error de conexion", JOptionPane.ERROR_MESSAGE);
            return;
        }

        IGestorConexionServidor conexion = new GestorConexionRedundanteServidor(direccionPrimaria, direccionSecundaria);
        IHistorialLocal historial = new HistorialLocal();
        IGestorAtencion manejador = new GestorAtencion(conexion, historial);

        vista.setBotonHabilitado(false);
        new Thread(() -> {
            try {
                int idPuesto = manejador.conectar();
                SwingUtilities.invokeLater(() -> {
                    vista.cerrar();
                    abrirPanelOperador(manejador, conexion, idPuesto);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(vista.getFrame(),
                        "No se pudo conectar al Servidor Central en " + ipPrimaria + ":" + puertoPrimario
                            + ".\nVerifique que el servidor este activo e intente nuevamente.",
                        "Error de conexion", JOptionPane.ERROR_MESSAGE);
                    vista.setBotonHabilitado(true);
                });
            }
        }).start();
    }

    private void abrirPanelOperador(IGestorAtencion manejador, IGestorConexionServidor conexion, int idPuesto) {
        PanelOperador vistaPanel = new PanelOperador();
        new ControladorPanelOperador(vistaPanel, manejador, conexion, idPuesto).iniciar();
    }

    private Direccion obtenerDireccionSecundaria(String ipSecundaria, String textoPuertoSecundario,
            Direccion direccionPrimaria) {

        if (ipSecundaria.isEmpty() && !textoPuertoSecundario.isEmpty()) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                "Ingrese la IP secundaria o elimine el puerto secundario.");
            return null;
        }

        if (!ipSecundaria.isEmpty() && textoPuertoSecundario.isEmpty()) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                "Ingrese el puerto de la IP secundaria o elimine la IP secundaria.");
            return null;
        }

        if (!ValidadorConexion.isValidIPv4(ipSecundaria)) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                "IP secundaria invalida.");
            return null;
        }

        if (!ValidadorConexion.isValidPuerto(textoPuertoSecundario)) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                "Puerto secundario invalido (0 - 65535).");
            return null;
        }
        int puertoSecundario = Integer.parseInt(textoPuertoSecundario);

        Direccion direccionSecundaria = new Direccion(ipSecundaria, puertoSecundario);

        if (direccionSecundaria.equals(direccionPrimaria)) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                "La direccion secundaria debe ser distinta de la primaria.");
            return null;
        }

        return direccionSecundaria;
    }
}
