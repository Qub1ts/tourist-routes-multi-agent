package com.sistemainteligentes.recomendador;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class AgenteRecomendador extends Agent {

    @Override
    protected void setup() {
        registrarServicio();
        addBehaviour(new RecogerFragmentosBehaviour(this));
        System.out.println("[Recomendador] Listo. Servicio DF: recomendar-ruta");
    }

    private void registrarServicio() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("recomendar-ruta");
        sd.setName("Agente recomendador basado en ranking y scoring");

        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
            doDelete();
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            // ignore
        }
        System.out.println("[Recomendador] Finalizado.");
    }
}