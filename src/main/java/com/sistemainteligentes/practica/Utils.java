package com.sistemainteligentes.practica;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

public class Utils {

    /**
     * Método auxiliar para enviar un mensaje de texto simple a otro agente.
     *
     * @param emisor         El agente que envía el mensaje (normalmente 'this' o 'myAgent')
     * @param nombreReceptor El nombre local del agente que recibirá el mensaje (ej: "buscar")
     * @param contenido      El texto que se desea enviar
     */
    public static void enviarMensaje(Agent emisor, String nombreReceptor, String contenido) {

        // 1. Creamos el mensaje. Usamos REQUEST porque le estamos pidiendo una acción (buscar)
        // (Si en tu práctica te exigen usar INFORM, cámbialo a ACLMessage.INFORM)
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);

        // 2. Añadimos el destinatario. Usamos AID.ISLOCALNAME porque le pasamos solo el nombre ("buscar")
        msg.addReceiver(new AID(nombreReceptor, AID.ISLOCALNAME));

        // 3. Añadimos el contenido (el texto que el usuario escribió)
        msg.setContent(contenido);

        // 4. El agente emisor envía el mensaje
        emisor.send(msg);

        System.out.println("Mensaje enviado a '" + nombreReceptor + "' con el texto: " + contenido);
    }
}
