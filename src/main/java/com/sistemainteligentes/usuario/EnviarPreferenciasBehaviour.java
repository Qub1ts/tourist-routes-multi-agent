package com.sistemainteligentes.usuario;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sistemainteligentes.comun.PreferenciasUsuario;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class EnviarPreferenciasBehaviour extends OneShotBehaviour {

    private static final long serialVersionUID = 1L;

    static final String SERVICIO_PERCEPCION_GENERICO = "fuente-percepcion";
    static final String ONTOLOGIA = "fuente-percepcion";

    private final PreferenciasUsuario preferencias;

    static final String SERVICIO_RECOMENDADOR = "recomendar-ruta";

    public EnviarPreferenciasBehaviour(PreferenciasUsuario preferencias) {
        this.preferencias = preferencias;
    }

    @Override
    public void action() {
        final Agent agenteActual = myAgent;

        AID[] proveedores = buscarProveedores(agenteActual);

        if (proveedores.length == 0) {
            avisarUsuario(agenteActual, "No se ha encontrado ningun agente de percepcion en el DF.");
            return;
        }

        String conversationId = "ruta-" + System.currentTimeMillis();

        ParallelBehaviour fanOutParalelo =
                new ParallelBehaviour(agenteActual, ParallelBehaviour.WHEN_ALL);

        List<String> nombres = new ArrayList<>();

        for (AID destino : proveedores) {
            nombres.add(destino.getLocalName());

            fanOutParalelo.addSubBehaviour(new OneShotBehaviour(agenteActual) {
                private static final long serialVersionUID = 1L;

                @Override
                public void action() {
                    enviarA(agenteActual, destino, conversationId);
                }
            });
        }

        agenteActual.addBehaviour(fanOutParalelo);

        avisarUsuario(agenteActual, "Preferencias enviadas en paralelo a "
                + proveedores.length
                + " agente(s) de percepcion: "
                + String.join(", ", nombres)
                + ". Cada uno consultara su API y enviara su informe al AgenteRecomendador "
                + "(conversationId=" + conversationId + ").");
    }

    private AID[] buscarProveedores(Agent agenteActual) {
        DFAgentDescription plantilla = new DFAgentDescription();

        ServiceDescription sd = new ServiceDescription();
        sd.setType(SERVICIO_PERCEPCION_GENERICO);
        plantilla.addServices(sd);

        try {
            DFAgentDescription[] resultados = DFService.search(agenteActual, plantilla);
            AID[] aids = new AID[resultados.length];

            for (int i = 0; i < resultados.length; i++) {
                aids[i] = resultados[i].getName();
            }

            return aids;

        } catch (FIPAException e) {
            System.err.println("[Usuario] Error consultando el DF: " + e.getMessage());
            return new AID[0];
        }
    }

    private boolean enviarA(Agent agenteActual, AID destino, String conversationId) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);

        msg.addReceiver(destino);
        msg.setOntology(ONTOLOGIA);
        msg.setLanguage("JavaSerialization");
        msg.setConversationId(conversationId);
        msg.setReplyWith("req-" + destino.getLocalName() + "-" + System.currentTimeMillis());

        try {
            msg.setContentObject(preferencias);
        } catch (IOException e) {
            System.err.println("[Usuario] Error serializando para "
                    + destino.getLocalName() + ": " + e.getMessage());
            return false;
        }

        agenteActual.send(msg);

        System.out.println("[Usuario] REQUEST paralelo -> "
                + destino.getLocalName()
                + " (conv=" + conversationId + ") "
                + preferencias);

        return true;
    }

    private void avisarUsuario(Agent agenteActual, String texto) {
        if (agenteActual instanceof AgenteUsuario) {
            ((AgenteUsuario) agenteActual).mostrarConfirmacion(texto);
        }

        System.out.println("[Usuario] " + texto);
    }
}