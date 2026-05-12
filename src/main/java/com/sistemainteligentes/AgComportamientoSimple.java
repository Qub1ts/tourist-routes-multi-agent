package com.sistemainteligentes;

import jade.core.Agent;
import jade.core.behaviours.*;

public class AgComportamientoSimple extends Agent {

    class ComportamientoSimple extends SimpleBehaviour{
    public void action(){
        for(int i=0;i<10;i++)
        System.out.println("Ejecuto tarea " + i);
    }

    public boolean done(){
        return true;
        //return false;
    }
}
    protected void setup(){
    System.out.println("Primer Agente JADE con Comportamiento Simple");
    AgComportamientoSimple2 cs2= new AgComportamientoSimple2();
    addBehaviour(cs2);
    }
}

