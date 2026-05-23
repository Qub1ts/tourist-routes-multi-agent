package com.sistemainteligentes.percepcion;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Wrapper minimo sobre java.net.http.HttpClient para que las clases
 * Fuente* no tengan que repetir el boilerplate de timeouts y manejo de
 * codigos HTTP.
 */
final class ClienteHttp {

    private static final Duration CONNECT = Duration.ofSeconds(5);
    private static final Duration REQUEST = Duration.ofSeconds(10);

    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(CONNECT)
        .build();

    String get(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
            .timeout(REQUEST)
            .header("Accept", "application/json")
            .GET()
            .build();
        return ejecutar(req, url);
    }

    String get(String url, String headerNombre, String headerValor)
            throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
            .timeout(REQUEST)
            .header("Accept", "application/json")
            .header(headerNombre, headerValor)
            .GET()
            .build();
        return ejecutar(req, url);
    }

    /**
     * Variante con varias cabeceras (utilizada por las APIs de RapidAPI,
     * que exigen X-RapidAPI-Key y X-RapidAPI-Host a la vez). Los
     * parametros se reciben en pares: {@code (h1, v1, h2, v2, ...)}.
     */
    String get(String url, String... headers) throws IOException, InterruptedException {
        if (headers.length % 2 != 0) {
            throw new IllegalArgumentException("Headers en pares (nombre, valor)");
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
            .timeout(REQUEST)
            .header("Accept", "application/json");
        for (int i = 0; i < headers.length; i += 2) {
            builder.header(headers[i], headers[i + 1]);
        }
        return ejecutar(builder.GET().build(), url);
    }

    String postForm(String url, String body) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
            .timeout(REQUEST)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(BodyPublishers.ofString(body))
            .build();
        return ejecutar(req, url);
    }

    private String ejecutar(HttpRequest req, String urlParaLog)
            throws IOException, InterruptedException {
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        int code = res.statusCode();
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + " <- " + urlParaLog
                + " body=" + recortar(res.body()));
        }
        return res.body();
    }

    static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String recortar(String s) {
        if (s == null) return "";
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
