package com.sistemainteligentes;

/**
 * Punto de entrada informativo del proyecto.
 *
 * El sistema multiagente se lanza con jade.Boot (ver README.md), no a traves
 * de esta clase. Esta clase solo se conserva porque Maven la genera por
 * defecto y para imprimir un recordatorio de como ejecutar el sistema.
 */
public final class App {
    private App() {
    }

    public static void main(String[] args) {
        System.out.println("Proyecto multiagente JADE - Recomendador de rutas turisticas");
        System.out.println("Ejecutar la plataforma JADE con jade.Boot. Ver README.md.");
    }
}
