package puesto_atencion.vistas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class PersonalAtencion {

    private JFrame frame;
    private DefaultListModel<String> listModel;
    private JList<String> listFila;
    private JButton btnLlamar;
    private JLabel lblClienteLlamado;

    public PersonalAtencion() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setBackground(Color.WHITE);
        frame.setBounds(100, 100, 450, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel lblTitulo = new JLabel("Puesto de Atencion");
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitulo.setBackground(Color.WHITE);
        lblTitulo.setFont(new Font("Tahoma", Font.BOLD, 24));
        frame.getContentPane().add(lblTitulo, BorderLayout.NORTH);

        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new BorderLayout(0, 0));

        JLabel lblRegistros = new JLabel("Clientes en fila:");
        lblRegistros.setHorizontalAlignment(SwingConstants.CENTER);
        lblRegistros.setBackground(Color.WHITE);
        lblRegistros.setForeground(Color.BLACK);
        lblRegistros.setFont(new Font("Tahoma", Font.PLAIN, 16));
        panel.add(lblRegistros, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        listFila = new JList<>(listModel);
        listFila.setForeground(Color.GRAY);
        listFila.setBackground(Color.WHITE);
        listFila.setFont(new Font("Tahoma", Font.PLAIN, 16));
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        listFila.setCellRenderer(renderer);
        panel.add(listFila, BorderLayout.CENTER);

        JPanel panelInferior = new JPanel();
        panelInferior.setBackground(Color.WHITE);
        panel.add(panelInferior, BorderLayout.SOUTH);
        panelInferior.setLayout(new BorderLayout(0, 5));

        lblClienteLlamado = new JLabel("Cliente llamado: -");
        lblClienteLlamado.setHorizontalAlignment(SwingConstants.CENTER);
        lblClienteLlamado.setFont(new Font("Tahoma", Font.PLAIN, 16));
        panelInferior.add(lblClienteLlamado, BorderLayout.NORTH);

        JPanel panelBoton = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panelBoton.getLayout();
        flowLayout.setVgap(10);
        panelBoton.setBackground(Color.WHITE);
        panelInferior.add(panelBoton, BorderLayout.CENTER);

        btnLlamar = new JButton("Llamar Siguiente");
        btnLlamar.setOpaque(true);
        btnLlamar.setBorderPainted(false);
        btnLlamar.setForeground(Color.WHITE);
        btnLlamar.setBackground(new Color(4, 137, 97));
        btnLlamar.setFont(new Font("Tahoma", Font.BOLD, 18));
        panelBoton.add(btnLlamar);
    }

    public void mostrarClienteLlamado(String dni) {
        lblClienteLlamado.setText("Cliente llamado: " + dni);
    }

    public void agregarDNI(String dni) {
        listModel.addElement(dni);
    }

    public void removerPrimero() {
        if (!listModel.isEmpty()) {
            listModel.removeElementAt(0);
        }
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public void addLlamarListener(ActionListener listener) {
        btnLlamar.addActionListener(listener);
    }

    public JFrame getFrame() {
        return frame;
    }
}
