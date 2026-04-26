package puesto_atencion.vistas;

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

public class ConectarServidor {

    private JFrame frame;
    private JTextField textFieldIPServidor;
    private JTextField textFieldPuertoServidor;
    private JButton btnConectar;

    public ConectarServidor() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBackground(Color.WHITE);
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setBounds(100, 100, 500, 260);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(0, 10));

        JLabel lblTitulo = new JLabel("Conexión puesto de atencion");
        lblTitulo.setFont(new Font("Tahoma", Font.BOLD, 22));
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        frame.getContentPane().add(lblTitulo, BorderLayout.NORTH);

        JPanel panelCentro = new JPanel();
        panelCentro.setBackground(Color.WHITE);
        frame.getContentPane().add(panelCentro, BorderLayout.CENTER);
        panelCentro.setLayout(new GridLayout(2, 1, 0, 5));

        JPanel filaServidor = new JPanel();
        filaServidor.setBackground(Color.WHITE);
        panelCentro.add(filaServidor);

        JLabel lblIP = new JLabel("IP Servidor:");
        lblIP.setFont(new Font("Tahoma", Font.PLAIN, 16));
        filaServidor.add(lblIP);

        textFieldIPServidor = new JTextField();
        textFieldIPServidor.setFont(new Font("Tahoma", Font.PLAIN, 14));
        textFieldIPServidor.setColumns(12);
        agregarPlaceholder(textFieldIPServidor, "Ej: 192.168.0.1");
        filaServidor.add(textFieldIPServidor);

        JLabel lblPuerto = new JLabel("Puerto:");
        lblPuerto.setFont(new Font("Tahoma", Font.PLAIN, 16));
        filaServidor.add(lblPuerto);

        textFieldPuertoServidor = new JTextField();
        textFieldPuertoServidor.setFont(new Font("Tahoma", Font.PLAIN, 14));
        textFieldPuertoServidor.setColumns(6);
        agregarPlaceholder(textFieldPuertoServidor, "EJ: 5000");
        filaServidor.add(textFieldPuertoServidor);

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

    public String getIPServidor() {
        return textFieldIPServidor.getText().trim();
    }

    public String getPuertoServidor() {
        return textFieldPuertoServidor.getText().trim();
    }

    public void setBotonHabilitado(boolean habilitado) {
        btnConectar.setEnabled(habilitado);
    }

    public void addConectarListener(ActionListener listener) {
        btnConectar.addActionListener(listener);
    }

    public JFrame getFrame() {
        return frame;
    }

    public void cerrar() {
        frame.dispose();
    }
}
