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
    private JTextField textFieldIPServidor;
    private JTextField textFieldPuertoServidor;
    private JTextField textFieldIPSecundario;
    private JTextField textFieldPuertoSecundario;
    private JButton btnConectar;

    public ConectarIP() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBackground(Color.WHITE);
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setBounds(100, 100, 580, 320);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(0, 10));

        JLabel lblTitulo = new JLabel("Conexión terminal de registro");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        frame.getContentPane().add(lblTitulo, BorderLayout.NORTH);

        JPanel panelCentro = new JPanel();
        panelCentro.setBackground(Color.WHITE);
        frame.getContentPane().add(panelCentro, BorderLayout.CENTER);
        panelCentro.setLayout(new GridLayout(3, 1, 0, 5));

        JPanel filaServidor = new JPanel();
        filaServidor.setBackground(Color.WHITE);
        panelCentro.add(filaServidor);

        JLabel lblIPServidor = new JLabel("IP Servidor primario:");
        lblIPServidor.setFont(new Font("SansSerif", Font.PLAIN, 16));
        filaServidor.add(lblIPServidor);

        textFieldIPServidor = new JTextField();
        textFieldIPServidor.setFont(new Font("SansSerif", Font.PLAIN, 14));
        textFieldIPServidor.setColumns(10);
        agregarPlaceholder(textFieldIPServidor, "Ej: 192.168.0.1");
        filaServidor.add(textFieldIPServidor);

        JLabel lblPuertoServidor = new JLabel("Puerto:");
        lblPuertoServidor.setFont(new Font("SansSerif", Font.PLAIN, 16));
        filaServidor.add(lblPuertoServidor);

        textFieldPuertoServidor = new JTextField();
        textFieldPuertoServidor.setFont(new Font("SansSerif", Font.PLAIN, 14));
        textFieldPuertoServidor.setColumns(6);
        agregarPlaceholder(textFieldPuertoServidor, "Ej: 8080");
        filaServidor.add(textFieldPuertoServidor);

        JPanel filaSecundario = new JPanel();
        filaSecundario.setBackground(Color.WHITE);
        panelCentro.add(filaSecundario);

        JLabel lblIPSec = new JLabel("IP Servidor secundario:");
        lblIPSec.setFont(new Font("SansSerif", Font.PLAIN, 16));
        filaSecundario.add(lblIPSec);

        textFieldIPSecundario = new JTextField();
        textFieldIPSecundario.setFont(new Font("SansSerif", Font.PLAIN, 14));
        textFieldIPSecundario.setColumns(10);
        agregarPlaceholder(textFieldIPSecundario, "(opcional)");
        filaSecundario.add(textFieldIPSecundario);

        JLabel lblPuertoSec = new JLabel("Puerto:");
        lblPuertoSec.setFont(new Font("SansSerif", Font.PLAIN, 16));
        filaSecundario.add(lblPuertoSec);

        textFieldPuertoSecundario = new JTextField();
        textFieldPuertoSecundario.setFont(new Font("SansSerif", Font.PLAIN, 14));
        textFieldPuertoSecundario.setColumns(6);
        agregarPlaceholder(textFieldPuertoSecundario, "(opcional)");
        filaSecundario.add(textFieldPuertoSecundario);

        JPanel filaBoton = new JPanel();
        FlowLayout flowLayout = (FlowLayout) filaBoton.getLayout();
        flowLayout.setVgap(10);
        filaBoton.setBackground(Color.WHITE);
        panelCentro.add(filaBoton);

        btnConectar = new JButton("CONECTAR");
        btnConectar.setOpaque(true);
        btnConectar.setBorderPainted(false);
        btnConectar.setForeground(Color.WHITE);
        btnConectar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnConectar.setBackground(new Color(0, 107, 63));
        filaBoton.add(btnConectar);
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

    public String getIPServidor() {
        return textFieldIPServidor.getText().trim();
    }

    public String getPuertoServidor() {
        return textFieldPuertoServidor.getText().trim();
    }

    public String getIPSecundario() {
        String t = textFieldIPSecundario.getText().trim();
        if (t.equals("(opcional)")) return "";
        return t;
    }

    public String getPuertoSecundario() {
        String t = textFieldPuertoSecundario.getText().trim();
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
