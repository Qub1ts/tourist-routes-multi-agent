package com.sistemainteligentes.percepcion;

import com.sistemainteligentes.comun.InformePercepcion;
import com.sistemainteligentes.comun.PreferenciasUsuario;

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
}
