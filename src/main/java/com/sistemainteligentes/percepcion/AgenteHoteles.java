package com.sistemainteligentes.percepcion;

import com.sistemainteligentes.comun.InformePercepcion;
import com.sistemainteligentes.comun.PreferenciasUsuario;

/**
 * Agente de percepcion especializado en hoteles. Consume el agregador
 * de Booking.com publicado en RapidAPI (host
 * {@code booking-com15.p.rapidapi.com}) a traves de {@link FuenteHoteles}.
 */
public class AgenteHoteles extends BasePercepcionAgent {

    private static final long serialVersionUID = 1L;

    private final FuenteHoteles fuente = new FuenteHoteles();

    @Override
    protected String tipoServicioEspecifico() {
        return "percepcion-hoteles";
    }

    @Override
    protected String nombreFuente() {
        return "hoteles";
    }

    @Override
    protected InformePercepcion consultarFuente(PreferenciasUsuario preferencias) {
        return fuente.consultar(preferencias);
    }
}
