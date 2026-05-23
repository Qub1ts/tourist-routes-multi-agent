package com.sistemainteligentes.comun;

import java.io.Serializable;

/**
 * Alojamiento devuelto por el AgenteHoteles. El AgenteRecomendador lo usa
 * para ajustar el presupuesto y ofrecer opcion de pernocta.
 */
public class Hotel implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombre;
    private String direccion;
    private double precioEurosNoche;   // 0 si no hay tarifa disponible
    private int estrellas;             // 1-5, 0 si no hay info
    private double valoracion;         // 0.0-5.0
    private String urlReserva;         // opcional

    public Hotel() {
    }

    public Hotel(String nombre, String direccion, double precioEurosNoche,
                 int estrellas, double valoracion, String urlReserva) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.precioEurosNoche = precioEurosNoche;
        this.estrellas = estrellas;
        this.valoracion = valoracion;
        this.urlReserva = urlReserva;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public double getPrecioEurosNoche() {
        return precioEurosNoche;
    }

    public void setPrecioEurosNoche(double precioEurosNoche) {
        this.precioEurosNoche = precioEurosNoche;
    }

    public int getEstrellas() {
        return estrellas;
    }

    public void setEstrellas(int estrellas) {
        this.estrellas = estrellas;
    }

    public double getValoracion() {
        return valoracion;
    }

    public void setValoracion(double valoracion) {
        this.valoracion = valoracion;
    }

    public String getUrlReserva() {
        return urlReserva;
    }

    public void setUrlReserva(String urlReserva) {
        this.urlReserva = urlReserva;
    }

    @Override
    public String toString() {
        return nombre + " (" + estrellas + "*, " + precioEurosNoche + " EUR/noche)";
    }
}
