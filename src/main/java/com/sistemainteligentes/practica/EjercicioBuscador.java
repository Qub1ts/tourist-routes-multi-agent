package com.sistemainteligentes.practica;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.util.List;
import java.util.Scanner;

// 1. IMPORTANTE: La clase debe heredar de Agent
public class EjercicioBuscador extends Agent {

    private ComportamientoUsuario cu;
    // 2. Usar un único Scanner para toda la vida del agente (evita fugas de memoria)
    private transient Scanner scanner;

    @Override
    protected void setup() {
        System.out.println("Soy el agente Cliente: " + getLocalName());
        scanner = new Scanner(System.in);
        cu = new ComportamientoUsuario();
        addBehaviour(cu);
    }

    @Override
    protected void takeDown() {
        // 3. Buenas prácticas: limpiar recursos al terminar el agente
        if (scanner != null) {
            scanner.close();
        }
        System.out.println("Agente Cliente " + getLocalName() + " terminando.");
    }

    // Comportamiento cíclico interno
    class ComportamientoUsuario extends CyclicBehaviour {

        @SuppressWarnings("unchecked") // Evita la advertencia del compilador al hacer el casting
        @Override
        public void action() {
            System.out.print("\nIntroduzca el texto a buscar: ");

            // Leemos el texto
            String temp = scanner.nextLine();

            // 4. Evitar búsquedas vacías
            if (temp == null || temp.trim().isEmpty()) {
                System.out.println("Por favor, introduzca un texto válido.");
                return; // Reinicia el ciclo
            }

            // Enviamos el mensaje al agente buscador
            Utils.enviarMensaje(myAgent, "buscar", temp);

            // 5. Esperamos la respuesta
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.blockingReceive(mt);

            if (msg != null) {
                try {
                    // Extraemos y mostramos los resultados
                    List<String> mensajes = (List<String>) msg.getContentObject();

                    System.out.println("--- Resultados de la búsqueda ---");
                    if (mensajes.isEmpty()) {
                        System.out.println("No se encontraron resultados.");
                    } else {
                        // Bucle for-each (más limpio y moderno)
                        for (String mensaje : mensajes) {
                            System.out.println("- " + mensaje);
                        }
                    }
                    System.out.println("---------------------------------");

                } catch (UnreadableException e) {
                    System.err.println("Error al leer el contenido del mensaje.");
                    e.printStackTrace();
                } catch (ClassCastException e) {
                    // Protege el programa si el Agente Buscador devuelve algo que no es un List<String>
                    System.err.println("El formato de la respuesta no es una lista de textos.");
                }
            }
        }
    }
}
