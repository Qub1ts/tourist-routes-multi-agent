package com.sistemainteligentes.percepcion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Resuelve las API keys que usan los agentes de percepcion. Busca cada
 * clave en dos sitios:
 *
 *   1. Variable de entorno.
 *   2. Si no esta, en el fichero {@code apikeys.properties} en la raiz
 *      del proyecto. Ese fichero esta en {@code .gitignore}.
 *
 * Si tampoco esta ahi devuelve cadena vacia, y el agente cae a su modo
 * simulado de respaldo.
 *
 * <h3>Claves soportadas</h3>
 * <ul>
 *   <li>{@code OPENWEATHER_API_KEY} / {@code openweather.api.key}</li>
 *   <li>{@code OPENTRIPMAP_API_KEY} / {@code opentripmap.api.key}</li>
 *   <li>{@code RAPIDAPI_KEY} / {@code rapidapi.key} (Booking.com via RapidAPI)</li>
 *   <li>{@code TICKETMASTER_API_KEY} / {@code ticketmaster.api.key}</li>
 * </ul>
 */
public final class ConfiguracionApis {

    private static final String FICHERO_PROPS = "apikeys.properties";
    private static final Properties PROPS = cargar();

    private ConfiguracionApis() {
    }

    public static String claveOpenWeather() {
        return resolver("OPENWEATHER_API_KEY", "openweather.api.key");
    }

    public static String claveOpenTripMap() {
        return resolver("OPENTRIPMAP_API_KEY", "opentripmap.api.key");
    }

    public static String claveRapidApi() {
        return resolver("RAPIDAPI_KEY", "rapidapi.key");
    }

    public static String claveTicketmaster() {
        return resolver("TICKETMASTER_API_KEY", "ticketmaster.api.key");
    }

    private static String resolver(String env, String prop) {
        String desdeEntorno = System.getenv(env);
        if (desdeEntorno != null && !desdeEntorno.isBlank()) {
            return desdeEntorno.trim();
        }
        String desdeFichero = PROPS.getProperty(prop);
        return desdeFichero != null ? desdeFichero.trim() : "";
    }

    private static Properties cargar() {
        Properties p = new Properties();
        Path ruta = Paths.get(FICHERO_PROPS);
        if (Files.isReadable(ruta)) {
            try (InputStream is = Files.newInputStream(ruta)) {
                p.load(is);
            } catch (IOException e) {
                System.err.println("[ConfiguracionApis] No se pudo leer "
                    + FICHERO_PROPS + ": " + e.getMessage());
            }
        }
        return p;
    }
}
