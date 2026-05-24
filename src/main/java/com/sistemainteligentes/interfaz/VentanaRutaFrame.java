package com.sistemainteligentes.interfaz;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/**
 * Swing window that shows the recommended tourist route that the
 * AgenteRecomendador sends to the AgenteInterfaz.
 *
 * It was built following the JADE slides example (using Swing inside an
 * agent) and reusing the same approach as
 * {@code usuario.InterfazPreferenciasFrame}: a non editable JTextArea
 * inside a JScrollPane, with methods invoked from the event dispatch
 * thread (EDT) by the agent.
 *
 * Each route that arrives is appended with its own header (time, sender,
 * session id) so that during the defense the message flow between agents
 * is clearly visible.
 */
public class VentanaRutaFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String SEPARADOR =
        "============================================================";

    private final JTextArea area = new JTextArea(22, 64);
    private final JLabel estado = new JLabel("Esperando ruta del recomendador...");
    private int rutasRecibidas = 0;

    public VentanaRutaFrame(String nombreAgente) {
        super("Ruta turistica recomendada (AgenteInterfaz " + nombreAgente + ")");
        construir();
        setSize(720, 540);
        setLocationRelativeTo(null);
        // We do not kill the JVM when closing: we only hide the window, so the
        // agent stays alive in the JADE platform.
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }

    private void construir() {
        setLayout(new BorderLayout());

        add(crearCabecera(), BorderLayout.NORTH);

        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setMargin(new java.awt.Insets(10, 10, 10, 10));
        area.setText("Aun no ha llegado ninguna ruta.\n"
            + "Cuando el usuario envie sus preferencias y el recomendador\n"
            + "calcule la ruta, aparecera aqui automaticamente.\n");
        add(new JScrollPane(area), BorderLayout.CENTER);

        add(crearBarraInferior(), BorderLayout.SOUTH);
    }

    private JPanel crearCabecera() {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBackground(new Color(33, 70, 110));
        cabecera.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel titulo = new JLabel("Ruta turistica recomendada");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));

        JLabel subtitulo = new JLabel(
            "Lugares, hoteles, eventos, clima y costes aproximados");
        subtitulo.setForeground(new Color(200, 215, 235));
        subtitulo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        cabecera.add(titulo, BorderLayout.NORTH);
        cabecera.add(subtitulo, BorderLayout.SOUTH);
        return cabecera;
    }

    private JPanel crearBarraInferior() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        estado.setHorizontalAlignment(SwingConstants.LEFT);

        JButton limpiar = new JButton("Limpiar");
        limpiar.addActionListener(e -> limpiar());

        barra.add(estado, BorderLayout.CENTER);
        barra.add(limpiar, BorderLayout.EAST);
        return barra;
    }

    /**
     * Appends a received route to the window. It is always called from the EDT
     * (the agent uses SwingUtilities.invokeLater).
     *
     * @param contenido      route already formatted as text
     * @param emisor         name of the agent that sent it
     * @param conversationId session id
     */
    public void mostrarRuta(String contenido, String emisor, String conversationId) {
        if (rutasRecibidas == 0) {
            area.setText(""); // clear the initial message when the first route arrives
        }
        rutasRecibidas++;

        StringBuilder bloque = new StringBuilder();
        bloque.append(SEPARADOR).append('\n');
        bloque.append("Ruta #").append(rutasRecibidas)
            .append("  |  ").append(LocalTime.now().format(HORA))
            .append("  |  de: ").append(emisor != null ? emisor : "desconocido");
        if (conversationId != null) {
            bloque.append("  |  sesion: ").append(conversationId);
        }
        bloque.append('\n');
        bloque.append(SEPARADOR).append('\n');
        bloque.append(contenido != null ? contenido : "(ruta vacia)").append('\n');
        bloque.append('\n');

        area.append(bloque.toString());
        area.setCaretPosition(area.getDocument().getLength());

        estado.setText("Rutas recibidas: " + rutasRecibidas
            + "   (ultima de " + (emisor != null ? emisor : "?") + ")");
    }

    private void limpiar() {
        area.setText("");
        rutasRecibidas = 0;
        estado.setText("Esperando ruta del recomendador...");
    }
}
