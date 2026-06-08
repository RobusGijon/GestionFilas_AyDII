package terminal_registro.controlador;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.swing.JOptionPane;

import terminal_registro.conexion.GestorConexionRedundanteRegistro;
import terminal_registro.disponibilidad.ConfigHA;
import terminal_registro.interfaces.IConexionRegistro;
import terminal_registro.interfaces.IEstrategiaEncriptacion;
import terminal_registro.protocolo.mensajes.Direccion;
import terminal_registro.util.Validador;
import terminal_registro.vistas.ConectarIP;
import terminal_registro.vistas.TerminalAutoservicio;

public class ControladorConexionTerminal {

    private final ConectarIP vista;

    private final IEstrategiaEncriptacion encriptacion;

    public ControladorConexionTerminal(ConectarIP vista, IEstrategiaEncriptacion encriptacion) {
        this.vista = vista;
        this.encriptacion = encriptacion;
    }

    public void iniciar() {
        vista.addConectarListener(e -> manejarConectar());
        vista.setVisible(true);
    }

    private void manejarConectar() {
        String ipPrimaria = vista.getIPServidor();
        if (!Validador.isValidIPv4(ipPrimaria)) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                    "Ingrese una IP de Servidor valida.");
            return;
        }

        String textoPuertoPrimario = vista.getPuertoServidor();
        if (!Validador.isValidPuerto(textoPuertoPrimario)) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                    "Ingrese un puerto de Servidor valido (0 - 65535).");
            return;
        }

        Direccion direccionPrimaria = new Direccion(ipPrimaria, Integer.parseInt(textoPuertoPrimario));

        String ipSecundaria = vista.getIPSecundario();
        String textoPuertoSecundario = vista.getPuertoSecundario();

        Direccion direccionSecundaria = null;
        if (!ipSecundaria.isEmpty() || !textoPuertoSecundario.isEmpty()) {
            direccionSecundaria = obtenerDireccionSecundaria(ipSecundaria, textoPuertoSecundario, direccionPrimaria);
            if (direccionSecundaria == null) {
                return;
            }
        }

        IConexionRegistro gestor = new GestorConexionRedundanteRegistro(direccionPrimaria, direccionSecundaria,
                encriptacion);

        if (!gestor.elServerEstaRespondiendo()) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                    "No se pudo conectar al servidor primario en " + direccionPrimaria.getIp() + ":"
                            + direccionPrimaria.getPuerto()
                            + ".\nVerifique IP y puerto del primario antes de continuar.",
                    "Error de conexion", JOptionPane.ERROR_MESSAGE);
            return;
        }

        TerminalAutoservicio vistaTerminal = new TerminalAutoservicio();
        ControladorTerminal controladorTerminal = new ControladorTerminal(vistaTerminal, gestor);

        vista.setVisible(false);
        controladorTerminal.iniciar();
    }

    private Direccion obtenerDireccionSecundaria(String ipSecundaria, String textoPuertoSecundario,
            Direccion direccionPrimaria) {

        if (ipSecundaria.isEmpty() && !textoPuertoSecundario.isEmpty()) {
            JOptionPane.showMessageDialog(vista.getFrame(), "Ingrese la IP secundaria o elimine el puerto secundario");
            return null;
        }

        if (!ipSecundaria.isEmpty() && textoPuertoSecundario.isEmpty()) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                    "Ingrese el puerto de la IP secundaria o elimine la IP secundaria");
            return null;
        }

        if (!Validador.isValidIPv4(ipSecundaria)) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                    "Ingrese una IP secundaria valida.");
            return null;
        }

        if (!Validador.isValidPuerto(textoPuertoSecundario)) {
            JOptionPane.showMessageDialog(vista.getFrame(),
                    "Ingrese un puerto secundario valido (0 - 65535).");
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
