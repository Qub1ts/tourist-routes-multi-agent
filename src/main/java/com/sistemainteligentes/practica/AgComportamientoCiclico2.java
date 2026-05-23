package com.sistemainteligentes.practica;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;

public class AgComportamientoCiclico2 extends Agent {

    // Es buena práctica hacer privadas las variables de los comportamientos
    private ComportamientoCiclico1 cs1;
    private ComportamientoCiclico2 cs2;
    private ComportamientoCiclico3 cs3;

    @Override
    protected void setup() {
        System.out.println("Primer Agente JADE con 2 Comportamientos");

        cs1 = new ComportamientoCiclico1();
        cs2 = new ComportamientoCiclico2();
        cs3 = new ComportamientoCiclico3();

        addBehaviour(cs1);
        addBehaviour(cs2);
        addBehaviour(cs3);
        cs3.block();

        System.out.println("Después de añadir los comportamientos");
    }

    // --- Primer Comportamiento ---
    private class ComportamientoCiclico1 extends CyclicBehaviour {
        int limite = 0;

        @Override
        public void action() {
            limite++;
            System.out.println("Ejecuto tarea C1 Lim: " + limite);

            // Cuando llega a 5000, elimina el comportamiento 2
            if (limite > 5000) {
                removeBehaviour(cs2);
                cs3.restart();
                System.out.println(">>> Comportamiento 2 eliminado por el Comportamiento 1 <<<");
                // Si también quieres que el comportamiento 1 se detenga aquí, descomenta la siguiente línea:
                // removeBehaviour(this);
            }
        }
    }

    // --- Segundo Comportamiento ---
    private class ComportamientoCiclico2 extends CyclicBehaviour {
        int limite = 0;

        @Override
        public void action() {
            limite++;
            System.out.println("Ejecuto tarea C2 Lim: " + limite);
        }
    }

    private class ComportamientoCiclico3 extends CyclicBehaviour {
        int limite=0;
        public void action() {
            limite++;
            System.out.println("Ejecuto tarea C3Lim" + limite);
        }
    }
}
