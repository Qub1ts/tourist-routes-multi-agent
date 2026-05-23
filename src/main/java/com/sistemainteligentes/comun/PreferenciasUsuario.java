package com.sistemainteligentes.comun;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Preferencias que el AgenteUsuario envia al AgenteRecomendador para que este
 * calcule una ruta turistica. Se transmite como contenido serializado de un
 * mensaje ACL ({@code ACLMessage.setContentObject}).
 */
public class PreferenciasUsuario implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ciudad;
    private int diasDisponibles;
    private double presupuestoMaximo;
    private List<String> intereses;

    public PreferenciasUsuario() {
        this.intereses = new ArrayList<>();
    }

    public PreferenciasUsuario(String ciudad, int diasDisponibles,
                               double presupuestoMaximo, List<String> intereses) {
        this.ciudad = ciudad;
        this.diasDisponibles = diasDisponibles;
        this.presupuestoMaximo = presupuestoMaximo;
        this.intereses = intereses != null ? new ArrayList<>(intereses) : new ArrayList<>();
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public int getDiasDisponibles() {
        return diasDisponibles;
    }

    public void setDiasDisponibles(int diasDisponibles) {
        this.diasDisponibles = diasDisponibles;
    }

    public double getPresupuestoMaximo() {
        return presupuestoMaximo;
    }

    public void setPresupuestoMaximo(double presupuestoMaximo) {
        this.presupuestoMaximo = presupuestoMaximo;
    }

    public List<String> getIntereses() {
        return intereses;
    }

    public void setIntereses(List<String> intereses) {
        this.intereses = intereses != null ? new ArrayList<>(intereses) : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "PreferenciasUsuario{"
            + "ciudad='" + ciudad + '\''
            + ", diasDisponibles=" + diasDisponibles
            + ", presupuestoMaximo=" + presupuestoMaximo
            + ", intereses=" + intereses
            + '}';
    }
}
