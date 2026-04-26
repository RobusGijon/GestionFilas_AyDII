package puesto_atencion.vistas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import puesto_atencion.datos.ClienteAtendido;

public class PanelOperador {

    public enum EstadoBotones {
        SIN_CLIENTE,
        CON_CLIENTE_EN_COOLDOWN,
        CON_CLIENTE_PUEDE_RELLAMAR,
        TERCER_INTENTO_EN_COOLDOWN,
        TERCER_INTENTO_VENCIDO
    }

    private static final Color VERDE = new Color(4, 137, 97);
    private static final Color ROJO = new Color(192, 57, 43);
    private static final Color GRIS = new Color(180, 180, 180);
    private static final Font FONT_FILA_DESTACADA = new Font("Tahoma", Font.BOLD, 16);
    private static final Font FONT_FILA_NORMAL = new Font("Tahoma", Font.PLAIN, 16);

    private JFrame frame;
    private JLabel lblTituloPuesto;
    private JLabel lblClienteActual;
    private DefaultTableModel modelHistorial;
    private JTable tablaHistorial;
    private JButton btnLlamarSiguiente;
    private JButton btnRellamar;
    private JButton btnEliminar;

    public PanelOperador() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setBounds(100, 100, 700, 480);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(0, 5));

        lblTituloPuesto = new JLabel("PUESTO Nº -");
        lblTituloPuesto.setHorizontalAlignment(SwingConstants.CENTER);
        lblTituloPuesto.setFont(new Font("Tahoma", Font.BOLD, 26));
        lblTituloPuesto.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        frame.getContentPane().add(lblTituloPuesto, BorderLayout.NORTH);

        JPanel panelCentro = new JPanel(new BorderLayout(0, 5));
        panelCentro.setBackground(Color.WHITE);
        frame.getContentPane().add(panelCentro, BorderLayout.CENTER);

        JLabel lblHistorial = new JLabel("Historial de clientes atendidos del puesto");
        lblHistorial.setHorizontalAlignment(SwingConstants.CENTER);
        lblHistorial.setFont(new Font("Tahoma", Font.BOLD, 16));
        panelCentro.add(lblHistorial, BorderLayout.NORTH);

        modelHistorial = new DefaultTableModel(new Object[] { "Nº de atendido", "Cliente (DNI)" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaHistorial = new JTable(modelHistorial);
        configurarTablaHistorial(tablaHistorial);
        JScrollPane scroll = new JScrollPane(tablaHistorial);
        scroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scroll.getViewport().setBackground(Color.WHITE);
        panelCentro.add(scroll, BorderLayout.CENTER);

        JPanel panelInferior = new JPanel(new BorderLayout(0, 8));
        panelInferior.setBackground(Color.WHITE);
        panelInferior.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        frame.getContentPane().add(panelInferior, BorderLayout.SOUTH);

        lblClienteActual = new JLabel("No hay clientes pendientes");
        lblClienteActual.setHorizontalAlignment(SwingConstants.CENTER);
        lblClienteActual.setFont(new Font("Tahoma", Font.BOLD, 18));
        panelInferior.add(lblClienteActual, BorderLayout.NORTH);

        JPanel panelBotones = new JPanel();
        FlowLayout fl = (FlowLayout) panelBotones.getLayout();
        fl.setHgap(15);
        panelBotones.setBackground(Color.WHITE);
        panelInferior.add(panelBotones, BorderLayout.CENTER);

        btnLlamarSiguiente = crearBoton("LLAMAR SIGUIENTE", VERDE);
        btnRellamar = crearBoton("Rellamar", GRIS);
        btnEliminar = crearBoton("ELIMINAR CLIENTE", GRIS);

        panelBotones.add(btnLlamarSiguiente);
        panelBotones.add(btnRellamar);
        panelBotones.add(btnEliminar);

        setEstadoBotones(EstadoBotones.SIN_CLIENTE);
    }

    private void configurarTablaHistorial(JTable tabla) {
        tabla.setBackground(Color.WHITE);
        tabla.setForeground(Color.DARK_GRAY);
        tabla.setFont(new Font("Tahoma", Font.PLAIN, 16));
        tabla.setRowHeight(28);
        tabla.setShowGrid(true);
        tabla.setGridColor(new Color(220, 220, 220));
        tabla.setRowSelectionAllowed(false);
        tabla.setCellSelectionEnabled(false);
        tabla.setFocusable(false);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, false, false, row, column);
                c.setForeground(VERDE);
                c.setFont(FONT_FILA_DESTACADA);
                c.setBackground(Color.WHITE);
                return c;
            }
        };
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        renderer.setVerticalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < tabla.getColumnModel().getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        JTableHeader header = tabla.getTableHeader();
        header.setFont(new Font("Tahoma", Font.BOLD, 16));
        header.setBackground(Color.WHITE);
        header.setForeground(Color.BLACK);
        header.setReorderingAllowed(false);
        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) header.getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private JButton crearBoton(String texto, Color fondo) {
        JButton b = new JButton(texto);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(fondo);
        b.setFont(new Font("Tahoma", Font.BOLD, 16));
        b.setFocusPainted(false);
        return b;
    }

    public void setIdPuesto(int id) {
        lblTituloPuesto.setText("PUESTO Nº " + id);
    }

    public void mostrarClienteActual(String dni, int intento) {
        lblClienteActual.setText("Cliente actual: " + dni + "   Intento " + intento + "/3");
    }

    public void mostrarFilaVacia() {
        lblClienteActual.setText("No hay clientes pendientes");
    }

    public void actualizarHistorial(List<ClienteAtendido> historial) {
        modelHistorial.setRowCount(0);
        int total = historial.size();
        for (int i = 0; i < total; i++) {
            ClienteAtendido c = historial.get(i);
            modelHistorial.addRow(new Object[] { String.valueOf(total - i), c.getDni() });
        }
    }

    public void setTextoRellamar(String texto) {
        btnRellamar.setText(texto);
    }

    public void setEstadoBotones(EstadoBotones estado) {
        btnLlamarSiguiente.setEnabled(true);
        btnLlamarSiguiente.setBackground(VERDE);

        switch (estado) {
            case SIN_CLIENTE:
                btnRellamar.setEnabled(false);
                btnRellamar.setBackground(GRIS);
                btnRellamar.setText("Rellamar");
                btnEliminar.setEnabled(false);
                btnEliminar.setBackground(GRIS);
                break;
            case CON_CLIENTE_EN_COOLDOWN:
                btnRellamar.setEnabled(false);
                btnRellamar.setBackground(GRIS);
                btnEliminar.setEnabled(false);
                btnEliminar.setBackground(GRIS);
                break;
            case CON_CLIENTE_PUEDE_RELLAMAR:
                btnRellamar.setEnabled(true);
                btnRellamar.setBackground(VERDE);
                btnRellamar.setText("Rellamar");
                btnEliminar.setEnabled(false);
                btnEliminar.setBackground(GRIS);
                break;
            case TERCER_INTENTO_EN_COOLDOWN:
                btnRellamar.setEnabled(false);
                btnRellamar.setBackground(GRIS);
                btnEliminar.setEnabled(false);
                btnEliminar.setBackground(GRIS);
                break;
            case TERCER_INTENTO_VENCIDO:
                btnRellamar.setEnabled(false);
                btnRellamar.setBackground(GRIS);
                btnRellamar.setText("Rellamar");
                btnEliminar.setEnabled(true);
                btnEliminar.setBackground(ROJO);
                break;
        }
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public JFrame getFrame() {
        return frame;
    }

    public void addLlamarSiguienteListener(ActionListener l) {
        btnLlamarSiguiente.addActionListener(l);
    }

    public void addRellamarListener(ActionListener l) {
        btnRellamar.addActionListener(l);
    }

    public void addEliminarListener(ActionListener l) {
        btnEliminar.addActionListener(l);
    }
}
