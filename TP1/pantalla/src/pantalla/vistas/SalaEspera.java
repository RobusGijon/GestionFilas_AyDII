package pantalla.vistas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class SalaEspera {

    private JFrame frame;
    private JLabel lblTurnoActual;
    private DefaultListModel<String> listModel;
    private JList<String> listAnteriores;

    public SalaEspera() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setBackground(Color.WHITE);
        frame.setBounds(100, 100, 450, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel lblTitulo = new JLabel("Sala de Espera");
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitulo.setBackground(Color.WHITE);
        lblTitulo.setFont(new Font("Tahoma", Font.BOLD, 24));
        frame.getContentPane().add(lblTitulo, BorderLayout.NORTH);

        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new BorderLayout(0, 0));

        JLabel lblTituloAnteriores = new JLabel("Turnos anteriores:");
        lblTituloAnteriores.setHorizontalAlignment(SwingConstants.CENTER);
        lblTituloAnteriores.setBackground(Color.WHITE);
        lblTituloAnteriores.setForeground(Color.BLACK);
        lblTituloAnteriores.setFont(new Font("Tahoma", Font.PLAIN, 16));
        panel.add(lblTituloAnteriores, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        listAnteriores = new JList<>(listModel);
        listAnteriores.setForeground(Color.GRAY);
        listAnteriores.setBackground(Color.WHITE);
        listAnteriores.setFont(new Font("Tahoma", Font.PLAIN, 16));
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        listAnteriores.setCellRenderer(renderer);
        panel.add(listAnteriores, BorderLayout.CENTER);

        JPanel panelActual = new JPanel();
        panelActual.setBackground(Color.WHITE);
        panel.add(panelActual, BorderLayout.SOUTH);
        panelActual.setLayout(new BorderLayout(0, 5));

        JLabel lblTextoTurno = new JLabel("Turno actual:");
        lblTextoTurno.setHorizontalAlignment(SwingConstants.CENTER);
        lblTextoTurno.setFont(new Font("Tahoma", Font.PLAIN, 16));
        panelActual.add(lblTextoTurno, BorderLayout.NORTH);

        lblTurnoActual = new JLabel("-");
        lblTurnoActual.setForeground(new Color(4, 137, 97));
        lblTurnoActual.setHorizontalAlignment(SwingConstants.CENTER);
        lblTurnoActual.setFont(new Font("Tahoma", Font.BOLD, 21));
        panelActual.add(lblTurnoActual, BorderLayout.SOUTH);
    }

    public void actualizarPantalla(String dni) {
        lblTurnoActual.setText(dni);
    }

    public void actualizarClientesAnteriores(String dniAnterior) {
        if (dniAnterior != null && !dniAnterior.equals("-")) {
            listModel.add(0, dniAnterior);
            if (listModel.getSize() > 4) {
                listModel.remove(listModel.getSize() - 1);
            }
        }
    }

    public String getTurnoActual() {
        return lblTurnoActual.getText();
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public JFrame getFrame() {
        return frame;
    }
}
