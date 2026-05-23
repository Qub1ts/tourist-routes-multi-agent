package com.sistemainteligentes.interfaz;

import javax.swing.SwingUtilities;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

/**
 * Output agent of the multi agent system. It is the last stage of the
 * pipeline:
 *
 *   Usuario  =>  (fan out)  =>  4 perception agents  =>  Recomendador  =>  INTERFAZ
 *
 * Its single responsibility is to receive the route already computed by the
 * AgenteRecomendador and present it to the user in a Swing window, showing
 * the places, hotels, events, expected weather and approximate costs.
 *
 * It follows the standard JADE life cycle:
 *   setup():    registers the "mostrar-ruta" service in the Directory
 *               Facilitator, opens the window and installs the listening
 *               behaviour {@link RecibirRutaBehaviour}.
 *   takeDown(): deregisters from the DF and closes the window.
 *
 * Messaging contract (see README, section 5):
 *   From:        AgenteRecomendador
 *   Performative: INFORM
 *   Ontology:    "mostrar-ruta"
 *   Content:     a serializable route object (RutaRecomendada) or, as a
 *                fallback, a plain String. This agent accepts both cases:
 *                if the content is an object its toString() is used; if it
 *                is plain text it is shown as is. That way this agent does
 *                not depend on how the recommender team ends up sending the
 *                route.
 */
public class AgenteInterfaz extends Agent {

    private static final long serialVersionUID = 1L;

    /** Service published by this agent in the DF. */
    public static final String SERVICIO = "mostrar-ruta";
    /** Ontology of the messages this agent accepts. */
    public static final String ONTOLOGIA = "mostrar-ruta";

    private VentanaRutaFrame ventana;

    @Override
    protected void setup() {
        System.out.println("[AgenteInterfaz] Started: " + getAID().getName());

        // In headless mode Swing cannot open a window and the agent would
        // die silently, so we detect it and report it (same pattern used by
        // the AgenteUsuario).
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            System.err.println("[AgenteInterfaz] Headless environment: the Swing "
                + "interface cannot be opened. Run without java.awt.headless=true.");
            doDelete();
            return;
        }

        registrarServicio();

        SwingUtilities.invokeLater(() -> {
            try {
                ventana = new VentanaRutaFrame(getLocalName());
                ventana.setVisible(true);
                // On macOS the window often stays behind the RMA, so we push
                // it to the front when starting.
                ventana.setAlwaysOnTop(true);
                ventana.toFront();
                ventana.requestFocus();
                ventana.setAlwaysOnTop(false);
            } catch (Throwable t) {
                System.err.println("[AgenteInterfaz] Error opening the Swing interface:");
                t.printStackTrace();
            }
        });

        addBehaviour(new RecibirRutaBehaviour(this));
        System.out.println("[AgenteInterfaz] Ready. Waiting for routes from the recommender...");
    }

    /**
     * Registers the "mostrar-ruta" service in the Directory Facilitator so the
     * AgenteRecomendador can discover this agent and send it the final route.
     */
    private void registrarServicio() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(SERVICIO);
        sd.setName("Visor de rutas turisticas");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println("[AgenteInterfaz] Registered in the DF as '" + SERVICIO + "'.");
        } catch (FIPAException e) {
            System.err.println("[AgenteInterfaz] Could not register in the DF:");
            e.printStackTrace();
            doDelete();
        }
    }

    /**
     * Called by {@link RecibirRutaBehaviour} when a route arrives. Forwards the
     * content to the Swing window on the event dispatch thread.
     *
     * @param contenido      route already formatted as text
     * @param emisor         local name of the sending agent
     * @param conversationId session id (format "ruta" plus a timestamp)
     */
    void mostrarRuta(String contenido, String emisor, String conversationId) {
        if (ventana != null) {
            SwingUtilities.invokeLater(() ->
                ventana.mostrarRuta(contenido, emisor, conversationId));
        } else {
            System.out.println("[AgenteInterfaz] Route received (no window):\n" + contenido);
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            // If it was already deregistered there is nothing to do.
        }
        if (ventana != null) {
            SwingUtilities.invokeLater(() -> ventana.dispose());
        }
        System.out.println("[AgenteInterfaz] Finished: " + getLocalName());
    }
}
