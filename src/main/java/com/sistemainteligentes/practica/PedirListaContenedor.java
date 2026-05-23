package com.sistemainteligentes.practica;

import jade.core.Location;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPANames;
import jade.domain.mobility.MobilityOntology;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.domain.JADEAgentManagement.QueryPlatformLocationsAction;
import jade.content.lang.sl.SLCodec;

// Heredamos de OneShotBehaviour porque es una acción que se ejecuta una sola vez
public class PedirListaContenedor extends OneShotBehaviour {

    @Override
    public void action() {
        // En los behaviours, usamos myAgent para acceder a los métodos del agente

        // 1. Registrar lenguaje y ontología
        myAgent.getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL0);
        myAgent.getContentManager().registerOntology(MobilityOntology.getInstance());

        // 2. Preparar el mensaje de solicitud
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.addReceiver(myAgent.getAMS());
        message.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
        message.setOntology(MobilityOntology.NAME);
        message.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        message.setConversationId("query-locations-" + System.currentTimeMillis());

        // 3. Preparar la acción a solicitar
        Action action = new Action();
        action.setActor(myAgent.getAMS());
        action.setAction(new QueryPlatformLocationsAction());

        try {
            // 4. Llenar el contenido y enviar
            myAgent.getContentManager().fillContent(message, action);
            myAgent.send(message);

            // 5. Esperar la respuesta de forma segura
            MessageTemplate mt = MessageTemplate.MatchConversationId(message.getConversationId());
            // Al usar blockingReceive en un Behaviour, bloqueamos solo este comportamiento
            // hasta que llegue la respuesta
            ACLMessage msg = myAgent.blockingReceive(mt);

            if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
                // 6. Extraer y procesar los resultados
                Result results = (Result) myAgent.getContentManager().extractContent(msg);

                jade.util.leap.Iterator res_it = results.getItems().iterator();
                while (res_it.hasNext()) {
                    Location loc = (Location) res_it.next();
                    System.out.println("Exists: " + loc.getName());
                }
            } else {
                 System.err.println("La respuesta del AMS falló o no fue un INFORM.");
            }

        } catch (Exception e) {
            System.err.println("Error al consultar las localizaciones de la plataforma:");
            e.printStackTrace();
        }
    }
}

