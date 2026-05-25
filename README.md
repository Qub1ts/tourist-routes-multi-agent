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

```bash
# 1. Clonar el repositorio
git clone https://github.com/Qub1ts/multi-agent.git
cd multi-agent

# 2. Compilar el proyecto (descarga las dependencias declaradas en pom.xml)
mvn -DskipTests compile
```

---

## 2. Captura de dependencias necesarias para instalar el proyecto

### Dependencias gestionadas por Maven

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

---

## 3. Instrucciones de ejecución

### Opción A — script `run.sh`

Desde la raíz del proyecto:

```bash
mvn -q -DskipTests compile
./run.sh
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
---

## 4. Datos de ejemplo para ejecutar la práctica

La ventana del `AgenteUsuario` se inicializa con un caso de prueba:

| Campo | Valor por defecto |
|---|---|
| Ciudad | `Madrid` |
| Días disponibles | `1` |
| Presupuesto máximo (€) | `50` |
| Intereses | `museos, comida` |

Otros casos de prueba, solo cambia los valores en la
ventana antes de pulsar *Enviar*:

| Caso | Ciudad | Días | Presupuesto | Intereses |
|---|---|---|---|---|
| Demo enunciado | Madrid | 1 | 50 € | museos, comida |
| Fin de semana económico | Barcelona | 2 | 150 € | naturaleza, museos |
| Ruta cultural | Sevilla | 3 | 300 € | cultura, monumentos |
| Lluvia (norte) | Bilbao | 2 | 200 € | gastronomia, museos |

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

---

## 6. Declaración de uso de IA

Se ha utilizado un asistente de IA (**Claude**, Anthropic) en las
siguientes tareas del proyecto:

- **Estructura del sistemas** nos ayudo a organizar el esqueleto de paquetes (`usuario/`, `percepcion/`,`recomendador/`, `interfaz/`, `comun/`) y nos recomendo que agentes llevar a cabo.
- **Arranque** el run.sh fue propuesto por IA para ejecutar el sistema desde un solo comando sh.

Toda la lógica funcional ha sido revisada por el grupo
