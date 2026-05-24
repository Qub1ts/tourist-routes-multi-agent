package com.sistemainteligentes.comun;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class InformeReplanificacion implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fuente; // "lugares" o "eventos"
    private String criterio; // "indoor" u "outdoor"
    private PreferenciasUsuario preferencias;

    private List<LugarTuristico> lugares = new ArrayList<>();
    private List<EventoTuristico> eventos = new ArrayList<>();

    private String mensaje;

    public InformeReplanificacion(String fuente, String criterio, PreferenciasUsuario preferencias) {
        this.fuente = fuente;
        this.criterio = criterio;
        this.preferencias = preferencias;
    }

    public String getFuente() {
        return fuente;
    }

    public String getCriterio() {
        return criterio;
    }

    public PreferenciasUsuario getPreferencias() {
        return preferencias;
    }

    public List<LugarTuristico> getLugares() {
        return lugares;
    }

    public void setLugares(List<LugarTuristico> lugares) {
        this.lugares = lugares;
    }

    public List<EventoTuristico> getEventos() {
        return eventos;
    }

    public void setEventos(List<EventoTuristico> eventos) {
        this.eventos = eventos;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    @Override
    public String toString() {
        return "InformeReplanificacion{" +
                "fuente='" + fuente + '\'' +
                ", criterio='" + criterio + '\'' +
                ", lugares=" + lugares.size() +
                ", eventos=" + eventos.size() +
                ", mensaje='" + mensaje + '\'' +
                '}';
    }
}