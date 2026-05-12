package com.sistemainteligentes;

import jade.core.behaviours.SimpleBehaviour;

public class AgComportamientoSimple2 extends SimpleBehaviour {

    @Override
    public void action() {
    // TODO Auto-generated method stub
    for(int i=0;i<10;i++)
        System.out.println("Soy cs2, Ejecuto tarea " + i);
    }

    @Override
    public boolean done() {
    // TODO Auto-generated method stub
    return true;
    //return false;
    }

}
