package com.sistemainteligentes.practica;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;

// Un agente extiende la clase Agent de JADE
public class Agente1 extends Agent {

    protected CyclicBehaviour cyclicBehaviour;

    @Override
    public void setup() {
        System.out.println("Soy el Agente 1");
        cyclicBehaviour = new CyclicBehaviour(this) {
            @Override
            public void action() {
                block();
            }
        };
        addBehaviour(cyclicBehaviour);
    }
}
