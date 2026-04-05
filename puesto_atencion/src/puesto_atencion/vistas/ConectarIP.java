package puesto_atencion.vistas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.SystemColor;
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
    private JTextField textFieldPuertoEscucha;
    private JTextField textFieldIPMonitor;
    private JTextField textFieldPuertoMonitor;
    private JButton btnConectar;

    public ConectarIP() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBackground(Color.WHITE);
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setBounds(100, 100, 500, 320);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(0, 10));

        JLabel lblTitulo = new JLabel("Conexion puesto de atencion");
        lblTitulo.setFont(new Font("Tahoma", Font.BOLD, 24));
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        frame.getContentPane().add(lblTitulo, BorderLayout.NORTH);

        JPanel panelCentro = new JPanel();
        panelCentro.setBackground(Color.WHITE);
        frame.getContentPane().add(panelCentro, BorderLayout.CENTER);
        panelCentro.setLayout(new java.awt.GridLayout(3, 1, 0, 5));

        JPanel filaPuertoEscucha = new JPanel();
        filaPuertoEscucha.setBackground(Color.WHITE);
        panelCentro.add(filaPuertoEscucha);

        JLabel lblPuertoEscucha = new JLabel("Puerto de escucha:");
        lblPuertoEscucha.setFont(new Font("Tahoma", Font.PLAIN, 16));
        filaPuertoEscucha.add(lblPuertoEscucha);

        textFieldPuertoEscucha = new JTextField();
        textFieldPuertoEscucha.setFont(new Font("Tahoma", Font.PLAIN, 14));
        textFieldPuertoEscucha.setColumns(8);
        agregarPlaceholder(textFieldPuertoEscucha, "EJ: 5555");
        filaPuertoEscucha.add(textFieldPuertoEscucha);

        JPanel filaPantalla = new JPanel();
        filaPantalla.setBackground(Color.WHITE);
        panelCentro.add(filaPantalla);

        JLabel lblIP = new JLabel("IP Pantalla:");
        lblIP.setFont(new Font("Tahoma", Font.PLAIN, 16));
        filaPantalla.add(lblIP);

        textFieldIPMonitor = new JTextField();
        textFieldIPMonitor.setFont(new Font("Tahoma", Font.PLAIN, 14));
        textFieldIPMonitor.setColumns(10);
        agregarPlaceholder(textFieldIPMonitor, "Ej: 192.168.0.1");
        filaPantalla.add(textFieldIPMonitor);

        JLabel lblHost = new JLabel("Puerto:");
        lblHost.setFont(new Font("Tahoma", Font.PLAIN, 16));
        filaPantalla.add(lblHost);

        textFieldPuertoMonitor = new JTextField();
        textFieldPuertoMonitor.setFont(new Font("Tahoma", Font.PLAIN, 14));
        textFieldPuertoMonitor.setColumns(6);
        agregarPlaceholder(textFieldPuertoMonitor, "EJ: 6666");
        filaPantalla.add(textFieldPuertoMonitor);

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

    public String getPuertoEscucha() {
        return textFieldPuertoEscucha.getText().trim();
    }

    public String getIPMonitor() {
        return textFieldIPMonitor.getText().trim();
    }

    public String getPuertoMonitor() {
        return textFieldPuertoMonitor.getText().trim();
    }

    public void addConectarListener(ActionListener listener) {
        btnConectar.addActionListener(listener);
    }

    public JFrame getFrame() {
        return frame;
    }
}
