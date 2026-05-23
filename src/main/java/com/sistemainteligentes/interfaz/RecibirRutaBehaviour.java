package com.sistemainteligentes.interfaz;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

/**
 * Cyclic behaviour that listens for the final route sent by the
 * AgenteRecomendador.
 *
 * It implements the pattern recommended in the class slides: it filters
 * incoming messages with a {@link MessageTemplate} that combines the
 * performative (INFORM) and the ontology ("mostrar-ruta"), and it blocks
 * with {@code block()} when the queue is empty. This satisfies the
 * requirement of having at least one blocking message filter without
 * freezing the agent.
 *
 * The message content can arrive in two ways and both are supported:
 *   1. As a serializable object (for example a RutaRecomendada defined by
 *      the recommender team), in which case its toString() is used.
 *   2. As plain text (setContent), in which case it is shown as is.
 */
public class RecibirRutaBehaviour extends CyclicBehaviour {

    private static final long serialVersionUID = 1L;

    /** Only INFORM with ontology "mostrar-ruta": we never steal other messages. */
    private static final MessageTemplate FILTRO = MessageTemplate.and(
        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
        MessageTemplate.MatchOntology(AgenteInterfaz.ONTOLOGIA));

    public RecibirRutaBehaviour(Agent agente) {
        super(agente);
    }

    @Override
    public void action() {
        ACLMessage msg = myAgent.receive(FILTRO);
        if (msg == null) {
            block(); // blocking wait until a route arrives
            return;
        }
        manejar(msg);
    }

    private void manejar(ACLMessage msg) {
        String contenido = extraerContenido(msg);
        String emisor = msg.getSender().getLocalName();
        String conversationId = msg.getConversationId();

        System.out.println("[AgenteInterfaz] Route received from " + emisor
            + " (conv=" + conversationId + ").");

        if (myAgent instanceof AgenteInterfaz) {
            ((AgenteInterfaz) myAgent).mostrarRuta(contenido, emisor, conversationId);
        }
    }

    /**
     * Gets the message content accepting both a serializable object and plain
     * text. If the object is not readable (not Serializable or not on the
     * classpath) it falls back to {@code getContent()}.
     */
    private String extraerContenido(ACLMessage msg) {
        try {
            Object obj = msg.getContentObject();
            if (obj != null) {
                return obj.toString();
            }
        } catch (UnreadableException e) {
            // The content is not a serializable object, so we use the text.
        }
        String texto = msg.getContent();
        return texto != null ? texto : "(the route arrived empty)";
    }
}
