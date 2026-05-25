package com.sistemainteligentes.comun;

import java.io.Serializable;

public class SolicitudReplanificacion implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ciudad;
    private String criterio; // "indoor" u "outdoor"
    private PreferenciasUsuario preferencias;

    public SolicitudReplanificacion(String ciudad, String criterio, PreferenciasUsuario preferencias) {
        this.ciudad = ciudad;
        this.criterio = criterio;
        this.preferencias = preferencias;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getCriterio() {
        return criterio;
    }

    public PreferenciasUsuario getPreferencias() {
        return preferencias;
    }

    @Override
    public String toString() {
        return "SolicitudReplanificacion{" +
                "ciudad='" + ciudad + '\'' +
                ", criterio='" + criterio + '\'' +
                ", preferencias=" + preferencias +
                '}';
    }
}