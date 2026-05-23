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
import com.sistemainteligentes.comun.PreferenciasUsuario;

/**
 * Comportamiento ciclico que atiende las peticiones que llegan al agente
 * de percepcion. Para cada REQUEST con ontologia
 * {@link BasePercepcionAgent#ONTOLOGIA_ENTRADA}:
 *
 *   1. Deserializa el contenido como {@link PreferenciasUsuario}.
 *   2. Delega en {@link BasePercepcionAgent#consultarFuente} para llamar
 *      a la API externa.
 *   3. Envia un INFORM de ACK al usuario (para que la interfaz pueda
 *      decirle "el agente X ya tiene sus datos").
 *   4. Reenvia un REQUEST al AgenteRecomendador con el mismo
 *      conversationId, llevando el {@link InformePercepcion} obtenido.
 *
 * Filtra los mensajes con {@link MessageTemplate} (REQUEST + ontologia)
 * y se bloquea con {@code block()} entre peticiones. Cumple el requisito
 * de "filtro de mensajes en modo bloqueante" del enunciado.
 *
 * El campo conversationId del mensaje de salida es el mismo que el de
 * entrada para que el AgenteRecomendador pueda correlacionar los
 * fragmentos que mandan los demas agentes de percepcion.
 */
class AtenderConsultasBehaviour extends CyclicBehaviour {

    private static final long serialVersionUID = 1L;

    private static final MessageTemplate FILTRO = MessageTemplate.and(
        MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
        MessageTemplate.MatchOntology(BasePercepcionAgent.ONTOLOGIA_ENTRADA));

    private final BasePercepcionAgent agentePercepcion;

    AtenderConsultasBehaviour(BasePercepcionAgent agente) {
        super(agente);
        this.agentePercepcion = agente;
    }

    @Override
    public void action() {
        ACLMessage peticion = myAgent.receive(FILTRO);
        if (peticion == null) {
            block();
            return;
        }
        procesar(peticion);
    }

    private void procesar(ACLMessage peticion) {
        PreferenciasUsuario preferencias = leerPreferencias(peticion);
        if (preferencias == null) {
            return; // el FAILURE ya se envio dentro de leerPreferencias()
        }

        String etiqueta = agentePercepcion.nombreFuente();
        String agName = myAgent.getLocalName();

        System.out.println("[" + agName + "] Consulta recibida de "
            + peticion.getSender().getLocalName() + ": " + preferencias);

        InformePercepcion informe = agentePercepcion.consultarFuente(preferencias);
        if (informe == null) {
            informe = new InformePercepcion(etiqueta,
                preferencias.getCiudad(), preferencias);
            informe.setErrorMensaje("El agente devolvio null inesperadamente.");
        }

        notificarAck(peticion, informe);

        AID recomendador = buscarRecomendador();
        if (recomendador == null) {
            informarAlUsuarioDeFallo(peticion,
                "No hay AgenteRecomendador registrado en el DF; el "
                + "informe de " + etiqueta + " no se ha podido entregar.");
            return;
        }

        reenviar(peticion, informe, recomendador);
    }

    private PreferenciasUsuario leerPreferencias(ACLMessage peticion) {
        try {
            Object contenido = peticion.getContentObject();
            if (!(contenido instanceof PreferenciasUsuario)) {
                informarAlUsuarioDeFallo(peticion,
                    "Contenido esperado PreferenciasUsuario, recibido "
                        + (contenido == null ? "null" : contenido.getClass().getName()));
                return null;
            }
            return (PreferenciasUsuario) contenido;
        } catch (UnreadableException e) {
            informarAlUsuarioDeFallo(peticion,
                "No se pudo leer el contenido: " + e.getMessage());
            return null;
        }
    }

    private void notificarAck(ACLMessage peticion, InformePercepcion informe) {
        ACLMessage ack = peticion.createReply();
        ack.setPerformative(ACLMessage.INFORM);
        ack.setContent("Agente de " + agentePercepcion.nombreFuente()
            + " listo: " + informe);
        myAgent.send(ack);
    }

    private AID buscarRecomendador() {
        DFAgentDescription plantilla = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(BasePercepcionAgent.SERVICIO_RECOMENDADOR);
        plantilla.addServices(sd);
        try {
            DFAgentDescription[] resultados = DFService.search(myAgent, plantilla);
            if (resultados.length > 0) {
                return resultados[0].getName();
            }
        } catch (FIPAException e) {
            System.err.println("[" + myAgent.getLocalName()
                + "] Error consultando DF: " + e.getMessage());
        }
        return null;
    }

    private void reenviar(ACLMessage peticionOriginal,
                          InformePercepcion informe,
                          AID recomendador) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(recomendador);
        msg.setOntology(BasePercepcionAgent.ONTOLOGIA_SALIDA);
        msg.setLanguage("JavaSerialization");
        if (peticionOriginal.getConversationId() != null) {
            // Vital para que el recomendador correlacione los fragmentos.
            msg.setConversationId(peticionOriginal.getConversationId());
        }
        msg.setReplyWith(myAgent.getLocalName() + "-" + System.currentTimeMillis());

        try {
            msg.setContentObject(informe);
        } catch (IOException e) {
            informarAlUsuarioDeFallo(peticionOriginal,
                "Error serializando informe: " + e.getMessage());
            return;
        }
        myAgent.send(msg);
        System.out.println("[" + myAgent.getLocalName() + "] Informe enviado a "
            + recomendador.getLocalName() + ": " + informe);
    }

    private void informarAlUsuarioDeFallo(ACLMessage peticion, String motivo) {
        ACLMessage respuesta = peticion.createReply();
        respuesta.setPerformative(ACLMessage.FAILURE);
        respuesta.setContent(motivo);
        myAgent.send(respuesta);
        System.err.println("[" + myAgent.getLocalName() + "] FAILURE -> "
            + peticion.getSender().getLocalName() + ": " + motivo);
    }
}
