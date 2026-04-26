package pantalla.vistas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class SalaEspera {

    private static final Color COLOR_ACTUAL = new Color(4, 137, 97);
    private static final int COL_PUESTO = 0;
    private static final int COL_DNI = 1;
    private static final int MAX_HISTORIAL = 5;
    private static final int PARPADEO_PERIODO_MS = 500;
    private static final int PARPADEO_TOGGLES = 20;

    private JFrame frame;

    private DefaultTableModel modeloActual;
    private JTable tablaActual;

    private DefaultTableModel modeloHistorial;
    private JTable tablaHistorial;

    private final LinkedHashMap<Integer, String> llamadosActivos = new LinkedHashMap<>();
    private final Map<Integer, Boolean> puestoOculto = new HashMap<>();
    private final Map<Integer, Timer> timersPorPuesto = new HashMap<>();

    public SalaEspera() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 900, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(Color.WHITE);
        frame.getContentPane().setLayout(new BorderLayout(0, 15));

        JLabel lblTitulo = new JLabel("Sala de Espera");
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Tahoma", Font.BOLD, 42));
        lblTitulo.setOpaque(true);
        lblTitulo.setBackground(Color.WHITE);
        frame.getContentPane().add(lblTitulo, BorderLayout.NORTH);

        JPanel panelPrincipal = new JPanel();
        panelPrincipal.setBackground(Color.WHITE);
        panelPrincipal.setLayout(new BorderLayout(0, 25));
        frame.getContentPane().add(panelPrincipal, BorderLayout.CENTER);

        JPanel panelTablas = new JPanel();
        panelTablas.setBackground(Color.WHITE);
        panelTablas.setLayout(new javax.swing.BoxLayout(panelTablas, javax.swing.BoxLayout.Y_AXIS));
        panelPrincipal.add(panelTablas, BorderLayout.CENTER);

        JLabel lblLlamadoActual = new JLabel("Atendiendo ahora");
        lblLlamadoActual.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        lblLlamadoActual.setHorizontalAlignment(SwingConstants.CENTER);
        lblLlamadoActual.setFont(new Font("Tahoma", Font.BOLD, 24));
        lblLlamadoActual.setForeground(Color.BLACK);
        panelTablas.add(lblLlamadoActual);

        panelTablas.add(javax.swing.Box.createRigidArea(new Dimension(0, 10)));

        modeloActual = new DefaultTableModel(new Object[] { "Puesto de atención", "DNI" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaActual = new JTable(modeloActual);
        configurarTabla(tablaActual, COLOR_ACTUAL, 26, 40, true);

        JScrollPane scrollActual = new JScrollPane(tablaActual);
        scrollActual.setPreferredSize(new Dimension(800, 180));
        panelTablas.add(scrollActual);

        panelTablas.add(javax.swing.Box.createRigidArea(new Dimension(0, 30)));

        JLabel lblHistorial = new JLabel("Historial de llamados");
        lblHistorial.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        lblHistorial.setHorizontalAlignment(SwingConstants.CENTER);
        lblHistorial.setFont(new Font("Tahoma", Font.BOLD, 24));
        lblHistorial.setForeground(Color.BLACK);
        panelTablas.add(lblHistorial);

        panelTablas.add(javax.swing.Box.createRigidArea(new Dimension(0, 10)));

        modeloHistorial = new DefaultTableModel(new Object[] { "Puesto de atención", "DNI" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaHistorial = new JTable(modeloHistorial);
        configurarTabla(tablaHistorial, Color.GRAY, 22, 34, false);

        JScrollPane scrollHistorial = new JScrollPane(tablaHistorial);
        scrollHistorial.setPreferredSize(new Dimension(800, 220));
        panelTablas.add(scrollHistorial);
    }

    private void configurarTabla(JTable tabla, Color colorTexto, int fontSize, int rowHeight, boolean esActual) {
        tabla.setBackground(Color.WHITE);
        tabla.setForeground(colorTexto);
        tabla.setFont(new Font("Tahoma", Font.BOLD, fontSize));
        tabla.setRowHeight(rowHeight);
        tabla.setShowGrid(true);
        tabla.setGridColor(new Color(220, 220, 220));
        tabla.setRowSelectionAllowed(false);
        tabla.setCellSelectionEnabled(false);
        tabla.setFocusable(false);

        JTableHeader header = tabla.getTableHeader();
        header.setFont(new Font("Tahoma", Font.BOLD, 24));
        header.setBackground(Color.WHITE);
        header.setForeground(Color.BLACK);
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer cellRenderer = esActual
                ? new RendererActual(colorTexto)
                : crearRendererCentrado(colorTexto);

        for (int i = 0; i < tabla.getColumnModel().getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) header.getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private DefaultTableCellRenderer crearRendererCentrado(Color colorTexto) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.CENTER);
        r.setVerticalAlignment(SwingConstants.CENTER);
        r.setForeground(colorTexto);
        return r;
    }

    private class RendererActual extends DefaultTableCellRenderer {
        private final Color colorNormal;

        RendererActual(Color colorNormal) {
            this.colorNormal = colorNormal;
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, false, false, row, column);
            c.setBackground(Color.WHITE);
            Integer idPuesto = idPuestoDeFila(row);
            boolean oculto = column == COL_DNI
                    && idPuesto != null
                    && Boolean.TRUE.equals(puestoOculto.get(idPuesto));
            c.setForeground(oculto ? Color.WHITE : colorNormal);
            return c;
        }
    }

    private Integer idPuestoDeFila(int row) {
        int i = 0;
        for (Integer id : llamadosActivos.keySet()) {
            if (i == row)
                return id;
            i++;
        }
        return null;
    }

    public void registrarLlamado(String dni, int idPuesto) {
        if (llamadosActivos.containsKey(idPuesto)) {
            String dniAnterior = llamadosActivos.get(idPuesto);
            agregarAHistorial(dniAnterior, idPuesto);
            detenerParpadeo(idPuesto);
            llamadosActivos.remove(idPuesto);
        }
        if (dni != null && !dni.isEmpty()) {
            llamadosActivos.put(idPuesto, dni);
        }
        reconstruirTablaActual();
    }

    public void quitarLlamado(int idPuesto) {
        detenerParpadeo(idPuesto);
        if (llamadosActivos.remove(idPuesto) != null) {
            reconstruirTablaActual();
        }
    }

    public void destacarRenotificacion(int idPuesto) {
        if (!llamadosActivos.containsKey(idPuesto)) {
            return;
        }
        detenerParpadeo(idPuesto);
        final int[] toggles = { 0 };
        Timer t = new Timer(PARPADEO_PERIODO_MS, e -> {
            boolean actual = Boolean.TRUE.equals(puestoOculto.get(idPuesto));
            puestoOculto.put(idPuesto, !actual);
            tablaActual.repaint();
            toggles[0]++;
            if (toggles[0] >= PARPADEO_TOGGLES) {
                detenerParpadeo(idPuesto);
            }
        });
        timersPorPuesto.put(idPuesto, t);
        t.start();
    }

    private void detenerParpadeo(int idPuesto) {
        Timer t = timersPorPuesto.remove(idPuesto);
        if (t != null && t.isRunning()) {
            t.stop();
        }
        if (puestoOculto.remove(idPuesto) != null) {
            tablaActual.repaint();
        }
    }

    private void reconstruirTablaActual() {
        modeloActual.setRowCount(0);
        for (Map.Entry<Integer, String> e : llamadosActivos.entrySet()) {
            Object[] fila = new Object[2];
            fila[COL_PUESTO] = "Puesto " + e.getKey();
            fila[COL_DNI] = e.getValue();
            modeloActual.addRow(fila);
        }
    }

    private void agregarAHistorial(String dni, int idPuesto) {
        Object[] fila = new Object[2];
        fila[COL_PUESTO] = "Puesto " + idPuesto;
        fila[COL_DNI] = dni;
        modeloHistorial.insertRow(0, fila);
        while (modeloHistorial.getRowCount() > MAX_HISTORIAL) {
            modeloHistorial.removeRow(modeloHistorial.getRowCount() - 1);
        }
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public JFrame getFrame() {
        return frame;
    }
}
