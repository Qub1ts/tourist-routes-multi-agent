package com.sistemainteligentes.usuario;

import java.io.IOException;

import com.sistemainteligentes.comun.PreferenciasUsuario;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

/**
 * Comportamiento de un solo disparo que dispara el flujo del sistema.
 *
 * Hace <b>fan-out</b>: busca en el DF TODOS los agentes que ofrezcan el
 * servicio generico {@value #SERVICIO_PERCEPCION_GENERICO} (lo registran
 * todos los agentes de percepcion: clima, lugares, hoteles, eventos...)
 * y envia un REQUEST identico a cada uno con las preferencias del
 * usuario.
 *
 * Todos los REQUEST comparten el mismo {@code conversationId} para que
 * el AgenteRecomendador pueda correlacionar los fragmentos
 * (InformePercepcion) que le devolvera cada agente de percepcion.
 *
 * Si no se encuentra ningun proveedor de percepcion, deja una traza en
 * la interfaz y termina (el usuario podra reintentar mas tarde).
 */
public class EnviarPreferenciasBehaviour extends OneShotBehaviour {

    private static final long serialVersionUID = 1L;

    /** Servicio comun que registran TODOS los agentes de percepcion. */
    static final String SERVICIO_PERCEPCION_GENERICO = "fuente-percepcion";

    /** Servicio que registra el AgenteRecomendador. */
    static final String SERVICIO_RECOMENDADOR = "recomendar-ruta";

    /** Ontologia que entienden los agentes de percepcion. */
    static final String ONTOLOGIA = "fuente-percepcion";

    private final PreferenciasUsuario preferencias;

    public EnviarPreferenciasBehaviour(PreferenciasUsuario preferencias) {
        this.preferencias = preferencias;
    }

    @Override
    public void action() {
        AID[] proveedores = buscarProveedores();
        if (proveedores.length == 0) {
            avisarUsuario("No se ha encontrado ningun agente de percepcion en "
                + "el DF (clima, lugares, hoteles, eventos...). Lanzalos "
                + "primero.");
            return;
        }

        String conversationId = "ruta-" + System.currentTimeMillis();
        int enviados = 0;
        StringBuilder nombres = new StringBuilder();
        for (AID destino : proveedores) {
            if (enviarA(destino, conversationId)) {
                enviados++;
                if (nombres.length() > 0) nombres.append(", ");
                nombres.append(destino.getLocalName());
            }
        }
        avisarUsuario("Preferencias enviadas a " + enviados + " agente(s) de "
            + "percepcion: " + nombres + ". Cada uno consultara su API y "
            + "enviara su informe al AgenteRecomendador (conversationId="
            + conversationId + ").");
    }

    private AID[] buscarProveedores() {
        DFAgentDescription plantilla = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(SERVICIO_PERCEPCION_GENERICO);
        plantilla.addServices(sd);
        try {
            DFAgentDescription[] resultados = DFService.search(myAgent, plantilla);
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

    private boolean enviarA(AID destino, String conversationId) {
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
        myAgent.send(msg);
        System.out.println("[Usuario] REQUEST -> " + destino.getLocalName()
            + " (conv=" + conversationId + ") " + preferencias);
        return true;
    }

    private void avisarUsuario(String texto) {
        if (myAgent instanceof AgenteUsuario) {
            ((AgenteUsuario) myAgent).mostrarConfirmacion(texto);
        }
        System.out.println("[Usuario] " + texto);
    }
}
