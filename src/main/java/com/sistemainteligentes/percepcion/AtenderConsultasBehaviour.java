package com.sistemainteligentes.percepcion;

import java.io.IOException;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import com.sistemainteligentes.comun.InformePercepcion;
import com.sistemainteligentes.comun.InformeReplanificacion;
import com.sistemainteligentes.comun.PreferenciasUsuario;
import com.sistemainteligentes.comun.SolicitudReplanificacion;

public class AtenderConsultasBehaviour extends CyclicBehaviour {

    private static final long serialVersionUID = 1L;

    private final BasePercepcionAgent agente;

    private static final MessageTemplate FILTRO_CONSULTA_NORMAL =
            MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchOntology(BasePercepcionAgent.ONTOLOGIA_ENTRADA)
            );

    private static final MessageTemplate FILTRO_REPLANIFICACION =
            MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchOntology(BasePercepcionAgent.ONTOLOGIA_REPLANIFICACION)
            );

    private static final MessageTemplate FILTRO =
            MessageTemplate.or(FILTRO_CONSULTA_NORMAL, FILTRO_REPLANIFICACION);

    public AtenderConsultasBehaviour(BasePercepcionAgent agente) {
        super(agente);
        this.agente = agente;
    }

    @Override
    public void action() {
        ACLMessage msg = myAgent.receive(FILTRO);

        if (msg == null) {
            block();
            return;
        }

        String ontologia = msg.getOntology();

        if (BasePercepcionAgent.ONTOLOGIA_ENTRADA.equals(ontologia)) {
            atenderConsultaNormal(msg);
        } else if (BasePercepcionAgent.ONTOLOGIA_REPLANIFICACION.equals(ontologia)) {
            atenderReplanificacion(msg);
        }
    }

    private void atenderConsultaNormal(ACLMessage msg) {
        try {
            PreferenciasUsuario preferencias =
                    (PreferenciasUsuario) msg.getContentObject();

            System.out.println("[" + agente.nombreFuente()
                    + "] Consulta recibida de usuario: " + preferencias);

            InformePercepcion informe = agente.consultarFuente(preferencias);

            informarAlUsuario(msg, informe);
            enviarAlRecomendador(msg.getConversationId(), informe);

        } catch (UnreadableException e) {
            System.err.println("[" + agente.nombreFuente()
                    + "] Error leyendo preferencias: " + e.getMessage());
        }
    }

    private void atenderReplanificacion(ACLMessage msg) {
        try {
            SolicitudReplanificacion solicitud =
                    (SolicitudReplanificacion) msg.getContentObject();

            System.out.println("[" + agente.nombreFuente()
                    + "] Replanificación recibida: " + solicitud);

            InformeReplanificacion informe =
                    agente.replanificarFuente(solicitud);

            ACLMessage respuesta = msg.createReply();
            respuesta.setPerformative(ACLMessage.INFORM);
            respuesta.setOntology(BasePercepcionAgent.ONTOLOGIA_RESPUESTA_REPLANIFICACION);
            respuesta.setConversationId(msg.getConversationId());
            respuesta.setContentObject(informe);

            myAgent.send(respuesta);

            System.out.println("[" + agente.nombreFuente()
                    + "] Informe de replanificación enviado: " + informe);

        } catch (UnreadableException | IOException e) {
            System.err.println("[" + agente.nombreFuente()
                    + "] Error en replanificación: " + e.getMessage());
        }
    }

    private void informarAlUsuario(ACLMessage msg, InformePercepcion informe) {
        ACLMessage respuesta = msg.createReply();
        respuesta.setPerformative(ACLMessage.INFORM);
        respuesta.setOntology(BasePercepcionAgent.ONTOLOGIA_ENTRADA);
        respuesta.setConversationId(msg.getConversationId());
        respuesta.setContent("Agente de " + agente.nombreFuente()
                + " listo: " + informe);

        myAgent.send(respuesta);
    }

    private void enviarAlRecomendador(String conversationId, InformePercepcion informe) {
        DFAgentDescription plantilla = new DFAgentDescription();

        ServiceDescription sd = new ServiceDescription();
        sd.setType(BasePercepcionAgent.SERVICIO_RECOMENDADOR);
        plantilla.addServices(sd);

        try {
            DFAgentDescription[] resultados = DFService.search(myAgent, plantilla);

            if (resultados.length == 0) {
                System.err.println("[" + agente.nombreFuente()
                        + "] No hay AgenteRecomendador registrado en el DF.");
                return;
            }

            AID recomendador = resultados[0].getName();

            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(recomendador);
            request.setOntology(BasePercepcionAgent.ONTOLOGIA_SALIDA);
            request.setConversationId(conversationId);
            request.setContentObject(informe);

            myAgent.send(request);

            System.out.println("[" + agente.nombreFuente()
                    + "] Informe enviado a recomendador: " + informe);

        } catch (FIPAException | IOException e) {
            System.err.println("[" + agente.nombreFuente()
                    + "] Error enviando informe al recomendador: " + e.getMessage());
        }
    }
}