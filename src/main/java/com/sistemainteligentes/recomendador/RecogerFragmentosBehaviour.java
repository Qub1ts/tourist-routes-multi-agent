package com.sistemainteligentes.recomendador;

import com.sistemainteligentes.comun.InformePercepcion;
import com.sistemainteligentes.comun.InformeReplanificacion;
import com.sistemainteligentes.comun.PreferenciasUsuario;
import com.sistemainteligentes.comun.SolicitudReplanificacion;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecogerFragmentosBehaviour extends CyclicBehaviour {

    private static final long serialVersionUID = 1L;

    private static final int FRAGMENTOS_ESPERADOS = 4;
    private static final int REPLANIFICACIONES_ESPERADAS = 2;

    private static final String ONTOLOGIA_RECOMENDAR = "recomendar-ruta";
    private static final String ONTOLOGIA_REPLANIFICAR = "replanificar-ruta";
    private static final String ONTOLOGIA_RESPUESTA_REPLANIFICACION = "respuesta-replanificacion";
    private static final String ONTOLOGIA_RECHAZAR = "rechazar-ruta";

    private static final MessageTemplate FILTRO_INFORMES =
            MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchOntology(ONTOLOGIA_RECOMENDAR)
            );

    private static final MessageTemplate FILTRO_REPLANIFICACION =
            MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchOntology(ONTOLOGIA_RESPUESTA_REPLANIFICACION)
            );

    private static final MessageTemplate FILTRO_RECHAZO =
            MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchOntology(ONTOLOGIA_RECHAZAR)
            );

    private static final MessageTemplate FILTRO =
            MessageTemplate.or(
                    MessageTemplate.or(FILTRO_INFORMES, FILTRO_REPLANIFICACION),
                    FILTRO_RECHAZO
            );

    private final Map<String, List<InformePercepcion>> sesiones = new HashMap<>();
    private final Map<String, List<InformePercepcion>> sesionesPendientes = new HashMap<>();
    private final Map<String, List<InformeReplanificacion>> replanificaciones = new HashMap<>();

    private final Map<String, List<InformePercepcion>> historialFragmentos = new HashMap<>();
    private final Map<String, List<InformeReplanificacion>> historialReplanificacion = new HashMap<>();
    private final Map<String, String> rutasGeneradas = new HashMap<>();
    private final Map<String, Integer> intentosAlternativa = new HashMap<>();

    // Guarda candidatos ya usados para no repetirlos cuando el usuario rechaza.
    private final Map<String, List<String>> candidatosUsadosPorSesion = new HashMap<>();

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

        if (ONTOLOGIA_RECOMENDAR.equals(msg.getOntology())) {
            atenderInformePercepcion(msg);
        } else if (ONTOLOGIA_RESPUESTA_REPLANIFICACION.equals(msg.getOntology())) {
            atenderRespuestaReplanificacion(msg);
        } else if (ONTOLOGIA_RECHAZAR.equals(msg.getOntology())) {
            atenderRechazoUsuario(msg);
        }
    }

    private void atenderInformePercepcion(ACLMessage msg) {
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

                String criterio = decidirCriterioPorClima(fragmentos);

                System.out.println("[Recomendador] Criterio de replanificacion detectado: "
                        + criterio.toUpperCase());

                sesionesPendientes.put(conversationId, fragmentos);
                replanificaciones.put(conversationId, new ArrayList<>());

                solicitarReplanificacion(conversationId, fragmentos, criterio);
            }

        } catch (UnreadableException e) {
            System.err.println("[Recomendador] Error leyendo InformePercepcion: " + e.getMessage());
        }
    }

    private void atenderRespuestaReplanificacion(ACLMessage msg) {
        try {
            InformeReplanificacion informe =
                    (InformeReplanificacion) msg.getContentObject();

            String conversationId = msg.getConversationId();

            replanificaciones
                    .computeIfAbsent(conversationId, k -> new ArrayList<>())
                    .add(informe);

            System.out.println("[Recomendador] Replanificacion recibida desde "
                    + informe.getFuente()
                    + " | criterio=" + informe.getCriterio()
                    + " | conv=" + conversationId
                    + " | total=" + replanificaciones.get(conversationId).size());

            if (replanificaciones.get(conversationId).size() >= REPLANIFICACIONES_ESPERADAS) {
                List<InformePercepcion> fragmentos =
                        sesionesPendientes.remove(conversationId);

                List<InformeReplanificacion> informesReplanificacion =
                        replanificaciones.remove(conversationId);

                procesarYEnviar(conversationId, fragmentos, informesReplanificacion);
            }

        } catch (UnreadableException e) {
            System.err.println("[Recomendador] Error leyendo InformeReplanificacion: "
                    + e.getMessage());
        }
    }

    private void atenderRechazoUsuario(ACLMessage msg) {
        String conversationId = msg.getConversationId();

        List<InformePercepcion> fragmentos = historialFragmentos.get(conversationId);
        List<InformeReplanificacion> informesReplanificacion =
                historialReplanificacion.getOrDefault(conversationId, new ArrayList<>());

        if (fragmentos == null || fragmentos.isEmpty()) {
            enviarAlInterfaz(conversationId,
                    "No se pudo generar una alternativa porque no se encontraron "
                            + "los datos originales de la sesion.");
            return;
        }

        int intento = intentosAlternativa.getOrDefault(conversationId, 1) + 1;
        intentosAlternativa.put(conversationId, intento);

        List<String> usados = candidatosUsadosPorSesion.getOrDefault(
                conversationId,
                new ArrayList<>()
        );

        String nuevaRuta = motor.generarRecomendacionAlternativa(fragmentos, intento, usados);

        List<String> nuevosUsados = motor.extraerNombresSeleccionados(
                fragmentos,
                intento - 1,
                usados
        );

        usados.addAll(nuevosUsados);
        candidatosUsadosPorSesion.put(conversationId, usados);

        nuevaRuta += "\n\nPROPUESTA ALTERNATIVA\n";
        nuevaRuta += "Esta ruta fue generada porque el usuario rechazo la propuesta anterior.\n";
        nuevaRuta += "Intento de recomendacion: #" + intento + "\n";
        nuevaRuta += "El recomendador excluyo candidatos ya mostrados y busco opciones diferentes.\n";

        nuevaRuta += construirResumenReplanificacion(informesReplanificacion);

        rutasGeneradas.put(conversationId, nuevaRuta);

        enviarAlInterfaz(conversationId, nuevaRuta);

        System.out.println("[Recomendador] Usuario rechazo la ruta.");
        System.out.println("[Recomendador] Nueva ruta real generada para conv="
                + conversationId + " intento=" + intento);
    }

    private void solicitarReplanificacion(
            String conversationId,
            List<InformePercepcion> fragmentos,
            String criterio
    ) {
        PreferenciasUsuario preferencias = fragmentos.get(0).getPreferencias();
        String ciudad = preferencias.getCiudad();

        SolicitudReplanificacion solicitud =
                new SolicitudReplanificacion(ciudad, criterio, preferencias);

        ParallelBehaviour replanificacionParalela =
                new ParallelBehaviour(myAgent, ParallelBehaviour.WHEN_ALL);

        replanificacionParalela.addSubBehaviour(new OneShotBehaviour(myAgent) {
            private static final long serialVersionUID = 1L;

            @Override
            public void action() {
                enviarSolicitudReplanificacion(
                        conversationId,
                        "percepcion-lugares",
                        solicitud
                );
            }
        });

        replanificacionParalela.addSubBehaviour(new OneShotBehaviour(myAgent) {
            private static final long serialVersionUID = 1L;

            @Override
            public void action() {
                enviarSolicitudReplanificacion(
                        conversationId,
                        "percepcion-eventos",
                        solicitud
                );
            }
        });

        myAgent.addBehaviour(replanificacionParalela);

        System.out.println("[Recomendador] Replanificacion paralela iniciada hacia lugares y eventos.");
    }

    private void enviarSolicitudReplanificacion(
            String conversationId,
            String tipoServicio,
            SolicitudReplanificacion solicitud
    ) {
        DFAgentDescription plantilla = new DFAgentDescription();

        ServiceDescription sd = new ServiceDescription();
        sd.setType(tipoServicio);
        plantilla.addServices(sd);

        try {
            DFAgentDescription[] resultados = DFService.search(myAgent, plantilla);

            if (resultados.length == 0) {
                System.err.println("[Recomendador] No se encontro agente con servicio: "
                        + tipoServicio);
                return;
            }

            AID agenteDestino = resultados[0].getName();

            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(agenteDestino);
            request.setOntology(ONTOLOGIA_REPLANIFICAR);
            request.setConversationId(conversationId);
            request.setContentObject(solicitud);

            myAgent.send(request);

            System.out.println("[Recomendador] Solicitud de replanificacion enviada a "
                    + agenteDestino.getLocalName()
                    + " | criterio=" + solicitud.getCriterio());

        } catch (FIPAException | IOException e) {
            System.err.println("[Recomendador] Error enviando replanificacion a "
                    + tipoServicio + ": " + e.getMessage());
        }
    }

    private String decidirCriterioPorClima(List<InformePercepcion> fragmentos) {
        String textoClima = "";

        for (InformePercepcion informe : fragmentos) {
            if (informe.getFuente() != null
                    && informe.getFuente().toLowerCase().contains("clima")) {
                textoClima = informe.toString().toLowerCase();
                break;
            }
        }

        if (textoClima.contains("lluvia")
                || textoClima.contains("rain")
                || textoClima.contains("tormenta")
                || textoClima.contains("storm")
                || textoClima.contains("nieve")
                || textoClima.contains("snow")) {
            return "indoor";
        }

        if (textoClima.contains("claro")
                || textoClima.contains("soleado")
                || textoClima.contains("sun")
                || textoClima.contains("clear")) {
            return "outdoor";
        }

        return "indoor";
    }

    private void procesarYEnviar(
            String conversationId,
            List<InformePercepcion> fragmentos,
            List<InformeReplanificacion> informesReplanificacion
    ) {
        String rutaRecomendada = motor.generarRecomendacion(fragmentos);

        rutaRecomendada += construirResumenReplanificacion(informesReplanificacion);

        historialFragmentos.put(conversationId, fragmentos);
        historialReplanificacion.put(conversationId, informesReplanificacion);
        rutasGeneradas.put(conversationId, rutaRecomendada);
        intentosAlternativa.put(conversationId, 1);

        candidatosUsadosPorSesion.put(
                conversationId,
                motor.extraerNombresSeleccionados(fragmentos, 0, new ArrayList<>())
        );

        System.out.println("\n================ RUTA RECOMENDADA ================");
        System.out.println(rutaRecomendada);
        System.out.println("===================================================\n");

        enviarAlInterfaz(conversationId, rutaRecomendada);
    }

    private String construirResumenReplanificacion(
            List<InformeReplanificacion> informes
    ) {
        StringBuilder sb = new StringBuilder();


        for (InformeReplanificacion informe : informes) {

            if (informe.getLugares() != null && !informe.getLugares().isEmpty()) {
                sb.append("Lugares alternativos sugeridos:\n");
                informe.getLugares().stream().limit(3).forEach(lugar ->
                        sb.append("- ").append(lugar).append("\n")
                );
            }

            if (informe.getEventos() != null && !informe.getEventos().isEmpty()) {
                sb.append("Eventos alternativos sugeridos:\n");
                informe.getEventos().stream().limit(3).forEach(evento ->
                        sb.append("- ").append(evento).append("\n")
                );
            }
        }

        return sb.toString();
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