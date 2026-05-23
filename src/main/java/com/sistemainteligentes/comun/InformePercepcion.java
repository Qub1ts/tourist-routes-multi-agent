package com.sistemainteligentes.comun;

import java.io.Serializable;
import java.util.List;

/**
 * Fragmento de informacion que envia un agente de percepcion al
 * AgenteRecomendador. Cada agente solo rellena el campo que le
 * corresponde a su fuente, los demas quedan a null. El recomendador
 * acumula los fragmentos que comparten {@code conversationId} (campo
 * del mensaje ACL) y, cuando tiene los que necesita, decide la ruta.
 *
 * Contrato con el recomendador:
 *
 *   ontologia ACL: "recomendar-ruta"
 *   performativa:  REQUEST  (la respuesta del recomendador va al
 *                            AgenteInterfaz con ontologia "mostrar-ruta"
 *                            y al AgenteUsuario con "recomendar-ruta")
 *   content:       InformePercepcion (este objeto, serializable)
 *
 * Campo {@code fuente} identifica el tipo de fragmento:
 *   - "clima"    -> {@link #datosClima} relleno
 *   - "lugares"  -> {@link #lugares} relleno
 *   - "hoteles"  -> {@link #hoteles} relleno
 *   - "eventos"  -> {@link #eventos} relleno
 *
 * Si la consulta a la API fallo, el agente puede mandar el informe con
 * los datos especificos a null y {@link #errorMensaje} explicando el
 * motivo, asi el recomendador sabe que esa fuente no llegara.
 */
public class InformePercepcion implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fuente;
    private String ciudad;
    private PreferenciasUsuario preferencias;
    private DatosClima datosClima;
    private List<LugarTuristico> lugares;
    private List<Hotel> hoteles;
    private List<EventoTuristico> eventos;
    private String errorMensaje;

    public InformePercepcion() {
    }

    public InformePercepcion(String fuente, String ciudad,
                             PreferenciasUsuario preferencias) {
        this.fuente = fuente;
        this.ciudad = ciudad;
        this.preferencias = preferencias;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public PreferenciasUsuario getPreferencias() {
        return preferencias;
    }

    public void setPreferencias(PreferenciasUsuario preferencias) {
        this.preferencias = preferencias;
    }

    public DatosClima getDatosClima() {
        return datosClima;
    }

    public void setDatosClima(DatosClima datosClima) {
        this.datosClima = datosClima;
    }

    public List<LugarTuristico> getLugares() {
        return lugares;
    }

    public void setLugares(List<LugarTuristico> lugares) {
        this.lugares = lugares;
    }

    public List<Hotel> getHoteles() {
        return hoteles;
    }

    public void setHoteles(List<Hotel> hoteles) {
        this.hoteles = hoteles;
    }

    public List<EventoTuristico> getEventos() {
        return eventos;
    }

    public void setEventos(List<EventoTuristico> eventos) {
        this.eventos = eventos;
    }

    public String getErrorMensaje() {
        return errorMensaje;
    }

    public void setErrorMensaje(String errorMensaje) {
        this.errorMensaje = errorMensaje;
    }

    @Override
    public String toString() {
        int n = 0;
        if (datosClima != null) n++;
        if (lugares != null) n += lugares.size();
        if (hoteles != null) n += hoteles.size();
        if (eventos != null) n += eventos.size();
        return "InformePercepcion{fuente=" + fuente
            + ", ciudad=" + ciudad
            + ", elementos=" + n
            + (errorMensaje != null ? ", ERROR=" + errorMensaje : "")
            + '}';
    }
}
