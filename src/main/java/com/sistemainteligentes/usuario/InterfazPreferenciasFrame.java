package com.sistemainteligentes.usuario;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.sistemainteligentes.comun.PreferenciasUsuario;

/**
 * Ventana Swing en la que el usuario introduce sus preferencias de ruta.
 * Al pulsar "Enviar preferencias" llama a
 * {@link AgenteUsuario#enviarPreferencias(PreferenciasUsuario)}.
 *
 * Se construyo siguiendo el ejemplo de las transparencias JADE_2024-25_5
 * (uso de Swing dentro de un agente).
 */
public class InterfazPreferenciasFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final AgenteUsuario agente;

    private final JTextField campoCiudad = new JTextField("Madrid");
    private final JTextField campoDias = new JTextField("1");
    private final JTextField campoPresupuesto = new JTextField("50");
    private final JTextField campoIntereses = new JTextField("museos, comida");
    private final JTextArea areaSalida = new JTextArea(10, 40);

    public InterfazPreferenciasFrame(AgenteUsuario agente) {
        super("AgenteUsuario - Recomendador de rutas turisticas");
        this.agente = agente;
        construir();
        setSize(640, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }

    private void construir() {
        JPanel formulario = new JPanel(new GridLayout(4, 2, 8, 8));
        formulario.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        formulario.add(new JLabel("Ciudad:"));
        formulario.add(campoCiudad);
        formulario.add(new JLabel("Dias disponibles:"));
        formulario.add(campoDias);
        formulario.add(new JLabel("Presupuesto maximo (EUR):"));
        formulario.add(campoPresupuesto);
        formulario.add(new JLabel("Intereses (separados por coma):"));
        formulario.add(campoIntereses);

        JButton botonEnviar = new JButton("Enviar preferencias");
        botonEnviar.addActionListener(e -> alPulsarEnviar());

        areaSalida.setEditable(false);
        areaSalida.setLineWrap(true);
        areaSalida.setWrapStyleWord(true);

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.add(formulario, BorderLayout.CENTER);
        panelSuperior.add(botonEnviar, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(panelSuperior, BorderLayout.NORTH);
        add(new JScrollPane(areaSalida), BorderLayout.CENTER);
    }

    private void alPulsarEnviar() {
        try {
            PreferenciasUsuario prefs = leerPreferencias();
            mostrarMensaje("Preferencias preparadas: " + prefs);
            agente.enviarPreferencias(prefs);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Los campos numericos deben contener un numero valido.",
                "Datos invalidos", JOptionPane.WARNING_MESSAGE);
        }
    }

    private PreferenciasUsuario leerPreferencias() {
        String ciudad = campoCiudad.getText().trim();
        int dias = Integer.parseInt(campoDias.getText().trim());
        double presupuesto = Double.parseDouble(campoPresupuesto.getText().trim());
        List<String> intereses = new ArrayList<>();
        for (String it : Arrays.asList(campoIntereses.getText().split(","))) {
            String limpio = it.trim().toLowerCase();
            if (!limpio.isEmpty()) {
                intereses.add(limpio);
            }
        }
        return new PreferenciasUsuario(ciudad, dias, presupuesto, intereses);
    }

    public void mostrarMensaje(String texto) {
        areaSalida.append(texto + System.lineSeparator());
        areaSalida.setCaretPosition(areaSalida.getDocument().getLength());
    }
}
