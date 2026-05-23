package com.sistemainteligentes.comun;

import java.io.Serializable;

/**
 * Punto de interes devuelto por el AgentePercepcionWeb.
 * Se incluye dentro de un {@link ResultadoPercepcion}.
 */
public class LugarTuristico implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombre;
    private String tipo;          // p.e. "museo", "restaurante", "parque"
    private double precioEuros;   // 0.0 si es gratuito
    private String horario;       // texto libre, p.e. "10:00 - 20:00"
    private double valoracion;    // 0.0 - 5.0

    public LugarTuristico() {
    }

    public LugarTuristico(String nombre, String tipo, double precioEuros,
                          String horario, double valoracion) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.precioEuros = precioEuros;
        this.horario = horario;
        this.valoracion = valoracion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public double getPrecioEuros() {
        return precioEuros;
    }

    public void setPrecioEuros(double precioEuros) {
        this.precioEuros = precioEuros;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public double getValoracion() {
        return valoracion;
    }

    public void setValoracion(double valoracion) {
        this.valoracion = valoracion;
    }

    @Override
    public String toString() {
        return nombre + " (" + tipo + ", " + precioEuros + " EUR, " + horario
            + ", *" + valoracion + ")";
    }
}
