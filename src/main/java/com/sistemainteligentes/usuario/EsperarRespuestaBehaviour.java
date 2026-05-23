package com.sistemainteligentes.usuario;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

/**
 * Comportamiento ciclico que escucha las respuestas que llegan al
 * AgenteUsuario:
 *
 *   - INFORM / FAILURE / REFUSE de cualquier agente de percepcion
 *     (ontologia "fuente-percepcion"): confirmacion de que ese agente
 *     concreto ya tiene su informe (o el motivo del fallo). En el caso
 *     del fan-out llegaran varios mensajes, uno por agente de
 *     percepcion.
 *   - INFORM de AgenteRecomendador (ontologia "recomendar-ruta"):
 *     confirmacion opcional de que la ruta esta lista y se mostrara en
 *     la interfaz del AgenteInterfaz.
 *
 * Implementa el patron recomendado en las transparencias: filtra los
 * mensajes con un {@link MessageTemplate} (combinando performativa y
 * ontologia) y se bloquea con {@code block()} cuando la cola esta
 * vacia. Asi cumple el requisito de "al menos un filtro de mensajes en
 * modo bloqueante" sin congelar al resto del agente.
 */
public class EsperarRespuestaBehaviour extends CyclicBehaviour {

    private static final long serialVersionUID = 1L;

    private static final MessageTemplate ONTOLOGIAS_ACEPTADAS =
        MessageTemplate.or(
            MessageTemplate.MatchOntology(EnviarPreferenciasBehaviour.ONTOLOGIA),
            MessageTemplate.MatchOntology(EnviarPreferenciasBehaviour.SERVICIO_RECOMENDADOR));

    private static final MessageTemplate PERFORMATIVAS_ACEPTADAS =
        MessageTemplate.or(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.FAILURE),
                MessageTemplate.MatchPerformative(ACLMessage.REFUSE)));

    private static final MessageTemplate FILTRO =
        MessageTemplate.and(ONTOLOGIAS_ACEPTADAS, PERFORMATIVAS_ACEPTADAS);

    public EsperarRespuestaBehaviour(Agent agente) {
        super(agente);
    }

    @Override
    public void action() {
        ACLMessage msg = myAgent.receive(FILTRO);
        if (msg == null) {
            block(); // espera bloqueante hasta que llegue un mensaje que case el filtro
            return;
        }
        manejar(msg);
    }

    private void manejar(ACLMessage msg) {
        String resumen;
        try {
            Object contenido = msg.getContentObject();
            resumen = contenido != null ? contenido.toString() : msg.getContent();
        } catch (UnreadableException e) {
            // Si el contenido no es Serializable, caemos al texto plano
            resumen = msg.getContent();
        }
        String texto = "[" + performativa(msg) + "] "
            + msg.getSender().getLocalName() + ": " + resumen;
        System.out.println("[Usuario] " + texto);
        if (myAgent instanceof AgenteUsuario) {
            ((AgenteUsuario) myAgent).mostrarConfirmacion(texto);
        }
    }

    private String performativa(ACLMessage msg) {
        return ACLMessage.getPerformative(msg.getPerformative());
    }
}
