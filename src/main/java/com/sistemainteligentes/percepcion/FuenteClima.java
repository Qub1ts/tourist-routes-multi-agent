package com.sistemainteligentes.percepcion;

import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sistemainteligentes.comun.DatosClima;
import com.sistemainteligentes.comun.InformePercepcion;
import com.sistemainteligentes.comun.PreferenciasUsuario;

/**
 * Cliente de OpenWeatherMap Current Weather Data
 * (https://openweathermap.org/current).
 *
 * Si no hay API key configurada o la llamada falla devuelve un informe
 * con clima simulado para Madrid/Barcelona; cualquier otra ciudad recibe
 * un mensaje generico. Esto garantiza que la demo arranque incluso sin
 * conexion.
 */
final class FuenteClima {

    private static final String URL_BASE =
        "https://api.openweathermap.org/data/2.5/weather";

    private final ClienteHttp http = new ClienteHttp();

    InformePercepcion consultar(PreferenciasUsuario prefs) {
        String ciudad = prefs.getCiudad() != null ? prefs.getCiudad() : "Desconocida";
        InformePercepcion informe = new InformePercepcion("clima", ciudad, prefs);

        String key = ConfiguracionApis.claveOpenWeather();
        if (key.isBlank()) {
            informe.setDatosClima(simular(ciudad));
            informe.setErrorMensaje("Sin OPENWEATHER_API_KEY; usando clima simulado.");
            return informe;
        }
        try {
            informe.setDatosClima(consultarReal(ciudad, key));
        } catch (Exception e) {
            System.err.println("[FuenteClima] Fallback simulado (" + e.getMessage() + ")");
            informe.setDatosClima(simular(ciudad));
            informe.setErrorMensaje("OpenWeather fallo: " + e.getMessage());
        }
        return informe;
    }

    private DatosClima consultarReal(String ciudad, String key) throws Exception {
        String url = URL_BASE
            + "?q=" + ClienteHttp.urlEncode(ciudad)
            + "&appid=" + key
            + "&units=metric&lang=es";
        JSONObject json = new JSONObject(http.get(url));
        if (json.optInt("cod", 200) != 200) {
            throw new RuntimeException("OpenWeather: " + json.optString("message", "?"));
        }
        DatosClima d = new DatosClima();
        JSONArray weather = json.optJSONArray("weather");
        d.setDescripcion(weather != null && weather.length() > 0
            ? weather.getJSONObject(0).optString("description", "")
            : "");
        JSONObject main = json.optJSONObject("main");
        if (main != null) {
            d.setTemperaturaActual(main.optDouble("temp", Double.NaN));
            d.setSensacionTermica(main.optDouble("feels_like", d.getTemperaturaActual()));
            d.setHumedad(main.optInt("humidity", 0));
        }
        JSONObject wind = json.optJSONObject("wind");
        if (wind != null) {
            d.setViento(wind.optDouble("speed", 0));
        }
        JSONObject rain = json.optJSONObject("rain");
        if (rain != null) {
            d.setProbabilidadLluvia(Math.min(1.0, rain.optDouble("1h", 0) / 5.0));
        }
        d.setResumen(construirResumen(d));
        return d;
    }

    private String construirResumen(DatosClima d) {
        if (Double.isNaN(d.getTemperaturaActual())) {
            return capitalizar(d.getDescripcion());
        }
        return String.format(Locale.ROOT, "%s, %.1f grados",
            capitalizar(d.getDescripcion()), d.getTemperaturaActual());
    }

    private DatosClima simular(String ciudad) {
        DatosClima d = new DatosClima();
        switch (ciudad.toLowerCase(Locale.ROOT)) {
            case "madrid":
                d.setResumen("Soleado, 24 grados, baja probabilidad de lluvia");
                d.setTemperaturaActual(24);
                d.setHumedad(45);
                break;
            case "barcelona":
                d.setResumen("Parcialmente nublado, 22 grados");
                d.setTemperaturaActual(22);
                d.setHumedad(60);
                break;
            case "sevilla":
                d.setResumen("Soleado, 30 grados");
                d.setTemperaturaActual(30);
                d.setHumedad(35);
                break;
            case "bilbao":
                d.setResumen("Lluvia ligera, 17 grados");
                d.setTemperaturaActual(17);
                d.setHumedad(80);
                d.setProbabilidadLluvia(0.6);
                break;
            default:
                d.setResumen("Sin datos meteorologicos para " + ciudad);
        }
        return d;
    }

    private static String capitalizar(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
