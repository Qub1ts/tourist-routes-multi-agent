package com.sistemainteligentes.recomendador;

public class CandidatoRuta implements Comparable<CandidatoRuta> {

    public enum Tipo {
        LUGAR,
        HOTEL,
        EVENTO
    }

    private final Tipo tipo;
    private final Object elementoOriginal;
    private final String nombre;
    private final String descripcion;
    private final double costeEstimado;

    private double scoreIntereses;
    private double scorePresupuesto;
    private double scoreClima;
    private double scorePopularidad;
    private double scoreDiversidad;
    private double scoreFinal;

    public CandidatoRuta(Tipo tipo, Object elementoOriginal, String nombre, String descripcion, double costeEstimado) {
        this.tipo = tipo;
        this.elementoOriginal = elementoOriginal;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.costeEstimado = costeEstimado;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public Object getElementoOriginal() {
        return elementoOriginal;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public double getCosteEstimado() {
        return costeEstimado;
    }

    public double getScoreIntereses() {
        return scoreIntereses;
    }

    public void setScoreIntereses(double scoreIntereses) {
        this.scoreIntereses = scoreIntereses;
    }

    public double getScorePresupuesto() {
        return scorePresupuesto;
    }

    public void setScorePresupuesto(double scorePresupuesto) {
        this.scorePresupuesto = scorePresupuesto;
    }

    public double getScoreClima() {
        return scoreClima;
    }

    public void setScoreClima(double scoreClima) {
        this.scoreClima = scoreClima;
    }

    public double getScorePopularidad() {
        return scorePopularidad;
    }

    public void setScorePopularidad(double scorePopularidad) {
        this.scorePopularidad = scorePopularidad;
    }

    public double getScoreDiversidad() {
        return scoreDiversidad;
    }

    public void setScoreDiversidad(double scoreDiversidad) {
        this.scoreDiversidad = scoreDiversidad;
    }

    public double getScoreFinal() {
        return scoreFinal;
    }

    public void calcularScoreFinal() {
        this.scoreFinal =
                0.45 * scoreIntereses +
                0.25 * scorePresupuesto +
                0.15 * scoreClima +
                0.10 * scorePopularidad +
                0.05 * scoreDiversidad;
    }

    @Override
    public int compareTo(CandidatoRuta otro) {
        return Double.compare(otro.scoreFinal, this.scoreFinal);
    }

    @Override
    public String toString() {
        return nombre + " | tipo=" + tipo
                + " | coste=" + costeEstimado
                + " | score=" + String.format("%.3f", scoreFinal);
    }
}