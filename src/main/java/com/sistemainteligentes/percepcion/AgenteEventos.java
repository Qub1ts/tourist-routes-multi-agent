package com.sistemainteligentes.percepcion;

import com.sistemainteligentes.comun.InformePercepcion;
import com.sistemainteligentes.comun.PreferenciasUsuario;

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
}
