package com.sistemainteligentes;

import jade.core.Agent;
import jade.core.behaviours.*;

public class AgComportamientoParalelo extends Agent {

    @Override
    protected void setup() {

        // --- Primer bloque secuencial ---
        SequentialBehaviour sequentialBehaviour1 = new SequentialBehaviour(this);

        sequentialBehaviour1.addSubBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                System.out.println("Subcomportamiento 1_1");
            }
        });

        sequentialBehaviour1.addSubBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                System.out.println("Subcomportamiento 1_2");
            }
        });

        sequentialBehaviour1.addSubBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                System.out.println("Subcomportamiento 1_3");
            }
        });


        // --- Segundo bloque secuencial ---
        SequentialBehaviour sequentialBehaviour2 = new SequentialBehaviour(this);

        sequentialBehaviour2.addSubBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                System.out.println("Subcomportamiento 2_1");
            }
        });

        sequentialBehaviour2.addSubBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                System.out.println("Subcomportamiento 2_2");
            }
        });

        sequentialBehaviour2.addSubBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                System.out.println("Subcomportamiento 2_3");
            }
        });


        // --- Comportamiento Paralelo Principal ---
        // Se ejecutará hasta que TODOS (WHEN_ALL) sus subcomportamientos terminen
        ParallelBehaviour parallelBehaviour = new ParallelBehaviour(this, ParallelBehaviour.WHEN_ALL);

        parallelBehaviour.addSubBehaviour(sequentialBehaviour1);
        parallelBehaviour.addSubBehaviour(sequentialBehaviour2);

        addBehaviour(parallelBehaviour);
    }
}
