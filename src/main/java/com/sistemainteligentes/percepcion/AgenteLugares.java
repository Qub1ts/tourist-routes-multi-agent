package com.sistemainteligentes.percepcion;

import java.util.ArrayList;
import java.util.List;

import com.sistemainteligentes.comun.InformePercepcion;
import com.sistemainteligentes.comun.InformeReplanificacion;
import com.sistemainteligentes.comun.LugarTuristico;
import com.sistemainteligentes.comun.PreferenciasUsuario;
import com.sistemainteligentes.comun.SolicitudReplanificacion;

/**
 * Agente de percepcion especializado en lugares turisticos. Consume
 * OpenTripMap a traves de {@link FuenteLugares}.
 */
public class AgenteLugares extends BasePercepcionAgent {

    private static final long serialVersionUID = 1L;

    private final FuenteLugares fuente = new FuenteLugares();

    @Override
    protected String tipoServicioEspecifico() {
        return "percepcion-lugares";
    }

    @Override
    protected String nombreFuente() {
        return "lugares";
    }

    @Override
    protected InformePercepcion consultarFuente(PreferenciasUsuario preferencias) {
        return fuente.consultar(preferencias);
    }

    @Override
    protected InformeReplanificacion replanificarFuente(SolicitudReplanificacion solicitud) {
        System.out.println("[lugares] Replanificando lugares con criterio: "
            + solicitud.getCriterio());

        InformePercepcion informeBase = fuente.consultar(solicitud.getPreferencias());

        List<LugarTuristico> lugaresOriginales = informeBase.getLugares();
        List<LugarTuristico> lugaresFiltrados = new ArrayList<>();

        String criterio = solicitud.getCriterio() == null
            ? ""
            : solicitud.getCriterio().toLowerCase();

        for (LugarTuristico lugar : lugaresOriginales) {
            String texto = lugar.toString().toLowerCase();

            if ("indoor".equals(criterio)) {
                if (esLugarIndoor(texto)) {
                    lugaresFiltrados.add(lugar);
                }
            } else if ("outdoor".equals(criterio)) {
                if (esLugarOutdoor(texto)) {
                    lugaresFiltrados.add(lugar);
                }
            }
        }

        if (lugaresFiltrados.isEmpty()) {
            lugaresFiltrados.addAll(lugaresOriginales);
        }

        InformeReplanificacion informe = new InformeReplanificacion(
            "lugares",
            criterio,
            solicitud.getPreferencias()
        );

        informe.setLugares(lugaresFiltrados);
        informe.setMensaje("Lugares replanificados con criterio " + criterio
            + ". Total: " + lugaresFiltrados.size());

        return informe;
    }

    private boolean esLugarIndoor(String texto) {
        return texto.contains("museo")
            || texto.contains("museum")
            || texto.contains("galeria")
            || texto.contains("gallery")
            || texto.contains("exposicion")
            || texto.contains("arte")
            || texto.contains("palacio")
            || texto.contains("iglesia")
            || texto.contains("catedral")
            || texto.contains("mercado")
            || texto.contains("restaurante")
            || texto.contains("cafe");
    }

    private boolean esLugarOutdoor(String texto) {
        return texto.contains("parque")
            || texto.contains("park")
            || texto.contains("plaza")
            || texto.contains("jardin")
            || texto.contains("garden")
            || texto.contains("mirador")
            || texto.contains("calle")
            || texto.contains("puerta")
            || texto.contains("retiro")
            || texto.contains("rio")
            || texto.contains("outdoor");
    }
}