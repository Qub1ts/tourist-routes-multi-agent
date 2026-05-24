package com.sistemainteligentes.recomendador;

import com.sistemainteligentes.comun.InformePercepcion;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecogerFragmentosBehaviour extends CyclicBehaviour {

    private static final int FRAGMENTOS_ESPERADOS = 4;

    private static final MessageTemplate FILTRO = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
            MessageTemplate.MatchOntology("recomendar-ruta")
    );

    private final Map<String, List<InformePercepcion>> sesiones = new HashMap<>();
    private final MotorRecomendacion motor = new MotorRecomendacion();

    public RecogerFragmentosBehaviour(Agent agente) {
        super(agente);
    }

    @Override
    public void action() {
        ACLMessage msg = myAgent.receive(FILTRO);

        if (msg == null) {
            block();
            return;
        }

        try {
            InformePercepcion fragmento = (InformePercepcion) msg.getContentObject();
            String conversationId = msg.getConversationId();

            sesiones.computeIfAbsent(conversationId, k -> new ArrayList<>()).add(fragmento);

            System.out.println("[Recomendador] Fragmento recibido: "
                    + fragmento.getFuente()
                    + " | conv=" + conversationId
                    + " | total=" + sesiones.get(conversationId).size());

            if (sesiones.get(conversationId).size() >= FRAGMENTOS_ESPERADOS) {
                List<InformePercepcion> fragmentos = sesiones.remove(conversationId);
                procesarYEnviar(conversationId, fragmentos);
            }

        } catch (UnreadableException e) {
            System.err.println("[Recomendador] Error leyendo InformePercepcion: " + e.getMessage());
        }
    }

    private void procesarYEnviar(String conversationId, List<InformePercepcion> fragmentos) {
        String rutaRecomendada = motor.generarRecomendacion(fragmentos);

        System.out.println("\n================ RUTA RECOMENDADA ================");
        System.out.println(rutaRecomendada);
        System.out.println("===================================================\n");

        enviarAlInterfaz(conversationId, rutaRecomendada);
    }

    private void enviarAlInterfaz(String conversationId, String contenidoRuta) {
        DFAgentDescription plantilla = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("mostrar-ruta");
        plantilla.addServices(sd);

        try {
            DFAgentDescription[] resultados = DFService.search(myAgent, plantilla);

            if (resultados.length == 0) {
                System.err.println("[Recomendador] No hay AgenteInterfaz en el DF. Ruta impresa en consola.");
                return;
            }

            AID interfaz = resultados[0].getName();

            ACLMessage respuesta = new ACLMessage(ACLMessage.INFORM);
            respuesta.addReceiver(interfaz);
            respuesta.setOntology("mostrar-ruta");
            respuesta.setConversationId(conversationId);
            respuesta.setContent(contenidoRuta);

            myAgent.send(respuesta);

            System.out.println("[Recomendador] Ruta enviada al AgenteInterfaz.");

        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}