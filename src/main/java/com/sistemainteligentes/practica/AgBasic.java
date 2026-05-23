package com.sistemainteligentes.practica;

import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.AgentContainer;

public class AgBasic extends Agent {
    protected void setup() {
        System.out.println("Primer Agente JADE");
        System.out.println("AID: " + this.getAID());
        System.out.println("Entrando en espera");
        this.doWait(5000);
        System.out.println("Saliendo de espera, entrando en suspendido");
        this.doSuspend();
        System.out.println("Saliendo de suspendido");

        AgentContainer container=(AgentContainer) getContainerController();
        Object[] params=new Object[1];
        params[0]="nuevo_parametro";

        try{
            AgentController agnt=container.createNewAgent("nuevoAgente", "com.sistemainteligentes.AgBasic", params);
            agnt.start();
        }
        catch(Exception e){e.printStackTrace();
        }
    }
}
