package com.sistemainteligentes;

import jade.core.Agent;

public class AgBasicoParam extends Agent {

    @Override
    protected void setup() {
        Object[] listaparametros = getArguments();

        if ((listaparametros == null) || (listaparametros.length < 1)) {
            System.out.println("No se han introducido parametros");
        } else {
            System.out.println("Agente JADE con Parametros: Soy el agente " + getLocalName());

            for (int i = 0; i < listaparametros.length; i++) {
                System.out.println("Parametro " + i + " es: " + (String) listaparametros[i]);
            }

            // Elimina el agente una vez que termina de leer los parámetros
            doDelete();
        }
    }

    // El método takeDown debe ir FUERA de setup(), pero dentro de la clase
    @Override
    protected void takeDown() {
        System.out.println("Bye...");
    }
}
