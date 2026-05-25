# Sistema multiagente de recomendación de rutas turísticas

Práctica de **Sistemas Inteligentes** — curso 2025‑2026 (UPM, ETSI Informáticos).

El sistema recomienda rutas turísticas personalizadas consultando información
real de la web (clima, lugares, hoteles y eventos) y filtrándola según las
preferencias del usuario.

> *Ejemplo del enunciado:* «Tengo 1 día en Madrid, quiero gastar poco y me
> interesan museos y comida.» El sistema consulta cuatro APIs externas en
> paralelo y devuelve una ruta puntuada con costes aproximados.

---

## 1. Instrucciones de instalación

### Requisitos previos

| Software | Versión mínima | Verificar con |
|---|---|---|
| Java JDK | 17 | `java -version` |
| Apache Maven | 3.6.3 | `mvn -version` |
| Git | cualquiera | `git --version` |

### Pasos

```bash
# 1. Clonar el repositorio
git clone https://github.com/Qub1ts/multi-agent.git
cd multi-agent

# 2. Compilar el proyecto (descarga las dependencias declaradas en pom.xml)
mvn -DskipTests compile
```

JADE 4.6.0 se distribuye **dentro del propio repositorio** (`lib/jade.jar`)
para que no haya que descargarlo de ningún sitio externo. Maven lo
referencia con scope `system` desde el `pom.xml`.

---

## 2. Captura de dependencias necesarias para instalar el proyecto

### Dependencias gestionadas por Maven (`pom.xml`)

```xml
<dependencies>
  <dependency>
    <groupId>com.tilab.jade</groupId>
    <artifactId>jade</artifactId>
    <version>4.6.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/jade.jar</systemPath>
  </dependency>
  <dependency>
    <groupId>org.json</groupId>
    <artifactId>json</artifactId>
    <version>20231013</version>
  </dependency>
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-api</artifactId>
    <version>5.6.0</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <version>5.6.0</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

### APIs externas consumidas en tiempo de ejecución

| Servicio | Proveedor | Plan gratuito | Variable de entorno |
|---|---|---|---|
| Clima | OpenWeatherMap *Current Weather Data* | 1 000 llamadas/día | `OPENWEATHER_API_KEY` |
| Lugares turísticos | OpenTripMap (`/geoname` + `/radius`) | sin coste | `OPENTRIPMAP_API_KEY` |
| Hoteles | Booking.com vía **RapidAPI** (`booking-com15.p.rapidapi.com`) | 100 llamadas/mes | `RAPIDAPI_KEY` |
| Eventos | Ticketmaster *Discovery API* | 5 000 llamadas/día | `TICKETMASTER_API_KEY` |

Las claves se proporcionan al sistema de una de estas tres formas (orden de
precedencia):

1. **Variables de entorno** (preferido para CI/defensa).
2. Fichero local `apikeys.properties` en la raíz del proyecto. Está
   incluido en `.gitignore`, por lo que **nunca se sube al repositorio**.
   Una plantilla con los nombres de las claves está en
   `apikeys.properties.example`.
3. **Catálogo simulado de respaldo**: si una clave falta o la API falla,
   ese agente concreto cae automáticamente a una lista fija para Madrid y
   Barcelona. El resto del sistema sigue funcionando.

---

## 3. Instrucciones de ejecución

### Opción A — script `run.sh` (recomendada)

Desde la raíz del proyecto:

```bash
./run.sh
```

El script:

1. Compila si hace falta.
2. Verifica que las clases de los 7 agentes están presentes.
3. Construye el classpath (`target/classes` + `lib/jade.jar` + `org.json`).
4. Carga las claves de API desde variables de entorno o de
   `apikeys.properties`.
5. Lanza la plataforma JADE con los 7 agentes y abre la **consola RMA**.

Hay también un modo sin display (servidor sin entorno gráfico):

```bash
./run.sh --headless
```

### Opción B — comando `jade.Boot` manual

```bash
CP="target/classes:lib/jade.jar:$HOME/.m2/repository/org/json/json/20231013/json-20231013.jar"

java -cp "$CP" jade.Boot -gui -agents \
  "clima:com.sistemainteligentes.percepcion.AgenteClima;\
lugares:com.sistemainteligentes.percepcion.AgenteLugares;\
hoteles:com.sistemainteligentes.percepcion.AgenteHoteles;\
eventos:com.sistemainteligentes.percepcion.AgenteEventos;\
recomendador:com.sistemainteligentes.recomendador.AgenteRecomendador;\
interfaz:com.sistemainteligentes.interfaz.AgenteInterfaz;\
usuario:com.sistemainteligentes.usuario.AgenteUsuario"
```

> En `jade.Boot` los agentes deben ir en **un único argumento separados
> por `;`** y precedidos del flag `-agents`. Si se separan con espacios,
> JADE interpreta el primero como fichero de configuración y arranca solo
> uno.

### Qué hacer una vez lanzado el sistema

1. Aparece la **consola RMA** de JADE y la **ventana Swing del
   `AgenteUsuario`**.
2. En la ventana del usuario están precargados los datos de ejemplo
   (sección 4).
3. Pulsar **Enviar preferencias**.
4. En consola se ve el fan-out a los 4 agentes de percepción y la
   recepción del recomendador.
5. La **ventana del `AgenteInterfaz`** muestra la ruta final.

---

## 4. Datos de ejemplo para ejecutar la práctica

La ventana del `AgenteUsuario` se inicializa con un caso de prueba
extraído directamente del enunciado:

| Campo | Valor por defecto |
|---|---|
| Ciudad | `Madrid` |
| Días disponibles | `1` |
| Presupuesto máximo (€) | `50` |
| Intereses | `museos, comida` |

Otros casos de prueba para la defensa (solo cambia los valores en la
ventana antes de pulsar *Enviar*):

| Caso | Ciudad | Días | Presupuesto | Intereses |
|---|---|---|---|---|
| Demo enunciado | Madrid | 1 | 50 € | museos, comida |
| Fin de semana económico | Barcelona | 2 | 150 € | naturaleza, museos |
| Ruta cultural | Sevilla | 3 | 300 € | cultura, monumentos |
| Lluvia (norte) | Bilbao | 2 | 200 € | gastronomia, museos |

Las cuatro APIs cubren todo el mundo, así que cualquier ciudad real es
válida. Si la ciudad no existe en alguna API, ese agente concreto
devolverá un `InformePercepcion` con `errorMensaje` y los demás
construirán la ruta sin esa fuente.

---

## 5. Diagrama de la arquitectura del sistema

```
                       +-----------------+
                       |  AgenteUsuario  |   (ventana Swing
                       |  (Swing GUI)    |    InterfazPreferenciasFrame)
                       +────────┬────────+
                                │  1) busca DF "fuente-percepcion"
                                │  2) REQUEST(PreferenciasUsuario)
                                │     fan-out a los 4 agentes
        ┌─────────────┬─────────┴─────────┬─────────────┐
        ▼             ▼                   ▼             ▼
 +─────────────+  +──────────────+  +──────────────+  +──────────────+
 | AgenteClima |  | AgenteLugares|  | AgenteHoteles|  | AgenteEventos|
 | OpenWeather |  | OpenTripMap  |  | Booking      |  | Ticketmaster |
 |             |  |              |  | (RapidAPI)   |  |  Discovery   |
 +──────┬──────+  +──────┬───────+  +──────┬───────+  +──────┬───────+
        │ REQUEST(InformePercepcion: solo SU fragmento)      │
        │ mismo conversationId para que el recomendador      │
        │ pueda agrupar los fragmentos                       │
        └─────────────┬─────────────────────────┬────────────┘
                      ▼                         ▼
                +───────────────────────────────+
                |       AgenteRecomendador       |
                |  • acumula los 4 fragmentos    |
                |    por conversationId          |
                |  • motor de scoring:           |
                |    0.45·int + 0.25·pres        |
                |    + 0.15·clima + 0.10·pop     |
                |    + 0.05·div                  |
                |  • construye RutaRecomendada   |
                +────────────────┬───────────────+
                                 │ INFORM(RutaRecomendada)
                                 ▼
                +───────────────────────────────+
                |        AgenteInterfaz         |
                |  ventana Swing con la ruta:   |
                |  • lugares puntuados          |
                |  • hotel recomendado          |
                |  • eventos sugeridos          |
                |  • coste total estimado       |
                +───────────────────────────────+

  ─────────────────────────  Servicios en el DF  ─────────────────────────
  fuente-percepcion ........ los 4 agentes de percepción (descubrimiento
                              dinámico desde el AgenteUsuario)
  percepcion-clima / lugares / hoteles / eventos ..... uno por agente
  recomendar-ruta .......... AgenteRecomendador
  mostrar-ruta ............. AgenteInterfaz
```

Todos los agentes se comunican mediante **mensajes ACL FIPA**. Cada
`CyclicBehaviour` filtra los suyos con `MessageTemplate.and(perf, onto)` y
se bloquea con `block()` hasta que llega un mensaje que case con el
filtro (cumple el requisito de *al menos un filtro de mensajes en modo
bloqueante*).

### Estructura del repositorio

```
proyecto/
├── lib/jade.jar                       # JADE 4.6.0 (sin descarga externa)
├── pom.xml                            # Build Maven (Java 17, org.json)
├── run.sh                             # Script de arranque
├── apikeys.properties.example         # Plantilla de claves (sin valores)
├── README.md                          # Este documento
└── src/main/java/com/sistemainteligentes/
    ├── App.java
    ├── comun/                         # Modelos serializables compartidos
    │   ├── PreferenciasUsuario.java
    │   ├── InformePercepcion.java
    │   ├── DatosClima.java
    │   ├── LugarTuristico.java
    │   ├── Hotel.java
    │   └── EventoTuristico.java
    ├── usuario/                       # AgenteUsuario + GUI Swing
    ├── percepcion/                    # 4 agentes + fuentes HTTP + base abstracta
    ├── recomendador/                  # AgenteRecomendador + motor de scoring
    └── interfaz/                      # AgenteInterfaz + ventana de salida
```

---

## 6. Declaración de uso de IA

Se ha utilizado un asistente de IA (**Claude**, Anthropic) en las
siguientes tareas del proyecto:

- **Estructura del repositorio y división del trabajo.** El asistente
  propuso el esqueleto de paquetes (`usuario/`, `percepcion/`,
  `recomendador/`, `interfaz/`, `comun/`) y la estrategia de ramas Git
  para que los tres miembros del grupo pudieran trabajar en paralelo sin
  conflictos de merge.
- **Refactor a clase base.** La jerarquía `BasePercepcionAgent` que
  factoriza el ciclo de vida común de los cuatro agentes de percepción
  (registro en el DF, behaviour cíclico bloqueante, reenvío al
  recomendador con `conversationId` propagado) se diseñó con apoyo del
  asistente.
- **Integración de las cuatro APIs externas.** El asistente generó los
  esqueletos de los clientes HTTP (`FuenteClima`, `FuenteLugares`,
  `FuenteHoteles`, `FuenteEventos`) usando `java.net.http.HttpClient` y
  `org.json`, incluida la autenticación por cabeceras `X-RapidAPI-Key /
  X-RapidAPI-Host` para Booking. Los autores verificaron y ajustaron los
  endpoints (p.ej. corregir `languagecode=es-es` a `languagecode=es` en
  RapidAPI Booking) tras hacer las llamadas reales.
- **Esqueletos para los compañeros.** Los puntos de partida del
  `AgenteRecomendador` (`Map<conversationId, fragmentos>` con
  `MessageTemplate` bloqueante) y del `AgenteInterfaz` (`CyclicBehaviour`
  + Swing) se redactaron con apoyo del asistente; sobre esa base los
  miembros del equipo implementaron el motor de scoring final y la
  ventana de visualización.
- **Documentación.** Este `README.md`, los `javadoc` de las clases
  principales y los comentarios explicativos en el código se han
  redactado con apoyo del asistente.
- **Pruebas end‑to‑end.** Los *smoke tests* contra las cuatro APIs
  reales y el flujo completo de los 7 agentes (lanzamiento JADE, fan-out,
  agregación, ruta final) se diseñaron y ejecutaron con asistencia para
  capturar los logs antes de la defensa.

Toda la lógica funcional ha sido revisada por los autores y se ajusta al
material visto en clase (transparencias de JADE 2025‑2026). El registro
y consulta de servicios en el Directory Facilitator, el uso de
`MessageTemplate` con filtros bloqueantes y la mensajería ACL siguen
literalmente los patrones presentados en las sesiones de teoría.
