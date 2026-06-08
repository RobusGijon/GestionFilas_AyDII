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
    private JTextField textFieldIPSec;
    private JTextField textFieldPuertoSec;
    private JButton btnConectar;

    public ConectarIP() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBackground(Color.WHITE);
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setBounds(100, 100, 600, 360);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(0, 20));

        JLabel lblTitulo = new JLabel("Conexión pantalla");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 24));
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
        btnConectar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnConectar.setBackground(new Color(0, 107, 63));
        panelBoton.add(btnConectar);

        JPanel panelCampos = new JPanel();
        panelCampos.setForeground(SystemColor.desktop);
        panelCampos.setBackground(Color.WHITE);
        panelCampos.setLayout(new java.awt.GridLayout(2, 1, 0, 8));
        panelCentro.add(panelCampos, BorderLayout.NORTH);

        JPanel filaPrim = new JPanel();
        filaPrim.setBackground(Color.WHITE);
        panelCampos.add(filaPrim);

        JLabel lblIP = new JLabel("IP Servidor primario:");
        lblIP.setHorizontalAlignment(SwingConstants.CENTER);
        lblIP.setFont(new Font("SansSerif", Font.PLAIN, 18));
        lblIP.setBackground(Color.WHITE);
        filaPrim.add(lblIP);

        textFieldIP = new JTextField();
        textFieldIP.setFont(new Font("SansSerif", Font.PLAIN, 14));
        textFieldIP.setColumns(10);
        agregarPlaceholder(textFieldIP, "Ej: 192.168.0.1");
        filaPrim.add(textFieldIP);

        JLabel lblPuerto = new JLabel("   Puerto:");
        lblPuerto.setHorizontalAlignment(SwingConstants.CENTER);
        lblPuerto.setFont(new Font("SansSerif", Font.PLAIN, 18));
        lblPuerto.setBackground(Color.WHITE);
        filaPrim.add(lblPuerto);

        textFieldPuerto = new JTextField();
        textFieldPuerto.setFont(new Font("SansSerif", Font.PLAIN, 14));
        textFieldPuerto.setColumns(10);
        agregarPlaceholder(textFieldPuerto, "Ej: 5555");
        filaPrim.add(textFieldPuerto);

        JPanel filaSec = new JPanel();
        filaSec.setBackground(Color.WHITE);
        panelCampos.add(filaSec);

        JLabel lblIPSec = new JLabel("IP Servidor secundario:");
        lblIPSec.setHorizontalAlignment(SwingConstants.CENTER);
        lblIPSec.setFont(new Font("SansSerif", Font.PLAIN, 18));
        lblIPSec.setBackground(Color.WHITE);
        filaSec.add(lblIPSec);

        textFieldIPSec = new JTextField();
        textFieldIPSec.setFont(new Font("SansSerif", Font.PLAIN, 14));
        textFieldIPSec.setColumns(10);
        agregarPlaceholder(textFieldIPSec, "(opcional)");
        filaSec.add(textFieldIPSec);

        JLabel lblPuertoSec = new JLabel("   Puerto:");
        lblPuertoSec.setHorizontalAlignment(SwingConstants.CENTER);
        lblPuertoSec.setFont(new Font("SansSerif", Font.PLAIN, 18));
        lblPuertoSec.setBackground(Color.WHITE);
        filaSec.add(lblPuertoSec);

        textFieldPuertoSec = new JTextField();
        textFieldPuertoSec.setFont(new Font("SansSerif", Font.PLAIN, 14));
        textFieldPuertoSec.setColumns(10);
        agregarPlaceholder(textFieldPuertoSec, "(opcional)");
        filaSec.add(textFieldPuertoSec);
    }

    private void agregarPlaceholder(JTextField field, String placeholder) {
        field.setText(placeholder);
        field.setForeground(new Color(107, 107, 107));
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
                    field.setForeground(new Color(107, 107, 107));
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

    public String getIPSecundario() {
        String t = textFieldIPSec.getText().trim();
        if (t.equals("(opcional)")) return "";
        return t;
    }

    public String getPuertoSecundario() {
        String t = textFieldPuertoSec.getText().trim();
        if (t.equals("(opcional)")) return "";
        return t;
    }

    public void addConectarListener(ActionListener listener) {
        btnConectar.addActionListener(listener);
    }

    public JFrame getFrame() {
        return frame;
    }
}
