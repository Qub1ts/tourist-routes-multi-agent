package com.sistemainteligentes.percepcion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sistemainteligentes.comun.EventoTuristico;
import com.sistemainteligentes.comun.InformePercepcion;
import com.sistemainteligentes.comun.PreferenciasUsuario;

/**
 * Cliente de Ticketmaster Discovery API
 * (https://developer.ticketmaster.com/products-and-docs/apis/discovery-api).
 *
 * Busca eventos en la ciudad indicada en las preferencias. Cae a un
 * catalogo simulado si no hay clave o si la llamada falla.
 */
final class FuenteEventos {

    private static final String URL_BASE =
        "https://app.ticketmaster.com/discovery/v2/events.json";
    private static final int LIMITE = 15;

    private final ClienteHttp http = new ClienteHttp();

    InformePercepcion consultar(PreferenciasUsuario prefs) {
        String ciudad = prefs.getCiudad() != null ? prefs.getCiudad() : "Desconocida";
        InformePercepcion informe = new InformePercepcion("eventos", ciudad, prefs);

        String key = ConfiguracionApis.claveTicketmaster();
        if (key.isBlank()) {
            informe.setEventos(simular(ciudad));
            informe.setErrorMensaje("Sin TICKETMASTER_API_KEY; usando catalogo simulado.");
            return informe;
        }
        try {
            informe.setEventos(consultarReal(ciudad, key));
        } catch (Exception e) {
            System.err.println("[FuenteEventos] Fallback simulado (" + e.getMessage() + ")");
            informe.setEventos(simular(ciudad));
            informe.setErrorMensaje("Ticketmaster fallo: " + e.getMessage());
        }
        return informe;
    }

    private List<EventoTuristico> consultarReal(String ciudad, String key) throws Exception {
        String url = URL_BASE
            + "?apikey=" + key
            + "&city=" + ClienteHttp.urlEncode(ciudad)
            + "&locale=*"
            + "&size=" + LIMITE
            + "&sort=date,asc";
        JSONObject json = new JSONObject(http.get(url));
        JSONObject embedded = json.optJSONObject("_embedded");
        if (embedded == null) return Collections.emptyList();
        JSONArray eventos = embedded.optJSONArray("events");
        if (eventos == null) return Collections.emptyList();

        List<EventoTuristico> resultado = new ArrayList<>();
        for (int i = 0; i < eventos.length(); i++) {
            JSONObject e = eventos.getJSONObject(i);
            EventoTuristico ev = new EventoTuristico();
            ev.setNombre(e.optString("name"));
            ev.setTipo(extraerTipo(e));
            ev.setFechaInicio(extraerFecha(e, "start"));
            ev.setFechaFin(extraerFecha(e, "end"));
            ev.setLugar(extraerLugar(e));
            extraerPrecios(e, ev);
            ev.setUrlEntradas(e.optString("url", null));
            resultado.add(ev);
        }
        return resultado;
    }

    private String extraerTipo(JSONObject e) {
        JSONArray clasif = e.optJSONArray("classifications");
        if (clasif != null && clasif.length() > 0) {
            JSONObject c = clasif.getJSONObject(0);
            JSONObject seg = c.optJSONObject("segment");
            if (seg != null) return seg.optString("name", "evento").toLowerCase(Locale.ROOT);
        }
        return "evento";
    }

    private String extraerFecha(JSONObject e, String tipo) {
        JSONObject dates = e.optJSONObject("dates");
        if (dates == null) return null;
        JSONObject t = dates.optJSONObject(tipo);
        if (t == null) return null;
        return t.optString("dateTime", t.optString("localDate", null));
    }

    private String extraerLugar(JSONObject e) {
        JSONObject embedded = e.optJSONObject("_embedded");
        if (embedded == null) return "(sin lugar)";
        JSONArray venues = embedded.optJSONArray("venues");
        if (venues == null || venues.length() == 0) return "(sin lugar)";
        return venues.getJSONObject(0).optString("name", "(sin lugar)");
    }

    private void extraerPrecios(JSONObject e, EventoTuristico ev) {
        JSONArray priceRanges = e.optJSONArray("priceRanges");
        if (priceRanges == null || priceRanges.length() == 0) return;
        JSONObject pr = priceRanges.getJSONObject(0);
        ev.setPrecioMinimoEuros(pr.optDouble("min", 0));
        ev.setPrecioMaximoEuros(pr.optDouble("max", 0));
    }

    private List<EventoTuristico> simular(String ciudad) {
        switch (ciudad.toLowerCase(Locale.ROOT)) {
            case "madrid":
                return Arrays.asList(
                    new EventoTuristico("Concierto en el WiZink Center", "concierto",
                        "2026-06-12T21:00:00Z", null, "WiZink Center",
                        35, 80, null),
                    new EventoTuristico("Exposicion temporal Reina Sofia", "exposicion",
                        "2026-06-01", "2026-09-30", "Museo Reina Sofia",
                        8, 12, null));
            case "barcelona":
                return Arrays.asList(
                    new EventoTuristico("Festival Sonar", "festival",
                        "2026-06-18", "2026-06-20", "Fira Gran Via",
                        90, 220, null));
            default:
                return Collections.emptyList();
        }
    }
}
