package com.sistemainteligentes.percepcion;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import com.sistemainteligentes.comun.InformePercepcion;
import com.sistemainteligentes.comun.InformeReplanificacion;
import com.sistemainteligentes.comun.PreferenciasUsuario;
import com.sistemainteligentes.comun.SolicitudReplanificacion;

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
 *   - El comportamiento ciclico que escucha:
 *       * REQUEST con ontologia {@value #ONTOLOGIA_ENTRADA}, para la
 *         consulta inicial de percepcion.
 *       * REQUEST con ontologia {@value #ONTOLOGIA_REPLANIFICACION}, para
 *         solicitudes de reajuste dinamico de la ruta.
 *   - El reenvio del {@link InformePercepcion} inicial al
 *     AgenteRecomendador.
 *   - La respuesta con {@link InformeReplanificacion} cuando el
 *     recomendador solicita alternativas indoor/outdoor.
 *   - El cierre del ciclo de vida (deregister en {@code takeDown()}).
 *
 * Cada subclase concreta solo aporta:
 *   - su tipo de servicio especifico,
 *   - una etiqueta logica para los logs,
 *   - la implementacion del acceso a su API,
 *   - opcionalmente una estrategia de replanificacion.
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

    /** Ontologia usada por el AgenteRecomendador para pedir alternativas. */
    public static final String ONTOLOGIA_REPLANIFICACION = "replanificar-ruta";

    /** Ontologia usada por los agentes de percepcion para responder alternativas. */
    public static final String ONTOLOGIA_RESPUESTA_REPLANIFICACION =
        "respuesta-replanificacion";

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
    protected abstract InformePercepcion consultarFuente(
        PreferenciasUsuario preferencias);

    /**
     * Punto de extension opcional para replanificacion.
     *
     * Por defecto devuelve un informe vacio indicando que este agente no
     * implementa replanificacion especifica. Las subclases que si tengan
     * sentido en la replanificacion, por ejemplo AgenteLugares y
     * AgenteEventos, deben sobrescribir este metodo.
     */
    protected InformeReplanificacion replanificarFuente(
        SolicitudReplanificacion solicitud) {

        InformeReplanificacion informe = new InformeReplanificacion(
            nombreFuente(),
            solicitud.getCriterio(),
            solicitud.getPreferencias()
        );

        informe.setMensaje("El agente '" + nombreFuente()
            + "' no implementa replanificacion especifica.");

        return informe;
    }

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
        generico.addOntologies(ONTOLOGIA_REPLANIFICACION);
        dfd.addServices(generico);

        ServiceDescription especifico = new ServiceDescription();
        especifico.setType(tipoServicioEspecifico());
        especifico.setName("Agente de " + nombreFuente());
        especifico.addOntologies(ONTOLOGIA_ENTRADA);
        especifico.addOntologies(ONTOLOGIA_REPLANIFICACION);
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