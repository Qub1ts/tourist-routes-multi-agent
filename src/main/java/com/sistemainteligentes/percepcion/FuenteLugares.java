package com.sistemainteligentes.percepcion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sistemainteligentes.comun.InformePercepcion;
import com.sistemainteligentes.comun.LugarTuristico;
import com.sistemainteligentes.comun.PreferenciasUsuario;

/**
 * Cliente de OpenTripMap (https://opentripmap.io/docs).
 * Hace dos llamadas: geocoding ciudad -> (lat,lon) y luego places radius
 * con los "kinds" mapeados desde los intereses del usuario.
 *
 * Sin clave o ante errores cae al catalogo simulado.
 */
final class FuenteLugares {

    private static final String URL_BASE = "https://api.opentripmap.com/0.1/en/places";
    private static final int RADIO_METROS = 5_000;
    private static final int LIMITE = 20;

    private final ClienteHttp http = new ClienteHttp();

    InformePercepcion consultar(PreferenciasUsuario prefs) {
        String ciudad = prefs.getCiudad() != null ? prefs.getCiudad() : "Desconocida";
        InformePercepcion informe = new InformePercepcion("lugares", ciudad, prefs);

        String key = ConfiguracionApis.claveOpenTripMap();
        if (key.isBlank()) {
            informe.setLugares(simular(ciudad, prefs.getIntereses()));
            informe.setErrorMensaje("Sin OPENTRIPMAP_API_KEY; usando catalogo simulado.");
            return informe;
        }
        try {
            informe.setLugares(consultarReal(ciudad, prefs.getIntereses(), key));
        } catch (Exception e) {
            System.err.println("[FuenteLugares] Fallback simulado (" + e.getMessage() + ")");
            informe.setLugares(simular(ciudad, prefs.getIntereses()));
            informe.setErrorMensaje("OpenTripMap fallo: " + e.getMessage());
        }
        return informe;
    }

    private List<LugarTuristico> consultarReal(String ciudad,
                                               List<String> intereses,
                                               String key) throws Exception {
        String urlGeo = URL_BASE + "/geoname?name="
            + ClienteHttp.urlEncode(ciudad) + "&apikey=" + key;
        JSONObject geo = new JSONObject(http.get(urlGeo));
        if (!geo.has("lat") || !geo.has("lon")) {
            throw new RuntimeException("OpenTripMap: ciudad no encontrada");
        }
        double lat = geo.getDouble("lat");
        double lon = geo.getDouble("lon");

        String kinds = String.join(",", mapearKinds(intereses));
        String urlRad = URL_BASE + "/radius"
            + "?radius=" + RADIO_METROS
            + "&lon=" + lon + "&lat=" + lat
            + "&kinds=" + kinds
            + "&format=json&limit=" + LIMITE
            + "&apikey=" + key;
        JSONArray crudos = new JSONArray(http.get(urlRad));

        List<LugarTuristico> resultado = new ArrayList<>();
        for (int i = 0; i < crudos.length(); i++) {
            JSONObject p = crudos.getJSONObject(i);
            String nombre = p.optString("name", "").trim();
            if (nombre.isBlank()) continue;
            String tipo = inferirTipo(p.optString("kinds", ""));
            resultado.add(new LugarTuristico(nombre, tipo,
                estimarPrecio(tipo), estimarHorario(tipo),
                mapearValoracion(p.optInt("rate", 0))));
        }
        return resultado;
    }

    private Set<String> mapearKinds(List<String> intereses) {
        Set<String> kinds = new LinkedHashSet<>();
        if (intereses == null || intereses.isEmpty()) {
            kinds.addAll(Arrays.asList("museums", "foods", "natural", "historic"));
            return kinds;
        }
        for (String raw : intereses) {
            String it = raw.toLowerCase(Locale.ROOT).trim();
            switch (it) {
                case "museo": case "museos": case "cultura":
                    kinds.add("museums"); break;
                case "comida": case "gastronomia": case "restaurantes":
                    kinds.add("foods"); break;
                case "naturaleza": case "parque": case "parques":
                    kinds.add("natural");
                    kinds.add("urban_environment");
                    break;
                case "historia": case "monumentos":
                    kinds.add("historic");
                    kinds.add("architecture");
                    break;
                case "religioso": case "iglesia":
                    kinds.add("religion"); break;
                default:
                    kinds.add(it);
            }
        }
        return kinds;
    }

    private String inferirTipo(String kinds) {
        String k = kinds.toLowerCase(Locale.ROOT);
        if (k.contains("museum")) return "museo";
        if (k.contains("food") || k.contains("restaurant")) return "restaurante";
        if (k.contains("natural") || k.contains("garden") || k.contains("park")) return "parque";
        if (k.contains("historic") || k.contains("architecture") || k.contains("monument")) return "monumento";
        if (k.contains("religion") || k.contains("church")) return "religioso";
        return "lugar";
    }

    private double estimarPrecio(String tipo) {
        switch (tipo) {
            case "museo": return 12.0;
            case "restaurante": return 25.0;
            case "parque": return 0.0;
            case "monumento": return 5.0;
            case "religioso": return 0.0;
            default: return 8.0;
        }
    }

    private String estimarHorario(String tipo) {
        switch (tipo) {
            case "museo": return "10:00 - 20:00";
            case "restaurante": return "13:00 - 23:00";
            case "parque": return "06:00 - 22:00";
            case "monumento": return "09:00 - 19:00";
            default: return "Consultar en web oficial";
        }
    }

    private double mapearValoracion(int rate) {
        if (rate <= 0) return 3.5;
        return Math.min(5.0, 3.0 + rate * 0.3);
    }

    private List<LugarTuristico> simular(String ciudad, List<String> intereses) {
        List<LugarTuristico> base;
        switch (ciudad.toLowerCase(Locale.ROOT)) {
            case "madrid":
                base = Arrays.asList(
                    new LugarTuristico("Museo del Prado", "museo", 15, "10:00 - 20:00", 4.8),
                    new LugarTuristico("Reina Sofia", "museo", 12, "10:00 - 21:00", 4.6),
                    new LugarTuristico("Parque del Retiro", "parque", 0, "06:00 - 22:00", 4.7),
                    new LugarTuristico("Mercado de San Miguel", "restaurante", 20, "10:00 - 24:00", 4.4));
                break;
            case "barcelona":
                base = Arrays.asList(
                    new LugarTuristico("Sagrada Familia", "monumento", 26, "09:00 - 18:00", 4.9),
                    new LugarTuristico("Parc Guell", "parque", 10, "09:30 - 19:30", 4.7),
                    new LugarTuristico("La Boqueria", "restaurante", 18, "08:00 - 20:30", 4.5));
                break;
            default:
                return Collections.emptyList();
        }
        if (intereses == null || intereses.isEmpty()) return base;
        List<LugarTuristico> filtrado = new ArrayList<>();
        for (LugarTuristico l : base) {
            for (String i : intereses) {
                if (encaja(l, i)) { filtrado.add(l); break; }
            }
        }
        return filtrado;
    }

    private boolean encaja(LugarTuristico l, String interes) {
        String it = interes.toLowerCase(Locale.ROOT).trim();
        String tipo = l.getTipo().toLowerCase(Locale.ROOT);
        if (it.equals(tipo)) return true;
        if ("comida".equals(it) || "gastronomia".equals(it)) return "restaurante".equals(tipo);
        if ("cultura".equals(it)) return "museo".equals(tipo);
        if ("naturaleza".equals(it)) return "parque".equals(tipo);
        return false;
    }
}
