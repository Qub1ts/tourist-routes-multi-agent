package com.sistemainteligentes.usuario;

import javax.swing.SwingUtilities;

import com.sistemainteligentes.comun.PreferenciasUsuario;

import jade.core.Agent;

/**
 * Agente responsable de capturar las preferencias del usuario a traves de
 * una interfaz Swing y enviarselas al AgentePercepcionWeb (primer eslabon
 * del pipeline). La percepcion consulta la API web real y reenvia los datos
 * al AgenteRecomendador.
 *
 * Sigue el ciclo de vida estandar de JADE:
 *  - setup(): inicializa la ventana e instala el comportamiento de escucha
 *    de respuestas.
 *  - takeDown(): cierra la ventana al destruir el agente.
 *
 * Cada vez que el usuario pulsa "Enviar preferencias" desde la interfaz se
 * anade dinamicamente un {@link EnviarPreferenciasBehaviour} para esa
 * peticion concreta.
 */
public class AgenteUsuario extends Agent {

    private static final long serialVersionUID = 1L;

    private InterfazPreferenciasFrame frame;

    @Override
    protected void setup() {
        System.out.println("[AgenteUsuario] Iniciado: " + getAID().getName());

        // Si Java arranca en modo headless la creacion de la ventana
        // lanzaria HeadlessException y el agente moriria silenciosamente.
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            System.err.println("[AgenteUsuario] El entorno es headless: no "
                + "se puede abrir la interfaz Swing. Ejecuta sin -Djava.awt.headless=true.");
            doDelete();
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                frame = new InterfazPreferenciasFrame(this);
                frame.setVisible(true);
                // En macOS la ventana se queda detras de la RMA con frecuencia;
                // forzamos que salga al frente al arrancar.
                frame.setAlwaysOnTop(true);
                frame.toFront();
                frame.requestFocus();
                frame.setAlwaysOnTop(false);
            } catch (Throwable t) {
                System.err.println("[AgenteUsuario] Error abriendo la interfaz Swing:");
                t.printStackTrace();
            }
        });

        addBehaviour(new EsperarRespuestaBehaviour(this));
    }

    /**
     * Punto de entrada que invoca la interfaz Swing al pulsar el boton de
     * enviar. Encola un comportamiento de un solo disparo para no bloquear
     * el hilo de la UI.
     */
    public void enviarPreferencias(PreferenciasUsuario preferencias) {
        addBehaviour(new EnviarPreferenciasBehaviour(preferencias));
    }

    /**
     * Llamado por el behaviour de escucha cuando llega una confirmacion
     * desde el AgentePercepcionWeb o el AgenteRecomendador. Se reenvia a la
     * interfaz Swing para mostrarsela al usuario.
     */
    void mostrarConfirmacion(String texto) {
        if (frame != null) {
            SwingUtilities.invokeLater(() -> frame.mostrarMensaje(texto));
        } else {
            System.out.println("[AgenteUsuario] " + texto);
        }
    }

    @Override
    protected void takeDown() {
        if (frame != null) {
            SwingUtilities.invokeLater(() -> frame.dispose());
        }
        System.out.println("[AgenteUsuario] Finalizado: " + getLocalName());
    }
}
