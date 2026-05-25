package com.sistemainteligentes.recomendador;

import com.sistemainteligentes.comun.InformePercepcion;
import com.sistemainteligentes.comun.PreferenciasUsuario;

import java.lang.reflect.Method;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

public class MotorRecomendacion {

    public String generarRecomendacion(List<InformePercepcion> fragmentos) {
        return generarRecomendacion(fragmentos, 0, false, new ArrayList<>());
    }

    public String generarRecomendacionAlternativa(
            List<InformePercepcion> fragmentos,
            int intento,
            List<String> nombresExcluidos
    ) {
        int desplazamiento = Math.max(1, intento - 1);
        return generarRecomendacion(fragmentos, desplazamiento, true, nombresExcluidos);
    }

    private String generarRecomendacion(
            List<InformePercepcion> fragmentos,
            int desplazamiento,
            boolean esAlternativa,
            List<String> nombresExcluidos
    ) {
        if (fragmentos == null || fragmentos.isEmpty()) {
            return "No se recibieron informes de percepción.";
        }

        PreferenciasUsuario preferencias = fragmentos.get(0).getPreferencias();

        String ciudad = leerString(preferencias, "getCiudad", "ciudad desconocida");
        int dias = leerInt(preferencias, "getDiasDisponibles", 1);
        double presupuesto = leerDouble(preferencias, "getPresupuestoMaximo", 50.0);
        String intereses = leerString(preferencias, "getIntereses", "");

        double presupuestoPorDia = presupuesto / Math.max(dias, 1);

        List<CandidatoRuta> candidatos = construirCandidatos(fragmentos);
        double scoreClimaGlobal = calcularScoreClimaGlobal(fragmentos);

        for (CandidatoRuta candidato : candidatos) {
            candidato.setScoreIntereses(calcularSimilitudIntereses(intereses, candidato));
            candidato.setScorePresupuesto(calcularScorePresupuesto(candidato, presupuestoPorDia));
            candidato.setScoreClima(scoreClimaGlobal);
            candidato.setScorePopularidad(calcularScorePopularidad(candidato));
            candidato.setScoreDiversidad(calcularScoreDiversidad(candidato));
            candidato.calcularScoreFinal();
        }

        Collections.sort(candidatos);

        List<CandidatoRuta> candidatosPorPresupuesto = filtrarViablesPorPresupuesto(candidatos);

        if (!candidatosPorPresupuesto.isEmpty()) {
            candidatos = candidatosPorPresupuesto;
        }

        Set<String> excluidos = nombresExcluidos == null
                ? new HashSet<>()
                : nombresExcluidos.stream()
                        .map(this::normalizarTexto)
                        .collect(Collectors.toSet());

        List<CandidatoRuta> candidatosFiltrados = candidatos.stream()
                .filter(c -> !excluidos.contains(normalizarTexto(c.getNombre())))
                .collect(Collectors.toList());

        if (candidatosFiltrados.isEmpty()) {
            candidatosFiltrados = candidatos;
        }

        int lugaresDeseados = Math.max(1, dias);
        int eventosDeseados = Math.max(1, dias);

        List<CandidatoRuta> lugares = filtrarPorTipo(
                candidatosFiltrados,
                CandidatoRuta.Tipo.LUGAR,
                lugaresDeseados,
                desplazamiento
        );

        List<CandidatoRuta> hoteles = filtrarPorTipo(
                candidatosFiltrados,
                CandidatoRuta.Tipo.HOTEL,
                1,
                desplazamiento
        );

        List<CandidatoRuta> eventos = filtrarPorTipo(
                candidatosFiltrados,
                CandidatoRuta.Tipo.EVENTO,
                eventosDeseados,
                desplazamiento
        );

        double costeTotal = calcularCosteTotal(lugares, hoteles, eventos, dias);

        if (costeTotal > presupuesto) {
            return construirRespuestaSinPresupuesto(
                    ciudad,
                    dias,
                    presupuesto,
                    presupuestoPorDia,
                    esAlternativa,
                    nombresExcluidos
            );
        }

        return construirRespuesta(
                ciudad,
                dias,
                presupuesto,
                presupuestoPorDia,
                intereses,
                scoreClimaGlobal,
                lugares,
                hoteles,
                eventos,
                candidatosFiltrados,
                esAlternativa,
                desplazamiento,
                nombresExcluidos,
                costeTotal
        );
    }

    public List<String> extraerNombresSeleccionados(
            List<InformePercepcion> fragmentos,
            int desplazamiento,
            List<String> nombresExcluidos
    ) {
        List<String> nombres = new ArrayList<>();

        if (fragmentos == null || fragmentos.isEmpty()) {
            return nombres;
        }

        PreferenciasUsuario preferencias = fragmentos.get(0).getPreferencias();

        String intereses = leerString(preferencias, "getIntereses", "");
        int dias = leerInt(preferencias, "getDiasDisponibles", 1);
        double presupuesto = leerDouble(preferencias, "getPresupuestoMaximo", 50.0);
        double presupuestoPorDia = presupuesto / Math.max(dias, 1);

        List<CandidatoRuta> candidatos = construirCandidatos(fragmentos);
        double scoreClimaGlobal = calcularScoreClimaGlobal(fragmentos);

        for (CandidatoRuta candidato : candidatos) {
            candidato.setScoreIntereses(calcularSimilitudIntereses(intereses, candidato));
            candidato.setScorePresupuesto(calcularScorePresupuesto(candidato, presupuestoPorDia));
            candidato.setScoreClima(scoreClimaGlobal);
            candidato.setScorePopularidad(calcularScorePopularidad(candidato));
            candidato.setScoreDiversidad(calcularScoreDiversidad(candidato));
            candidato.calcularScoreFinal();
        }

        Collections.sort(candidatos);

        List<CandidatoRuta> candidatosPorPresupuesto = filtrarViablesPorPresupuesto(candidatos);

        if (!candidatosPorPresupuesto.isEmpty()) {
            candidatos = candidatosPorPresupuesto;
        }

        Set<String> excluidos = nombresExcluidos == null
                ? new HashSet<>()
                : nombresExcluidos.stream()
                        .map(this::normalizarTexto)
                        .collect(Collectors.toSet());

        List<CandidatoRuta> candidatosFiltrados = candidatos.stream()
                .filter(c -> !excluidos.contains(normalizarTexto(c.getNombre())))
                .collect(Collectors.toList());

        if (candidatosFiltrados.isEmpty()) {
            candidatosFiltrados = candidatos;
        }

        List<CandidatoRuta> lugares = filtrarPorTipo(
                candidatosFiltrados,
                CandidatoRuta.Tipo.LUGAR,
                Math.max(1, dias),
                desplazamiento
        );

        List<CandidatoRuta> hoteles = filtrarPorTipo(
                candidatosFiltrados,
                CandidatoRuta.Tipo.HOTEL,
                1,
                desplazamiento
        );

        List<CandidatoRuta> eventos = filtrarPorTipo(
                candidatosFiltrados,
                CandidatoRuta.Tipo.EVENTO,
                Math.max(1, dias),
                desplazamiento
        );

        double costeTotal = calcularCosteTotal(lugares, hoteles, eventos, dias);

        if (costeTotal > presupuesto) {
            return nombres;
        }

        lugares.forEach(c -> nombres.add(c.getNombre()));
        hoteles.forEach(c -> nombres.add(c.getNombre()));
        eventos.forEach(c -> nombres.add(c.getNombre()));

        return nombres;
    }

    private String construirRespuestaSinPresupuesto(
            String ciudad,
            int dias,
            double presupuesto,
            double presupuestoPorDia,
            boolean esAlternativa,
            List<String> nombresExcluidos
    ) {
        StringBuilder sb = new StringBuilder();

        if (esAlternativa) {
            sb.append("NO SE PUDO GENERAR UNA NUEVA PROPUESTA ALTERNATIVA\n\n");
        } else {
            sb.append("NO SE PUDO GENERAR UNA RUTA PARA ").append(ciudad.toUpperCase()).append("\n\n");
        }

        sb.append("El sistema no encontro una combinacion de hotel, lugares y eventos ");
        sb.append("que se ajustara al presupuesto indicado.\n\n");

        sb.append("Datos de la busqueda\n");
        sb.append("- Ciudad: ").append(ciudad).append("\n");
        sb.append("- Dias solicitados: ").append(dias).append("\n");
        sb.append("- Presupuesto maximo: ").append(String.format("%.2f", presupuesto)).append(" EUR\n");
        sb.append("- Presupuesto aproximado por dia: ")
                .append(String.format("%.2f", presupuestoPorDia)).append(" EUR\n");

        if (nombresExcluidos != null && !nombresExcluidos.isEmpty()) {
            sb.append("- Candidatos previamente rechazados/excluidos: ")
                    .append(nombresExcluidos.size()).append("\n");
        }

        sb.append("\nResultado\n");
        sb.append("- No se encontraron hoteles que se ajustaran al presupuesto indicado.\n");
        sb.append("- No se encontraron lugares que se ajustaran al presupuesto indicado.\n");
        sb.append("- No se encontraron eventos que se ajustaran al presupuesto indicado.\n\n");

        sb.append("Recomendacion\n");
        sb.append("- Aumentar el presupuesto.\n");
        sb.append("- Reducir los dias de viaje.\n");
        sb.append("- Buscar actividades gratuitas.\n");
        sb.append("- Cambiar intereses o ciudad de destino.\n");

        return sb.toString();
    }

    private String construirRespuesta(
            String ciudad,
            int dias,
            double presupuesto,
            double presupuestoPorDia,
            String intereses,
            double scoreClima,
            List<CandidatoRuta> lugares,
            List<CandidatoRuta> hoteles,
            List<CandidatoRuta> eventos,
            List<CandidatoRuta> rankingGlobal,
            boolean esAlternativa,
            int desplazamiento,
            List<String> nombresExcluidos,
            double costeTotal
    ) {
        StringBuilder sb = new StringBuilder();

        if (esAlternativa) {
            sb.append("NUEVA PROPUESTA ALTERNATIVA\n");
            sb.append("Usted ha rechazado la propuesta anterior. ");
            sb.append("El sistema excluyo candidatos ya mostrados y recalculo la seleccion.\n");
            sb.append("Candidatos excluidos previamente: ")
                    .append(nombresExcluidos == null ? 0 : nombresExcluidos.size())
                    .append("\n\n");
        }

        sb.append("RUTA RECOMENDADA PARA ").append(ciudad.toUpperCase()).append("\n\n");

        sb.append("Preferencias del usuario\n");
        sb.append("- Ciudad: ").append(ciudad).append("\n");
        sb.append("- Dias disponibles: ").append(dias).append("\n");
        sb.append("- Presupuesto maximo: ").append(String.format("%.2f", presupuesto)).append(" EUR\n");
        sb.append("- Presupuesto aproximado por dia: ").append(String.format("%.2f", presupuestoPorDia)).append(" EUR\n");
        sb.append("- Intereses: ").append(intereses).append("\n\n");

        sb.append("Evaluacion del clima\n");
        sb.append("- Score climatico global: ").append(String.format("%.2f", scoreClima)).append("/1.00\n\n");

        sb.append("Itinerario propuesto\n");

        CandidatoRuta hotel = hoteles.isEmpty() ? null : hoteles.get(0);

        for (int dia = 1; dia <= Math.max(1, dias); dia++) {
            sb.append("\nDia ").append(dia).append("\n");

            CandidatoRuta lugar = obtenerPorIndiceCircular(lugares, dia - 1);
            CandidatoRuta evento = obtenerPorIndiceCircular(eventos, dia - 1);

            sb.append("- Lugar sugerido: ").append(lugar != null ? lugar.getNombre() : "no disponible").append("\n");
            sb.append("- Evento sugerido: ").append(evento != null ? evento.getNombre() : "no disponible").append("\n");
            sb.append("- Hotel base: ").append(hotel != null ? hotel.getNombre() : "no disponible").append("\n");
        }

        sb.append("\nHotel recomendado\n");
        agregarLista(sb, hoteles);

        sb.append("\nLugares seleccionados\n");
        agregarLista(sb, lugares);

        sb.append("\nEventos seleccionados\n");
        agregarLista(sb, eventos);

        sb.append("\nCoste aproximado estimado de la ruta seleccionada\n");
        sb.append("- Total aproximado: ").append(String.format("%.2f", costeTotal)).append(" EUR\n");
        sb.append("- Estado: dentro del presupuesto.\n");

        return sb.toString();
    }

    private void agregarLista(StringBuilder sb, List<CandidatoRuta> lista) {
        if (lista == null || lista.isEmpty()) {
            sb.append("- No disponible.\n");
            return;
        }

        for (int i = 0; i < lista.size(); i++) {
            agregarCandidato(sb, i + 1, lista.get(i));
        }
    }

    private double calcularCosteTotal(
            List<CandidatoRuta> lugares,
            List<CandidatoRuta> hoteles,
            List<CandidatoRuta> eventos,
            int dias
    ) {
        double costeTotal = 0.0;

        for (CandidatoRuta lugar : lugares) {
            costeTotal += lugar.getCosteEstimado();
        }

        for (CandidatoRuta evento : eventos) {
            costeTotal += evento.getCosteEstimado();
        }

        if (!hoteles.isEmpty()) {
            costeTotal += hoteles.get(0).getCosteEstimado() * dias;
        }

        return costeTotal;
    }

    private List<CandidatoRuta> construirCandidatos(List<InformePercepcion> fragmentos) {
        List<CandidatoRuta> candidatos = new ArrayList<>();

        for (InformePercepcion informe : fragmentos) {
            String fuente = informe.getFuente() == null ? "" : informe.getFuente().toLowerCase();
            List<Object> elementos = extraerElementos(informe);

            for (Object elemento : elementos) {
                if (elemento == null) continue;

                if (fuente.contains("lugar")) {
                    candidatos.add(crearCandidato(CandidatoRuta.Tipo.LUGAR, elemento));
                } else if (fuente.contains("hotel")) {
                    candidatos.add(crearCandidato(CandidatoRuta.Tipo.HOTEL, elemento));
                } else if (fuente.contains("evento")) {
                    candidatos.add(crearCandidato(CandidatoRuta.Tipo.EVENTO, elemento));
                }
            }
        }

        return candidatos;
    }

    private CandidatoRuta crearCandidato(CandidatoRuta.Tipo tipo, Object elemento) {
        String nombre = primerStringDisponible(elemento, "getNombre", "getName", "getTitulo", "getTitle");

        if (nombre == null || nombre.isBlank()) {
            nombre = elemento.getClass().getSimpleName();
        }

        String descripcion = primerStringDisponible(elemento, "getDescripcion", "getDescription", "getCategoria", "getTipo", "getDireccion");

        if (descripcion == null || descripcion.isBlank()) {
            descripcion = elemento.toString();
        }

        double coste = primerDoubleDisponible(elemento, "getPrecio", "getPrecioEstimado", "getCoste", "getCosto", "getPrice");

        if (coste <= 0) {
            coste = costeEstimadoPorTipo(tipo);
        }

        return new CandidatoRuta(tipo, elemento, nombre, descripcion, coste);
    }

    private double calcularSimilitudIntereses(String interesesUsuario, CandidatoRuta candidato) {
        Set<String> queryOriginal = tokenizar(interesesUsuario);
        Set<String> queryExpandida = expandirConOntologiaTuristica(queryOriginal);

        String textoCandidato = candidato.getNombre() + " " + candidato.getDescripcion();
        Set<String> documento = tokenizar(textoCandidato);

        if (queryExpandida.isEmpty() || documento.isEmpty()) {
            return 0.30;
        }

        Set<String> interseccion = new HashSet<>(queryExpandida);
        interseccion.retainAll(documento);

        Set<String> union = new HashSet<>(queryExpandida);
        union.addAll(documento);

        double jaccard = union.isEmpty() ? 0.0 : (double) interseccion.size() / union.size();

        double bonusOntologico = 0.0;

        for (String termino : queryExpandida) {
            if (documento.contains(termino)) {
                bonusOntologico += 0.12;
            }
        }

        return limitar01(jaccard + bonusOntologico);
    }

    private Set<String> expandirConOntologiaTuristica(Set<String> intereses) {
        Map<String, Set<String>> ontologia = new HashMap<>();

        ontologia.put("museo", Set.of("museo", "museos", "exposicion", "galeria", "arte", "cultura", "historia"));
        ontologia.put("museos", Set.of("museo", "museos", "exposicion", "galeria", "arte", "cultura", "historia"));
        ontologia.put("arte", Set.of("arte", "museo", "galeria", "exposicion", "cultura", "historia"));
        ontologia.put("cultura", Set.of("cultura", "museo", "monumento", "historia", "arte", "exposicion", "patrimonio"));
        ontologia.put("historia", Set.of("historia", "museo", "monumento", "cultura", "patrimonio", "palacio"));
        ontologia.put("comida", Set.of("comida", "restaurante", "mercado", "gastronomia", "tapas", "cocina", "bar"));
        ontologia.put("gastronomia", Set.of("comida", "restaurante", "mercado", "gastronomia", "tapas", "cocina", "bar"));
        ontologia.put("restaurante", Set.of("comida", "restaurante", "gastronomia", "tapas", "cocina", "bar"));
        ontologia.put("naturaleza", Set.of("naturaleza", "parque", "jardin", "rio", "montana", "senderismo", "aire", "libre"));
        ontologia.put("parque", Set.of("naturaleza", "parque", "jardin", "aire", "libre", "paseo"));
        ontologia.put("ocio", Set.of("ocio", "concierto", "evento", "espectaculo", "teatro", "musica", "show"));
        ontologia.put("musica", Set.of("musica", "concierto", "evento", "espectaculo", "teatro", "ocio"));
        ontologia.put("compras", Set.of("compras", "tienda", "mercado", "centro", "comercial", "shopping"));
        ontologia.put("religion", Set.of("religion", "iglesia", "catedral", "templo", "monasterio"));

        Set<String> expandidos = new HashSet<>(intereses);

        for (String interes : intereses) {
            Set<String> relacionados = ontologia.get(interes);
            if (relacionados != null) {
                expandidos.addAll(relacionados);
            }
        }

        return expandidos;
    }

    private double calcularScorePresupuesto(CandidatoRuta candidato, double presupuestoPorDia) {
        double coste = candidato.getCosteEstimado();

        if (coste <= 0) return 0.60;

        if (candidato.getTipo() == CandidatoRuta.Tipo.HOTEL) {
            if (coste <= presupuestoPorDia) return 1.0;
            return Math.max(0.0, 1.0 - ((coste - presupuestoPorDia) / presupuestoPorDia));
        }

        double presupuestoActividad = presupuestoPorDia * 0.40;

        if (coste <= presupuestoActividad) return 1.0;

        return Math.max(0.0, 1.0 - ((coste - presupuestoActividad) / Math.max(presupuestoActividad, 1.0)));
    }

    private double calcularScoreClimaGlobal(List<InformePercepcion> fragmentos) {
        String textoClima = fragmentos.stream()
                .filter(f -> f.getFuente() != null && f.getFuente().toLowerCase().contains("clima"))
                .map(Object::toString)
                .collect(Collectors.joining(" "))
                .toLowerCase();

        if (textoClima.isBlank()) return 0.70;

        if (textoClima.contains("lluvia") || textoClima.contains("rain")
                || textoClima.contains("tormenta") || textoClima.contains("storm")) {
            return 0.45;
        }

        if (textoClima.contains("nieve") || textoClima.contains("snow")) {
            return 0.35;
        }

        if (textoClima.contains("claro") || textoClima.contains("soleado")
                || textoClima.contains("sun") || textoClima.contains("clear")) {
            return 1.0;
        }

        if (textoClima.contains("nublado") || textoClima.contains("cloud")) {
            return 0.75;
        }

        return 0.70;
    }

    private double calcularScorePopularidad(CandidatoRuta candidato) {
        double valoracion = primerDoubleDisponible(candidato.getElementoOriginal(), "getValoracion", "getRating", "getPuntuacion", "getScore");

        if (valoracion <= 0) return 0.60;

        if (valoracion > 5.0) return limitar01(valoracion / 10.0);

        return limitar01(valoracion / 5.0);
    }

    private double calcularScoreDiversidad(CandidatoRuta candidato) {
        switch (candidato.getTipo()) {
            case LUGAR:
                return 1.00;
            case EVENTO:
                return 0.85;
            case HOTEL:
                return 0.70;
            default:
                return 0.60;
        }
    }

    private CandidatoRuta obtenerPorIndiceCircular(List<CandidatoRuta> lista, int indice) {
        if (lista == null || lista.isEmpty()) return null;
        return lista.get(indice % lista.size());
    }

    private void agregarCandidato(StringBuilder sb, int posicion, CandidatoRuta candidato) {
        sb.append(posicion).append(". ").append(candidato.getNombre()).append("\n");
        sb.append("   Tipo: ").append(candidato.getTipo()).append("\n");
        sb.append("   Descripcion: ").append(recortar(candidato.getDescripcion(), 180)).append("\n");
        sb.append("   Coste estimado: ").append(String.format("%.2f", candidato.getCosteEstimado())).append(" EUR\n");
        sb.append("   Score final: ").append(String.format("%.3f", candidato.getScoreFinal())).append("\n");
        sb.append("   Detalle score: intereses=")
                .append(String.format("%.2f", candidato.getScoreIntereses()))
                .append(", presupuesto=")
                .append(String.format("%.2f", candidato.getScorePresupuesto()))
                .append(", clima=")
                .append(String.format("%.2f", candidato.getScoreClima()))
                .append(", popularidad=")
                .append(String.format("%.2f", candidato.getScorePopularidad()))
                .append(", diversidad=")
                .append(String.format("%.2f", candidato.getScoreDiversidad()))
                .append("\n");
    }

    private List<CandidatoRuta> filtrarViablesPorPresupuesto(List<CandidatoRuta> candidatos) {
        return candidatos.stream()
                .filter(c -> c.getScorePresupuesto() > 0.0)
                .collect(Collectors.toList());
    }

    private List<CandidatoRuta> filtrarPorTipo(List<CandidatoRuta> candidatos, CandidatoRuta.Tipo tipo, int limite, int desplazamiento) {
        List<CandidatoRuta> filtrados = candidatos.stream()
                .filter(c -> c.getTipo() == tipo)
                .collect(Collectors.toList());

        if (filtrados.isEmpty()) return filtrados;

        int inicio = Math.min(Math.max(0, desplazamiento), Math.max(0, filtrados.size() - 1));

        List<CandidatoRuta> resultado = new ArrayList<>();

        for (int i = inicio; i < filtrados.size() && resultado.size() < limite; i++) {
            resultado.add(filtrados.get(i));
        }

        for (int i = 0; i < inicio && resultado.size() < limite; i++) {
            resultado.add(filtrados.get(i));
        }

        return resultado;
    }

    private List<Object> extraerElementos(InformePercepcion informe) {
        List<Object> elementos = new ArrayList<>();

        for (Method metodo : informe.getClass().getMethods()) {
            if (metodo.getParameterCount() != 0) continue;

            String nombreMetodo = metodo.getName().toLowerCase();

            if (!nombreMetodo.startsWith("get")) continue;
            if (nombreMetodo.contains("class")) continue;
            if (nombreMetodo.contains("preferencias")) continue;
            if (nombreMetodo.contains("fuente")) continue;
            if (nombreMetodo.contains("error")) continue;

            try {
                Object valor = metodo.invoke(informe);
                if (valor instanceof Collection<?>) elementos.addAll((Collection<?>) valor);
            } catch (Exception ignored) {
            }
        }

        return elementos;
    }

    private Set<String> tokenizar(String texto) {
        if (texto == null) return new HashSet<>();

        String limpio = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9ñü ]", " ");

        Set<String> stopwords = Set.of(
                "el", "la", "los", "las", "un", "una", "unos", "unas",
                "de", "del", "en", "y", "o", "con", "para", "por",
                "the", "a", "an", "of", "in", "on", "and", "or", "for",
                "madrid", "barcelona", "ciudad"
        );

        return Arrays.stream(limpio.split("\\s+"))
                .map(String::trim)
                .filter(t -> t.length() > 2)
                .filter(t -> !stopwords.contains(t))
                .collect(Collectors.toSet());
    }

    private String primerStringDisponible(Object objeto, String... metodos) {
        for (String metodo : metodos) {
            String valor = leerString(objeto, metodo, null);
            if (valor != null && !valor.isBlank()) return valor;
        }
        return null;
    }

    private double primerDoubleDisponible(Object objeto, String... metodos) {
        for (String metodo : metodos) {
            double valor = leerDouble(objeto, metodo, -1.0);
            if (valor > 0) return valor;
        }
        return -1.0;
    }

    private String leerString(Object objeto, String metodo, String valorPorDefecto) {
        try {
            Method m = objeto.getClass().getMethod(metodo);
            Object valor = m.invoke(objeto);
            if (valor == null) return valorPorDefecto;
            return String.valueOf(valor);
        } catch (Exception e) {
            return valorPorDefecto;
        }
    }

    private int leerInt(Object objeto, String metodo, int valorPorDefecto) {
        try {
            Method m = objeto.getClass().getMethod(metodo);
            Object valor = m.invoke(objeto);
            if (valor instanceof Number) return ((Number) valor).intValue();
            return Integer.parseInt(String.valueOf(valor));
        } catch (Exception e) {
            return valorPorDefecto;
        }
    }

    private double leerDouble(Object objeto, String metodo, double valorPorDefecto) {
        try {
            Method m = objeto.getClass().getMethod(metodo);
            Object valor = m.invoke(objeto);
            if (valor instanceof Number) return ((Number) valor).doubleValue();
            return Double.parseDouble(String.valueOf(valor));
        } catch (Exception e) {
            return valorPorDefecto;
        }
    }

    private double costeEstimadoPorTipo(CandidatoRuta.Tipo tipo) {
        switch (tipo) {
            case HOTEL:
                return 45.0;
            case EVENTO:
                return 15.0;
            case LUGAR:
                return 8.0;
            default:
                return 10.0;
        }
    }

    private double limitar01(double valor) {
        return Math.max(0.0, Math.min(1.0, valor));
    }

    private String recortar(String texto, int max) {
        if (texto == null) return "";
        if (texto.length() <= max) return texto;
        return texto.substring(0, max) + "...";
    }

    private String normalizarTexto(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }
}