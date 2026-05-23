package com.sistemainteligentes.comun;

import java.io.Serializable;

/**
 * Evento (concierto, exposicion, festival...) devuelto por el
 * AgenteEventos. El AgenteRecomendador puede incluirlo en la ruta si
 * coincide con las fechas del usuario y entra en su presupuesto.
 */
public class EventoTuristico implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombre;
    private String tipo;             // "concierto", "exposicion", "festival"...
    private String fechaInicio;      // ISO-8601, ej "2026-06-15T20:30:00Z"
    private String fechaFin;         // ISO-8601, opcional
    private String lugar;            // venue / sala / recinto
    private double precioMinimoEuros;
    private double precioMaximoEuros;
    private String urlEntradas;      // opcional

    public EventoTuristico() {
    }

    public EventoTuristico(String nombre, String tipo, String fechaInicio,
                           String fechaFin, String lugar,
                           double precioMinimoEuros, double precioMaximoEuros,
                           String urlEntradas) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.lugar = lugar;
        this.precioMinimoEuros = precioMinimoEuros;
        this.precioMaximoEuros = precioMaximoEuros;
        this.urlEntradas = urlEntradas;
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

    public String getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(String fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public String getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(String fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getLugar() {
        return lugar;
    }

    public void setLugar(String lugar) {
        this.lugar = lugar;
    }

    public double getPrecioMinimoEuros() {
        return precioMinimoEuros;
    }

    public void setPrecioMinimoEuros(double precioMinimoEuros) {
        this.precioMinimoEuros = precioMinimoEuros;
    }

    public double getPrecioMaximoEuros() {
        return precioMaximoEuros;
    }

    public void setPrecioMaximoEuros(double precioMaximoEuros) {
        this.precioMaximoEuros = precioMaximoEuros;
    }

    public String getUrlEntradas() {
        return urlEntradas;
    }

    public void setUrlEntradas(String urlEntradas) {
        this.urlEntradas = urlEntradas;
    }

    @Override
    public String toString() {
        return nombre + " (" + tipo + " @ " + lugar + ", " + fechaInicio + ")";
    }
}
