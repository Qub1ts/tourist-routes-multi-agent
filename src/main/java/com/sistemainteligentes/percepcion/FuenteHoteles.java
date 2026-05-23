package com.sistemainteligentes.percepcion;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sistemainteligentes.comun.Hotel;
import com.sistemainteligentes.comun.InformePercepcion;
import com.sistemainteligentes.comun.PreferenciasUsuario;

/**
 * Cliente del agregador de Booking.com publicado en RapidAPI:
 *   https://rapidapi.com/DataCrawler/api/booking-com15
 *
 * RapidAPI funciona con dos cabeceras fijas:
 *   X-RapidAPI-Key:  <clave del usuario>
 *   X-RapidAPI-Host: booking-com15.p.rapidapi.com
 *
 * Flujo:
 *   1. /api/v1/hotels/searchDestination?query={ciudad}
 *      devuelve un dest_id y un search_type para identificar la ciudad.
 *   2. /api/v1/hotels/searchHotels?dest_id=...&search_type=...
 *      con fechas de check-in/check-out calculadas a partir de
 *      preferencias.diasDisponibles (por defecto, hoy +1 -> hoy +1+dias).
 *
 * Si la clave RAPIDAPI_KEY no esta configurada, o si la API responde
 * con 4xx/5xx, o si el JSON tiene una estructura inesperada, el agente
 * cae al catalogo simulado para Madrid y Barcelona.
 */
final class FuenteHoteles {

    private static final String HOST = "booking-com15.p.rapidapi.com";
    private static final String URL_BUSCAR_DESTINO =
        "https://booking-com15.p.rapidapi.com/api/v1/hotels/searchDestination";
    private static final String URL_BUSCAR_HOTELES =
        "https://booking-com15.p.rapidapi.com/api/v1/hotels/searchHotels";
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int LIMITE = 10;

    private final ClienteHttp http = new ClienteHttp();

    InformePercepcion consultar(PreferenciasUsuario prefs) {
        String ciudad = prefs.getCiudad() != null ? prefs.getCiudad() : "Desconocida";
        InformePercepcion informe = new InformePercepcion("hoteles", ciudad, prefs);

        String key = ConfiguracionApis.claveRapidApi();
        if (key.isBlank()) {
            informe.setHoteles(simular(ciudad));
            informe.setErrorMensaje("Sin RAPIDAPI_KEY; usando catalogo simulado.");
            return informe;
        }
        try {
            informe.setHoteles(consultarReal(prefs, key));
        } catch (Exception e) {
            System.err.println("[FuenteHoteles] Fallback simulado (" + e.getMessage() + ")");
            informe.setHoteles(simular(ciudad));
            informe.setErrorMensaje("Booking/RapidAPI fallo: " + e.getMessage());
        }
        return informe;
    }

    private List<Hotel> consultarReal(PreferenciasUsuario prefs, String key) throws Exception {
        // 1. Resolver dest_id + search_type a partir del nombre de la ciudad.
        String urlDestino = URL_BUSCAR_DESTINO
            + "?query=" + ClienteHttp.urlEncode(prefs.getCiudad());
        String bodyDestino = http.get(urlDestino,
            "X-RapidAPI-Key", key,
            "X-RapidAPI-Host", HOST);
        JSONObject jsonDestino = new JSONObject(bodyDestino);
        if (!jsonDestino.optBoolean("status", false)) {
            throw new RuntimeException("Booking destination: "
                + jsonDestino.optString("message", "respuesta sin status"));
        }
        JSONArray destinos = jsonDestino.optJSONArray("data");
        if (destinos == null || destinos.length() == 0) {
            throw new RuntimeException("Booking: ciudad '" + prefs.getCiudad()
                + "' no encontrada");
        }
        JSONObject destino = destinos.getJSONObject(0);
        String destId = destino.optString("dest_id", "");
        String searchType = destino.optString("search_type", "city");
        if (destId.isBlank()) {
            throw new RuntimeException("Booking: dest_id vacio");
        }

        // 2. Buscar hoteles para las fechas estimadas.
        LocalDate llegada = LocalDate.now().plusDays(1);
        int dias = Math.max(1, prefs.getDiasDisponibles());
        LocalDate salida = llegada.plusDays(dias);

        String urlHoteles = URL_BUSCAR_HOTELES
            + "?dest_id=" + ClienteHttp.urlEncode(destId)
            + "&search_type=" + ClienteHttp.urlEncode(searchType)
            + "&arrival_date=" + llegada.format(FMT_FECHA)
            + "&departure_date=" + salida.format(FMT_FECHA)
            + "&adults=1&room_qty=1&page_number=1"
            + "&units=metric&currency_code=EUR&languagecode=es"
            + "&temperature_unit=c";
        String bodyHoteles = http.get(urlHoteles,
            "X-RapidAPI-Key", key,
            "X-RapidAPI-Host", HOST);
        JSONObject jsonHoteles = new JSONObject(bodyHoteles);
        if (!jsonHoteles.optBoolean("status", false)) {
            throw new RuntimeException("Booking searchHotels: "
                + jsonHoteles.optString("message", "respuesta sin status"));
        }
        JSONObject data = jsonHoteles.optJSONObject("data");
        if (data == null) {
            return Collections.emptyList();
        }
        JSONArray hotelesArr = data.optJSONArray("hotels");
        if (hotelesArr == null) {
            return Collections.emptyList();
        }

        List<Hotel> resultado = new ArrayList<>();
        int n = Math.min(LIMITE, hotelesArr.length());
        for (int i = 0; i < n; i++) {
            JSONObject prop = hotelesArr.getJSONObject(i).optJSONObject("property");
            if (prop == null) continue;
            String nombre = prop.optString("name", "").trim();
            if (nombre.isBlank()) continue;

            String direccion = prop.optString("wishlistName",
                prop.optString("city_in_trans", "(centro)"));
            int estrellas = prop.optInt("propertyClass",
                prop.optInt("accuratePropertyClass", 0));
            double valoracion = prop.optDouble("reviewScore", 0) / 2.0; // 0-10 -> 0-5
            double precioNoche = extraerPrecio(prop, dias);
            // RapidAPI Booking no devuelve URL de reserva directa.
            resultado.add(new Hotel(nombre, direccion, precioNoche,
                estrellas, valoracion, null));
        }
        return resultado;
    }

    /**
     * El objeto priceBreakdown.grossPrice contiene el precio TOTAL para
     * toda la estancia. Lo dividimos por noches para que el recomendador
     * pueda comparar facilmente con el presupuesto del usuario.
     */
    private double extraerPrecio(JSONObject prop, int noches) {
        JSONObject pb = prop.optJSONObject("priceBreakdown");
        if (pb == null) return 0;
        JSONObject gross = pb.optJSONObject("grossPrice");
        if (gross == null) return 0;
        double total = gross.optDouble("value", 0);
        return noches > 0 ? Math.round(total / noches * 100.0) / 100.0 : total;
    }

    private List<Hotel> simular(String ciudad) {
        switch (ciudad.toLowerCase(Locale.ROOT)) {
            case "madrid":
                return Arrays.asList(
                    new Hotel("Hotel Atocha Gran Via", "Calle de Atocha 23", 85, 3, 4.2, null),
                    new Hotel("Petit Palace Puerta del Sol", "Centro", 110, 4, 4.5, null),
                    new Hotel("Hostal Madrid Centro", "Gran Via 48", 55, 2, 3.8, null));
            case "barcelona":
                return Arrays.asList(
                    new Hotel("Catalonia Plaza Catalunya", "Centro", 130, 4, 4.4, null),
                    new Hotel("Praktik Rambla", "La Rambla", 90, 3, 4.1, null));
            default:
                return Collections.emptyList();
        }
    }
}
