package terminal_registro.vistas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class ConectarIP {

    private JFrame frame;
    private JTextField textFieldIPAtencion;
    private JTextField textFieldPuertoAtencion;
    private JTextField textFieldIPPantalla;
    private JTextField textFieldPuertoPantalla;
    private JButton btnConectar;

    public ConectarIP() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBackground(Color.WHITE);
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setBounds(100, 100, 550, 320);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(0, 10));

        JLabel lblTitulo = new JLabel("Conexion terminal de registro");
        lblTitulo.setFont(new Font("Tahoma", Font.BOLD, 24));
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        frame.getContentPane().add(lblTitulo, BorderLayout.NORTH);

        JPanel panelCentro = new JPanel();
        panelCentro.setBackground(Color.WHITE);
        frame.getContentPane().add(panelCentro, BorderLayout.CENTER);
        panelCentro.setLayout(new GridLayout(3, 1, 0, 5));

        // Fila 1: IP y Puerto del Puesto de Atencion
        JPanel filaAtencion = new JPanel();
        filaAtencion.setBackground(Color.WHITE);
        panelCentro.add(filaAtencion);

        JLabel lblIPAtencion = new JLabel("IP Atencion:");
        lblIPAtencion.setFont(new Font("Tahoma", Font.PLAIN, 16));
        filaAtencion.add(lblIPAtencion);

        textFieldIPAtencion = new JTextField();
        textFieldIPAtencion.setFont(new Font("Tahoma", Font.PLAIN, 14));
        textFieldIPAtencion.setColumns(10);
        agregarPlaceholder(textFieldIPAtencion, "Ej: 192.168.0.1");
        filaAtencion.add(textFieldIPAtencion);

        JLabel lblPuertoAtencion = new JLabel("Puerto:");
        lblPuertoAtencion.setFont(new Font("Tahoma", Font.PLAIN, 16));
        filaAtencion.add(lblPuertoAtencion);

        textFieldPuertoAtencion = new JTextField();
        textFieldPuertoAtencion.setFont(new Font("Tahoma", Font.PLAIN, 14));
        textFieldPuertoAtencion.setColumns(6);
        agregarPlaceholder(textFieldPuertoAtencion, "EJ: 5555");
        filaAtencion.add(textFieldPuertoAtencion);

        // Fila 2: IP y Puerto de la Pantalla
        JPanel filaPantalla = new JPanel();
        filaPantalla.setBackground(Color.WHITE);
        panelCentro.add(filaPantalla);

        JLabel lblIPPantalla = new JLabel("IP Pantalla:");
        lblIPPantalla.setFont(new Font("Tahoma", Font.PLAIN, 16));
        filaPantalla.add(lblIPPantalla);

        textFieldIPPantalla = new JTextField();
        textFieldIPPantalla.setFont(new Font("Tahoma", Font.PLAIN, 14));
        textFieldIPPantalla.setColumns(10);
        agregarPlaceholder(textFieldIPPantalla, "Ej: 192.168.0.1");
        filaPantalla.add(textFieldIPPantalla);

        JLabel lblPuertoPantalla = new JLabel("Puerto:");
        lblPuertoPantalla.setFont(new Font("Tahoma", Font.PLAIN, 16));
        filaPantalla.add(lblPuertoPantalla);

        textFieldPuertoPantalla = new JTextField();
        textFieldPuertoPantalla.setFont(new Font("Tahoma", Font.PLAIN, 14));
        textFieldPuertoPantalla.setColumns(6);
        agregarPlaceholder(textFieldPuertoPantalla, "EJ: 6666");
        filaPantalla.add(textFieldPuertoPantalla);

        // Fila 3: Boton
        JPanel filaBoton = new JPanel();
        FlowLayout flowLayout = (FlowLayout) filaBoton.getLayout();
        flowLayout.setVgap(10);
        filaBoton.setBackground(Color.WHITE);
        panelCentro.add(filaBoton);

        btnConectar = new JButton("CONECTAR");
        btnConectar.setOpaque(true);
        btnConectar.setBorderPainted(false);
        btnConectar.setForeground(Color.WHITE);
        btnConectar.setFont(new Font("Tahoma", Font.BOLD, 14));
        btnConectar.setBackground(new Color(4, 137, 97));
        filaBoton.add(btnConectar);
    }

    private void agregarPlaceholder(JTextField field, String placeholder) {
        field.setText(placeholder);
        field.setForeground(Color.GRAY);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(Color.GRAY);
                }
            }
        });
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public String getIPAtencion() {
        return textFieldIPAtencion.getText().trim();
    }

    public String getPuertoAtencion() {
        return textFieldPuertoAtencion.getText().trim();
    }

    public String getIPPantalla() {
        return textFieldIPPantalla.getText().trim();
    }

    public String getPuertoPantalla() {
        return textFieldPuertoPantalla.getText().trim();
    }

    public void addConectarListener(ActionListener listener) {
        btnConectar.addActionListener(listener);
    }

    public JFrame getFrame() {
        return frame;
    }
}
