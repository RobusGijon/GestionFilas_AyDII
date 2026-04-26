package terminal_registro.vistas;

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

public class TerminalAutoservicio {

    private JFrame frame;
    private JTextField txtDNI;
    private JButton btnEnviar;

    public TerminalAutoservicio() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBackground(Color.WHITE);
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setBounds(100, 100, 450, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(0, 20));

        JLabel lblTitulo = new JLabel("Terminal de registro");
        lblTitulo.setFont(new Font("Tahoma", Font.BOLD, 24));
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        frame.getContentPane().add(lblTitulo, BorderLayout.NORTH);

        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new BorderLayout(0, 0));

        JLabel lblIngrese = new JLabel("Ingrese su DNI:");
        lblIngrese.setBackground(Color.WHITE);
        lblIngrese.setFont(new Font("Tahoma", Font.PLAIN, 18));
        lblIngrese.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblIngrese, BorderLayout.NORTH);

        JPanel panelCentro = new JPanel();
        panel.add(panelCentro, BorderLayout.CENTER);
        panelCentro.setLayout(new BorderLayout(0, 0));

        JPanel panelInput = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panelInput.getLayout();
        flowLayout.setVgap(20);
        flowLayout.setHgap(10);
        panelInput.setBackground(Color.WHITE);
        panelInput.setForeground(SystemColor.desktop);
        panelCentro.add(panelInput, BorderLayout.CENTER);

        txtDNI = new JTextField();
        txtDNI.setFont(new Font("Tahoma", Font.PLAIN, 14));
        txtDNI.setColumns(10);
        agregarPlaceholder(txtDNI, "Ej: 123456789");
        panelInput.add(txtDNI);

        btnEnviar = new JButton("ENVIAR");
        btnEnviar.setOpaque(true);
        btnEnviar.setBorderPainted(false);
        btnEnviar.setForeground(Color.WHITE);
        btnEnviar.setBackground(new Color(4, 137, 97));
        btnEnviar.setFont(new Font("Tahoma", Font.BOLD, 14));
        panelInput.add(btnEnviar);
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

    public String getDNI() {
        return txtDNI.getText().trim();
    }

    public void limpiarDNI() {
        txtDNI.setText("");
        txtDNI.requestFocus();
    }

    public void bloquearEnvio() {
        btnEnviar.setEnabled(false);
        txtDNI.setEnabled(false);
    }

    public void addEnviarListener(ActionListener listener) {
        btnEnviar.addActionListener(listener);
    }

    public JFrame getFrame() {
        return frame;
    }
}
