package com.sistemainteligentes.practica;

import jade.lang.acl.*;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

public class ResponderBehaviourMio extends SimpleBehaviour {

    // Filtro: solo mensajes REQUEST
    private static final MessageTemplate mt =
        MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

    public ResponderBehaviourMio(Agent agent) {
        super(agent);
    }

    @Override
    public void action() {
        ACLMessage aclMessage = myAgent.receive(mt);
        if (aclMessage != null) {
            System.out.println();
            System.out.println(myAgent.getLocalName() +
                ": Recibo el mensaje:\n" + aclMessage);
        } else {
            block(); // espera bloqueante eficiente
        }
    }

    @Override
    public boolean done() {
        return false; // nunca termina, sigue escuchando
    }
}
