package com.sistemainteligentes.percepcion;

import com.sistemainteligentes.comun.InformePercepcion;
import com.sistemainteligentes.comun.PreferenciasUsuario;

/**
 * Agente de percepcion especializado en clima. Consume OpenWeatherMap a
 * traves de {@link FuenteClima}.
 *
 * Servicios DF:
 *   - {@code fuente-percepcion}  (generico, para que el usuario descubra)
 *   - {@code percepcion-clima}   (especifico, por si el recomendador
 *                                  quiere consultar solo el clima)
 */
public class AgenteClima extends BasePercepcionAgent {

    private static final long serialVersionUID = 1L;

    private final FuenteClima fuente = new FuenteClima();

    @Override
    protected String tipoServicioEspecifico() {
        return "percepcion-clima";
    }

    @Override
    protected String nombreFuente() {
        return "clima";
    }

    @Override
    protected InformePercepcion consultarFuente(PreferenciasUsuario preferencias) {
        return fuente.consultar(preferencias);
    }
}
