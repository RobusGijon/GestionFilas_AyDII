package pantalla.vistas;

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
    private JTextField textFieldIP;
    private JTextField textFieldPuerto;
    private JButton btnConectar;

    public ConectarIP() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBackground(Color.WHITE);
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setBounds(100, 100, 550, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(0, 20));

        JLabel lblTitulo = new JLabel("Conexion pantalla");
        lblTitulo.setFont(new Font("Tahoma", Font.BOLD, 24));
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        frame.getContentPane().add(lblTitulo, BorderLayout.NORTH);

        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new BorderLayout(0, 0));

        JPanel panelCentro = new JPanel();
        panel.add(panelCentro, BorderLayout.CENTER);
        panelCentro.setLayout(new BorderLayout(0, 0));

        JPanel panelBoton = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panelBoton.getLayout();
        flowLayout.setVgap(20);
        flowLayout.setHgap(10);
        panelBoton.setBackground(Color.WHITE);
        panelBoton.setForeground(SystemColor.desktop);
        panelCentro.add(panelBoton, BorderLayout.CENTER);

        btnConectar = new JButton("CONECTAR");
        btnConectar.setOpaque(true);
        btnConectar.setBorderPainted(false);
        btnConectar.setForeground(Color.WHITE);
        btnConectar.setFont(new Font("Tahoma", Font.BOLD, 14));
        btnConectar.setBackground(new Color(4, 137, 97));
        panelBoton.add(btnConectar);

        JPanel panelCampos = new JPanel();
        panelCampos.setForeground(SystemColor.desktop);
        panelCampos.setBackground(Color.WHITE);
        panelCentro.add(panelCampos, BorderLayout.NORTH);

        JLabel lblIP = new JLabel("IP:");
        lblIP.setHorizontalAlignment(SwingConstants.CENTER);
        lblIP.setFont(new Font("Tahoma", Font.PLAIN, 18));
        lblIP.setBackground(Color.WHITE);
        panelCampos.add(lblIP);

        textFieldIP = new JTextField();
        textFieldIP.setFont(new Font("Tahoma", Font.PLAIN, 14));
        textFieldIP.setColumns(10);
        agregarPlaceholder(textFieldIP, "Ej: 192.168.0.1");
        panelCampos.add(textFieldIP);

        JLabel lblHost = new JLabel("   Puerto de escucha:");
        lblHost.setHorizontalAlignment(SwingConstants.CENTER);
        lblHost.setFont(new Font("Tahoma", Font.PLAIN, 18));
        lblHost.setBackground(Color.WHITE);
        panelCampos.add(lblHost);

        textFieldPuerto = new JTextField();
        textFieldPuerto.setFont(new Font("Tahoma", Font.PLAIN, 14));
        textFieldPuerto.setColumns(10);
        agregarPlaceholder(textFieldPuerto, "EJ: 5555");
        panelCampos.add(textFieldPuerto);

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

    public String getIP() {
        return textFieldIP.getText().trim();
    }

    public String getPuerto() {
        return textFieldPuerto.getText().trim();
    }

    public void addConectarListener(ActionListener listener) {
        btnConectar.addActionListener(listener);
    }

    public JFrame getFrame() {
        return frame;
    }
}
