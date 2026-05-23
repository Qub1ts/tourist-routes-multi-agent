package com.sistemainteligentes.comun;

import java.io.Serializable;

/**
 * Resumen meteorologico que devuelve el AgenteClima. El campo
 * {@code resumen} esta pensado para mostrarse directamente al usuario; el
 * resto de campos es para que el AgenteRecomendador pueda razonar (p.e.
 * "si llueve, priorizar planes en interior").
 */
public class DatosClima implements Serializable {

    private static final long serialVersionUID = 1L;

    private String resumen;             // "Soleado, 24.5 grados"
    private String descripcion;         // "cielo claro"
    private double temperaturaActual;   // grados Celsius
    private double sensacionTermica;    // grados Celsius
    private int humedad;                // 0-100
    private double viento;              // m/s
    private double probabilidadLluvia;  // 0.0-1.0 (si la API no la da, 0.0)

    public DatosClima() {
    }

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getTemperaturaActual() {
        return temperaturaActual;
    }

    public void setTemperaturaActual(double temperaturaActual) {
        this.temperaturaActual = temperaturaActual;
    }

    public double getSensacionTermica() {
        return sensacionTermica;
    }

    public void setSensacionTermica(double sensacionTermica) {
        this.sensacionTermica = sensacionTermica;
    }

    public int getHumedad() {
        return humedad;
    }

    public void setHumedad(int humedad) {
        this.humedad = humedad;
    }

    public double getViento() {
        return viento;
    }

    public void setViento(double viento) {
        this.viento = viento;
    }

    public double getProbabilidadLluvia() {
        return probabilidadLluvia;
    }

    public void setProbabilidadLluvia(double probabilidadLluvia) {
        this.probabilidadLluvia = probabilidadLluvia;
    }

    @Override
    public String toString() {
        return resumen != null ? resumen : descripcion;
    }
}
