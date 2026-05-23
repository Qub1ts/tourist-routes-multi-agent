package com.sistemainteligentes.percepcion;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import com.sistemainteligentes.comun.InformePercepcion;
import com.sistemainteligentes.comun.PreferenciasUsuario;

/**
 * Clase base abstracta para todos los agentes de percepcion del sistema
 * (clima, lugares, hoteles, eventos...).
 *
 * Factoriza:
 *   - El registro en el DF con DOS servicios:
 *       * uno generico, {@value #SERVICIO_GENERICO}, que el AgenteUsuario
 *         busca para descubrir TODOS los proveedores de percepcion sin
 *         tener que conocerlos por nombre (fan-out automatico).
 *       * uno especifico (cada subclase decide su tipo), por si otro
 *         agente quiere consultar una sola fuente.
 *   - El comportamiento ciclico que escucha REQUEST con ontologia
 *     {@value #ONTOLOGIA_ENTRADA}, llama a {@link #consultarFuente(
 *     PreferenciasUsuario)} y reenvia el {@link InformePercepcion}
 *     resultante al AgenteRecomendador.
 *   - El cierre del ciclo de vida (deregister en {@code takeDown()}).
 *
 * Cada subclase concreta solo aporta tres cosas: su tipo de servicio
 * especifico, una etiqueta logica para los logs y la implementacion del
 * acceso a su API. Asi anadir una nueva fuente (p.e. transporte publico)
 * es trivial.
 */
public abstract class BasePercepcionAgent extends Agent {

    private static final long serialVersionUID = 1L;

    /** Tipo de servicio comun que registran TODOS los agentes de percepcion. */
    public static final String SERVICIO_GENERICO = "fuente-percepcion";

    /** Ontologia que usa el AgenteUsuario para pedir percepcion. */
    public static final String ONTOLOGIA_ENTRADA = "fuente-percepcion";

    /** Ontologia con la que se reenvia el informe al AgenteRecomendador. */
    public static final String ONTOLOGIA_SALIDA = "recomendar-ruta";

    /** Tipo de servicio que registrara el AgenteRecomendador. */
    public static final String SERVICIO_RECOMENDADOR = "recomendar-ruta";

    /**
     * Tipo de servicio especifico que registrara la subclase
     * (p.e. {@code "percepcion-clima"}). No puede ser {@code null}.
     */
    protected abstract String tipoServicioEspecifico();

    /**
     * Nombre corto de la fuente, usado para los logs y como valor del
     * campo {@link InformePercepcion#getFuente()}.
     * Ejemplos: {@code "clima"}, {@code "lugares"}, {@code "hoteles"}.
     */
    protected abstract String nombreFuente();

    /**
     * Punto de extension: la subclase implementa el acceso a su API y
     * devuelve el informe ya construido. Si la API falla, debe rellenar
     * {@link InformePercepcion#setErrorMensaje} y devolver el informe
     * (NO lanzar excepcion) para que el flujo siga.
     */
    protected abstract InformePercepcion consultarFuente(PreferenciasUsuario preferencias);

    @Override
    protected void setup() {
        System.out.println("[" + getLocalName() + "] Iniciado (" + nombreFuente() + ")");
        registrarServicios();
        addBehaviour(new AtenderConsultasBehaviour(this));
        System.out.println("[" + getLocalName() + "] Esperando peticiones "
            + "(servicios DF: '" + SERVICIO_GENERICO + "', '"
            + tipoServicioEspecifico() + "').");
    }

    private void registrarServicios() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription generico = new ServiceDescription();
        generico.setType(SERVICIO_GENERICO);
        generico.setName("Fuente de percepcion: " + nombreFuente());
        generico.addOntologies(ONTOLOGIA_ENTRADA);
        dfd.addServices(generico);

        ServiceDescription especifico = new ServiceDescription();
        especifico.setType(tipoServicioEspecifico());
        especifico.setName("Agente de " + nombreFuente());
        especifico.addOntologies(ONTOLOGIA_ENTRADA);
        dfd.addServices(especifico);

        try {
            DFService.register(this, dfd);
            System.out.println("[" + getLocalName() + "] Servicios registrados en el DF.");
        } catch (FIPAException e) {
            System.err.println("[" + getLocalName() + "] Fallo al registrar en DF: "
                + e.getMessage());
            doDelete();
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            System.err.println("[" + getLocalName() + "] Fallo al desregistrar: "
                + e.getMessage());
        }
        System.out.println("[" + getLocalName() + "] Finalizado.");
    }
}
