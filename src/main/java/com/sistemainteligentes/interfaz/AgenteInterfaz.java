package com.sistemainteligentes.interfaz;

import javax.swing.SwingUtilities;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class AgenteInterfaz extends Agent {

    private static final long serialVersionUID = 1L;
    static final String ONTOLOGIA = "mostrar-ruta";
    private VentanaRutaFrame ventana;

    @Override
    protected void setup() {
        System.out.println("[AgenteInterfaz] Iniciado: " + getAID().getName());

        registrarServicio();

        SwingUtilities.invokeLater(() -> {
            ventana = new VentanaRutaFrame(getLocalName(), this::rechazarRuta);
            ventana.setVisible(true);
            ventana.setAlwaysOnTop(true);
            ventana.toFront();
            ventana.setAlwaysOnTop(false);
        });

        addBehaviour(new RecibirRutaBehaviour(this));
    }

    private void registrarServicio() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("mostrar-ruta");
        sd.setName("Interfaz visual de rutas turisticas");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            System.out.println("[AgenteInterfaz] Servicio mostrar-ruta registrado en DF.");
        } catch (FIPAException e) {
            System.err.println("[AgenteInterfaz] Error registrando servicio: " + e.getMessage());
            doDelete();
        }
    }

    public void mostrarRuta(String contenido, String emisor, String conversationId) {
        if (ventana != null) {
            SwingUtilities.invokeLater(() ->
                    ventana.mostrarRuta(contenido, emisor, conversationId)
            );
        }
    }

    private void rechazarRuta(String conversationId) {
        DFAgentDescription plantilla = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("recomendar-ruta");
        plantilla.addServices(sd);

        try {
            DFAgentDescription[] resultados = DFService.search(this, plantilla);

            if (resultados.length == 0) {
                System.err.println("[AgenteInterfaz] No se encontro AgenteRecomendador.");
                return;
            }

            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(resultados[0].getName());
            msg.setOntology("rechazar-ruta");
            msg.setConversationId(conversationId);
            msg.setContent("rechazada");

            send(msg);

            System.out.println("[AgenteInterfaz] Usuario rechazo la ruta. Solicitando alternativa...");

        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            // ignore
        }

        if (ventana != null) {
            SwingUtilities.invokeLater(() -> ventana.dispose());
        }

        System.out.println("[AgenteInterfaz] Finalizado.");
    }
}