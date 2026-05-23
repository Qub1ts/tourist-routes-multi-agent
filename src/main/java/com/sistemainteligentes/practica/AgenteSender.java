package com.sistemainteligentes.practica;

import jade.lang.acl.*;
import jade.core.*;
import jade.core.Agent;

public class AgenteSender extends Agent {
    protected void setup() {
        System.out.println("Agente agenteSender");
        System.out.println("AID: " + getAID());
        System.out.println("Nombre AID: " + getAID().getName());

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        AID r = new AID("agenteReceiver", AID.ISLOCALNAME);
        // Nota: AID.ISLOCALNAME es más portable que escribir la IP a mano.
        // Si trabajaras entre máquinas distintas, usarías:
        // r.setName("agenteReceiver@192.168.x.x:1099/JADE");

        System.out.println("Envio mensaje a: " + r.getName());
        msg.addReceiver(r);
        msg.setContent("Mensaje de prueba");
        this.send(msg);
        System.out.println("Mensaje Enviado....");
    }
}
