package com.sistemainteligentes.percepcion;

import java.util.ArrayList;
import java.util.List;

import com.sistemainteligentes.comun.EventoTuristico;
import com.sistemainteligentes.comun.InformePercepcion;
import com.sistemainteligentes.comun.InformeReplanificacion;
import com.sistemainteligentes.comun.PreferenciasUsuario;
import com.sistemainteligentes.comun.SolicitudReplanificacion;

/**
 * Agente de percepcion especializado en eventos. Consume Ticketmaster
 * Discovery API a traves de {@link FuenteEventos}.
 */
public class AgenteEventos extends BasePercepcionAgent {

    private static final long serialVersionUID = 1L;

    private final FuenteEventos fuente = new FuenteEventos();

    @Override
    protected String tipoServicioEspecifico() {
        return "percepcion-eventos";
    }

    @Override
    protected String nombreFuente() {
        return "eventos";
    }

    @Override
    protected InformePercepcion consultarFuente(PreferenciasUsuario preferencias) {
        return fuente.consultar(preferencias);
    }

    @Override
    protected InformeReplanificacion replanificarFuente(SolicitudReplanificacion solicitud) {
        System.out.println("[eventos] Replanificando eventos con criterio: "
            + solicitud.getCriterio());

        InformePercepcion informeBase = fuente.consultar(solicitud.getPreferencias());

        List<EventoTuristico> eventosOriginales = informeBase.getEventos();
        List<EventoTuristico> eventosFiltrados = new ArrayList<>();

        String criterio = solicitud.getCriterio() == null
            ? ""
            : solicitud.getCriterio().toLowerCase();

        for (EventoTuristico evento : eventosOriginales) {
            String texto = evento.toString().toLowerCase();

            if ("indoor".equals(criterio)) {
                if (esEventoIndoor(texto)) {
                    eventosFiltrados.add(evento);
                }
            } else if ("outdoor".equals(criterio)) {
                if (esEventoOutdoor(texto)) {
                    eventosFiltrados.add(evento);
                }
            }
        }

        if (eventosFiltrados.isEmpty()) {
            eventosFiltrados.addAll(eventosOriginales);
        }

        InformeReplanificacion informe = new InformeReplanificacion(
            "eventos",
            criterio,
            solicitud.getPreferencias()
        );

        informe.setEventos(eventosFiltrados);
        informe.setMensaje("Eventos replanificados con criterio " + criterio
            + ". Total: " + eventosFiltrados.size());

        return informe;
    }

    private boolean esEventoIndoor(String texto) {
        return texto.contains("museo")
            || texto.contains("exposicion")
            || texto.contains("exhibition")
            || texto.contains("teatro")
            || texto.contains("theatre")
            || texto.contains("concierto")
            || texto.contains("concert")
            || texto.contains("sala")
            || texto.contains("auditorio")
            || texto.contains("indoor");
    }

    private boolean esEventoOutdoor(String texto) {
        return texto.contains("festival")
            || texto.contains("aire libre")
            || texto.contains("outdoor")
            || texto.contains("parque")
            || texto.contains("plaza")
            || texto.contains("calle")
            || texto.contains("estadio")
            || texto.contains("recorrido");
    }
}