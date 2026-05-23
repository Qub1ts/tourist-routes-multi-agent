# Sistema inteligente de recomendación de rutas turísticas

Práctica de Sistemas Inteligentes (curso 2025‑2026) implementada como sistema
multiagente sobre la plataforma **JADE**.

El sistema recomienda rutas turísticas según el clima, el presupuesto, el
tiempo disponible y las preferencias del usuario. Ejemplo de uso:
> *"Tengo 1 día en Madrid, quiero gastar poco y me interesan museos y comida."*

El sistema consulta información externa, procesa las opciones y devuelve una
ruta recomendada con costes aproximados.

---

## 1. Arquitectura

```
                  +-----------------+
                  |  AgenteUsuario  |  (interfaz Swing)
                  +────────┬────────+
                           │ busca DF "fuente-percepcion"
                           │ REQUEST(PreferenciasUsuario) en fan-out
        ┌──────────────────┼──────────────────┬──────────────────┐
        ▼                  ▼                  ▼                  ▼
 +─────────────+    +─────────────+    +─────────────+    +─────────────+
 |  AgenteClima |    |AgenteLugares|    |AgenteHoteles|    |AgenteEventos|
 | OpenWeather  |    | OpenTripMap |    | RapidAPI    |    |Ticketmaster |
 |              |    |             |    | (Booking)   |    |             |
 +──────┬──────+    +──────┬──────+    +──────┬──────+    +──────┬──────+
        │                  │                  │                  │
        │ REQUEST(InformePercepcion: solo SU fragmento)           │
        │ mismo conversationId para que el recomendador los una   │
        └──────────────────┴────────┬─────────┴──────────────────┘
                                    ▼
                          +─────────────────────+
                          |  AgenteRecomendador |  ← compañero/a
                          | acumula fragmentos  |
                          | por conversationId  |
                          | y construye la ruta |
                          +──────────┬──────────+
                                     ▼ INFORM(RutaRecomendada)
                          +─────────────────────+
                          |    AgenteInterfaz   |  ← compañero/a
                          |   muestra la ruta   |
                          +─────────────────────+
```

Flujo paso a paso:

1. El usuario rellena sus preferencias en la ventana del `AgenteUsuario`
   y pulsa "Enviar".
2. `AgenteUsuario` busca en el DF **todos** los agentes que ofrezcan el
   servicio genérico `fuente-percepcion` y manda un `REQUEST` con las
   mismas `PreferenciasUsuario` a cada uno (**fan-out**). Todos los
   mensajes comparten el mismo `conversationId`.
3. Cada agente de percepción consulta **su** API externa
   (`AgenteClima → OpenWeatherMap`, `AgenteLugares → OpenTripMap`,
   `AgenteHoteles → Booking.com vía RapidAPI`,
   `AgenteEventos → Ticketmaster`), construye
   un `InformePercepcion` con solo su fragmento de datos y se lo envía
   al `AgenteRecomendador`. También manda un `INFORM` de ACK al usuario
   para que la interfaz informe del progreso.
4. `AgenteRecomendador` (lo hace el compañero/a) recibe los `N`
   fragmentos del mismo `conversationId`, los acumula, aplica sus
   reglas de filtrado/puntuación según las preferencias y construye la
   ruta recomendada.
5. `AgenteRecomendador` envía la ruta al `AgenteInterfaz` con
   `INFORM`. Opcionalmente confirma al `AgenteUsuario`.
6. `AgenteInterfaz` muestra la ruta, los hoteles, los eventos, el
   clima esperado y los costes al usuario.

Todos los agentes se registran/consultan en el **Directory Facilitator
(DF)** de JADE y se comunican mediante mensajes **ACL** (FIPA-ACL). Cada
comportamiento implementa filtrado de mensajes con `MessageTemplate` y
recepción en modo bloqueante (`block()` / `blockingReceive()`).

| Agente | Responsable | Servicios DF | Estado |
|---|---|---|---|
| `AgenteUsuario` | Julio | – (consumidor) | ✅ implementado y probado |
| `AgenteClima` | Julio | `fuente-percepcion`, `percepcion-clima` | ✅ implementado + OpenWeather real |
| `AgenteLugares` | Julio | `fuente-percepcion`, `percepcion-lugares` | ✅ implementado + OpenTripMap real |
| `AgenteHoteles` | Julio | `fuente-percepcion`, `percepcion-hoteles` | ✅ implementado + Booking/RapidAPI real |
| `AgenteEventos` | Julio | `fuente-percepcion`, `percepcion-eventos` | ✅ implementado + Ticketmaster real |
| `AgenteRecomendador` | Compañero/a | `recomendar-ruta` | ⏳ pendiente (ver §6) |
| `AgenteInterfaz` | Compañero/a | `mostrar-ruta` | ⏳ pendiente (ver §6) |

**Estado actual del sistema:** los 5 agentes de Julio funcionan end-to-end
sobre las 4 APIs reales (clima, lugares, hoteles, eventos). Falta solo
el `AgenteRecomendador` y el `AgenteInterfaz` que están a cargo de los
compañeros (sección 6).

Que cada agente de percepción registre el tipo genérico `fuente-percepcion`
permite añadir nuevas fuentes (transporte público, eventos deportivos,
gastronomía especializada...) sin tocar al `AgenteUsuario`: basta con
crear una nueva subclase de `BasePercepcionAgent` y lanzarla; el usuario
la descubrirá automáticamente vía DF.

---

## 2. Estructura del repositorio

```
proyecto/
├── lib/jade.jar                  # JADE 4.6.0 (incluido)
├── pom.xml                       # Maven (Java 17, org.json)
├── apikeys.properties.example    # Plantilla para las 5 API keys
├── run.sh                        # Script de arranque del sistema
├── src/main/java/com/sistemainteligentes/
│   ├── App.java                  # Punto de entrada informativo
│   ├── practica/                 # Ejercicios de las transparencias
│   ├── comun/                    # Modelos serializables compartidos
│   │   ├── PreferenciasUsuario.java
│   │   ├── DatosClima.java
│   │   ├── LugarTuristico.java
│   │   ├── Hotel.java
│   │   ├── EventoTuristico.java
│   │   └── InformePercepcion.java     # fragmento que cada percepción
│   │                                    # manda al recomendador
│   ├── usuario/                  # AgenteUsuario  (rama agente-controlador)
│   │   ├── AgenteUsuario.java
│   │   ├── InterfazPreferenciasFrame.java
│   │   ├── EnviarPreferenciasBehaviour.java  # fan-out DF
│   │   └── EsperarRespuestaBehaviour.java
│   ├── percepcion/               # 4 agentes  (rama agente-percepcion)
│   │   ├── BasePercepcionAgent.java        # ciclo de vida factorizado
│   │   ├── AtenderConsultasBehaviour.java  # common behaviour
│   │   ├── ClienteHttp.java                # helper HTTP (GET/POST)
│   │   ├── ConfiguracionApis.java          # resuelve API keys
│   │   ├── AgenteClima.java                # ┐
│   │   ├── AgenteLugares.java              # │ 4 agentes
│   │   ├── AgenteHoteles.java              # │ especificos
│   │   ├── AgenteEventos.java              # ┘
│   │   ├── FuenteClima.java                # ┐
│   │   ├── FuenteLugares.java              # │ Clientes HTTP
│   │   ├── FuenteHoteles.java              # │ por proveedor
│   │   └── FuenteEventos.java              # ┘
│   ├── recomendador/             # AgenteRecomendador (compañero/a)
│   └── interfaz/                 # AgenteInterfaz (compañero/a)
└── src/test/java/...
```

Cada rama contiene únicamente los agentes que le corresponden. La rama
`master` ya tiene integrados todos los agentes implementados por Julio;
los compañeros/as solo tienen que mergear sus ramas a master para
completar el sistema.

---

## 3. Instalación

### Requisitos

- **Java 17** o superior (`java -version`).
- **Maven 3.6.3** o superior (`mvn -version`).
- JADE 4.6.0 — ya incluido en `lib/jade.jar` (no requiere instalación
  externa).

### Compilar

Desde la raíz del proyecto:

```bash
mvn -DskipTests compile
```

Si es la primera vez también:

```bash
mvn -DskipTests install:install-file \
    -Dfile=lib/jade.jar \
    -DgroupId=com.tilab.jade -DartifactId=jade -Dversion=4.6.0 -Dpackaging=jar
```

(Solo es necesario si Maven se queja de no encontrar JADE; el `pom.xml`
referencia el JAR como `system` por defecto.)

---

## 4. Ejecución

JADE se lanza con la clase `jade.Boot`. El classpath debe incluir las clases
compiladas y `lib/jade.jar`.

### 4.1 Atajo recomendado: `./run.sh`

Desde la raíz del proyecto, en una rama que contenga **los dos** agentes
(p.e. `master` después de los merges, o tu rama local de integración):

```bash
./run.sh
```

El script compila si hace falta, monta el classpath con `lib/jade.jar` y
`org.json` desde `~/.m2`, carga las claves (env var o `apikeys.properties`)
y lanza JADE con los agentes en la misma plataforma.

Si solo quieres ver la consola RMA vacía y crear agentes a mano:

```bash
# macOS / Linux
java -cp "target/classes:lib/jade.jar" jade.Boot -gui

# Windows (PowerShell)
java -cp "target\classes;lib\jade.jar" jade.Boot -gui
```

El flag `-gui` abre la consola **RMA** (Remote Agent Management). Desde ahí
puedes crear agentes a mano, lanzar el `DummyAgent` y el `SnifferAgent`, o
inspeccionar el DF.

### 4.2 Lanzar el sistema completo (a mano, sin `run.sh`)

Recomendado para la defensa de la práctica: una única plataforma con
todos los agentes registrados de golpe.

**IMPORTANTE:** en `jade.Boot` los agentes van en **UN ÚNICO argumento
separados por `;`** (no por espacios) y precedidos del flag `-agents`.
Si los separas con espacios, JADE intenta cargar el primero como
fichero de configuración y solo arranca uno. Esta es una trampa muy
típica.

Comando completo con los 5 agentes de Julio (los 4 de percepción + el
usuario). Cuando los compañeros integren los suyos, basta con añadirlos
a la lista:

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

El orden dentro del `-agents` no afecta al flujo de mensajes (todos
arrancan en paralelo en la misma plataforma). Lo único que importa es
que todos estén vivos antes de que el usuario pulse "Enviar
preferencias" en la ventana Swing; eso lo garantiza el propio
`jade.Boot` antes de devolver el control.

### 4.3 Lanzar solo los agentes de esta rama

- **Rama `agente-controlador`** (Julio): solo el `AgenteUsuario`.

  ```bash
  java -cp "$CP" jade.Boot -gui -agents \
      "usuario:com.sistemainteligentes.usuario.AgenteUsuario"
  ```

  Abre la ventana Swing de captura de preferencias. Si no hay agentes
  de percepción registrados en el DF, el botón de envío lo notifica.

- **Rama `agente-percepcion`** (Julio): los 4 agentes de percepción.

  ```bash
  java -cp "$CP" jade.Boot -gui -agents \
      "clima:com.sistemainteligentes.percepcion.AgenteClima;\
lugares:com.sistemainteligentes.percepcion.AgenteLugares;\
hoteles:com.sistemainteligentes.percepcion.AgenteHoteles;\
eventos:com.sistemainteligentes.percepcion.AgenteEventos"
  ```

  Cada uno registra dos servicios en el DF (`fuente-percepcion` para
  descubrimiento dinámico y su tipo específico
  `percepcion-{clima|lugares|hoteles|eventos}`) y queda escuchando
  `REQUEST` con ontología `fuente-percepcion` y contenido
  `comun.PreferenciasUsuario`. Tras consultar su API real reenvía un
  `REQUEST` con `comun.InformePercepcion` al agente que ofrezca el
  servicio `recomendar-ruta`.

### 4.4 Ejecución multi‑máquina (defensa)

JADE permite arrancar contenedores secundarios que se conecten al *Main
Container*. En la máquina que hace de servidor:

```bash
java -cp "$CP" jade.Boot -gui -host 0.0.0.0
```

En cada cliente (sustituyendo `IP_SERVIDOR` por la IP del servidor):

```bash
java -cp "$CP" jade.Boot -container -host IP_SERVIDOR \
    usuario:com.sistemainteligentes.usuario.AgenteUsuario
```

### 4.5 Datos de ejemplo

La interfaz del `AgenteUsuario` se rellena por defecto con un caso de prueba
extraído del enunciado:

| Campo | Valor por defecto |
|---|---|
| Ciudad | `Madrid` |
| Días disponibles | `1` |
| Presupuesto máximo (€) | `50` |
| Intereses | `museos, comida` |

Al pulsar **Enviar preferencias** se construye un objeto
`PreferenciasUsuario` y se hace **fan-out**: el `AgenteUsuario` busca en
el DF todos los proveedores de `fuente-percepcion` y envía la misma
petición a cada uno (clima, lugares, hoteles, eventos).

Cada agente de percepción consulta una API externa real (configurable,
ver sección 4.6). Si no hay conexión o la API falla, ese agente concreto
cae a su catálogo simulado para que la demo siga funcionando.

### 4.6 Configurar las APIs externas

El sistema usa **cuatro APIs gratuitas**, una por cada agente de
percepción:

| Dato | Proveedor | Variables de entorno | Cómo conseguir la clave |
|---|---|---|---|
| Clima | [OpenWeatherMap](https://openweathermap.org/api) | `OPENWEATHER_API_KEY` | Registro → My API Keys → copiar |
| Lugares | [OpenTripMap](https://opentripmap.io/docs) | `OPENTRIPMAP_API_KEY` | Registro → API key |
| Hoteles | [Booking.com vía RapidAPI](https://rapidapi.com/DataCrawler/api/booking-com15) | `RAPIDAPI_KEY` | Registro en RapidAPI → suscribirse al plan "Basic" de **booking-com15** (gratis, 100 llamadas/mes) → copiar la clave del panel |
| Eventos | [Ticketmaster Discovery](https://developer.ticketmaster.com/products-and-docs/apis/getting-started/) | `TICKETMASTER_API_KEY` | My Apps → Add new app → Consumer Key |

> Nota sobre RapidAPI: tu clave del panel de RapidAPI sirve para *todas*
> las APIs a las que te suscribas, pero el `AgenteHoteles` está
> programado contra el host concreto `booking-com15.p.rapidapi.com`. Si
> ese host no aparece en tu panel, recibirás `HTTP 403 "You are not
> subscribed to this API."` y el agente caerá al catálogo simulado.

Hay **dos formas** de proporcionar las claves a los agentes. Cada agente
busca primero en la variable de entorno; si no existe, lee
`apikeys.properties`.

**Opción A — variables de entorno (recomendada para CI/defensa):**

```bash
export OPENWEATHER_API_KEY=xxxxxxxx
export OPENTRIPMAP_API_KEY=xxxxxxxx
export RAPIDAPI_KEY=xxxxxxxx
export TICKETMASTER_API_KEY=xxxxxxxx
./run.sh
```

**Opción B — fichero local `apikeys.properties`:**

```bash
cp apikeys.properties.example apikeys.properties
# editar apikeys.properties y rellenar las 5 claves
```

El fichero `apikeys.properties` está incluido en `.gitignore`: nunca se
subirá al repo. **No commitéis las claves bajo ningún concepto.**

Si alguna clave está vacía o la API falla, ese agente concreto **cae
automáticamente** a su catálogo simulado (datos fijos para Madrid y
Barcelona). El resto de agentes que sí tengan clave siguen consumiendo
sus APIs reales. Eso garantiza que la demo siempre tira aunque el wifi
falle el día de la defensa.

---

## 5. Contratos de mensajería (para los compañeros/as)

El flujo del pipeline es:
`Usuario → (fan-out) → 4 agentes de percepción → Recomendador → Interfaz`.

Todos los mensajes que salen del `AgenteUsuario` para una misma sesión
comparten el **mismo `conversationId`** (formato `ruta-<timestamp>`). El
`AgenteRecomendador` debe acumular los `InformePercepcion` que vayan
llegando con ese mismo id hasta tener los `N` esperados (o un timeout)
para producir la ruta.

| De → A | Performativa | Ontología | Contenido (`setContentObject`) |
|---|---|---|---|
| Usuario → cada agente percepción | `REQUEST` | `fuente-percepcion` | `comun.PreferenciasUsuario` |
| Percepción → Usuario | `INFORM` (ACK) | `fuente-percepcion` | `String` informando que su informe ya salió |
| Percepción → Usuario | `FAILURE` (si falla) | `fuente-percepcion` | `String` con motivo |
| Percepción → Recomendador | `REQUEST` | `recomendar-ruta` | `comun.InformePercepcion` (solo se rellena el fragmento que corresponde a su fuente; campo `fuente` = "clima"/"lugares"/"hoteles"/"eventos") |
| Recomendador → Interfaz | `INFORM` | `mostrar-ruta` | `comun.RutaRecomendada` (a definir por el equipo del recomendador) |
| Recomendador → Usuario | `INFORM` (opcional) | `recomendar-ruta` | confirmación o resumen en `String` |

Servicios en el DF:

- `AgenteClima` registra `fuente-percepcion` + `percepcion-clima`.
- `AgenteLugares` registra `fuente-percepcion` + `percepcion-lugares`.
- `AgenteHoteles` registra `fuente-percepcion` + `percepcion-hoteles`.
- `AgenteEventos` registra `fuente-percepcion` + `percepcion-eventos`.
- `AgenteRecomendador` (compañero) debe registrar `recomendar-ruta`.
- `AgenteInterfaz` (compañero) debe registrar `mostrar-ruta`.
- `AgenteUsuario` no registra servicio (es un cliente).

Los `MessageTemplate` de cada agente combinan `MatchPerformative` con
`MatchOntology` para evitar que un comportamiento "robe" mensajes
destinados a otro.

---

## 6. Guía paso a paso para los compañeros/as

Esta sección tiene todo lo que necesitan los compañeros para integrar sus
dos agentes (`AgenteRecomendador` y `AgenteInterfaz`) **sin tener que
mirar el código de Julio**. Solo deben seguir los pasos, copiar los
esqueletos y rellenar la lógica de su agente.

### 6.1 Setup inicial (5 minutos)

```bash
# 1. Clonar el repo
git clone https://github.com/Qub1ts/multi-agent.git
cd multi-agent

# 2. Crear tu rama desde master
git checkout master
git checkout -b agente-recomendador          # o "agente-interfaz", como prefieras
git push -u origin agente-recomendador

# 3. Comprobar que el sistema base compila
mvn -DskipTests compile
```

Las claves de API no son necesarias para vuestros agentes (vosotros no
llamáis a ninguna API). Si quieres probar el flujo entero localmente,
pídele a Julio el `apikeys.properties` o trabaja sin claves (los agentes
de percepción caerán a su catálogo simulado pero todo funciona).

### 6.2 Para el equipo del **AgenteRecomendador**

**Qué hace tu agente:**
1. Se registra en el DF como `recomendar-ruta`.
2. Recibe `REQUEST` con un `InformePercepcion` de cada uno de los 4
   agentes de percepción (clima, lugares, hoteles, eventos). Cada
   fragmento llega con su propio mensaje pero **todos comparten el
   mismo `conversationId`** (formato `ruta-<timestamp>`) — así sabes
   qué fragmentos pertenecen a la misma sesión.
3. Acumula los fragmentos por `conversationId` hasta tener los 4 (o
   vence un timeout, ej. 8 s).
4. Filtra/puntúa los lugares y los hoteles según las preferencias del
   usuario (`PreferenciasUsuario` viene dentro del propio
   `InformePercepcion`).
5. Emite un `INFORM` al `AgenteInterfaz` con la ruta final.

**Crea estos dos ficheros** en
`src/main/java/com/sistemainteligentes/recomendador/`:

📄 `AgenteRecomendador.java`

```java
package com.sistemainteligentes.recomendador;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class AgenteRecomendador extends Agent {
    @Override
    protected void setup() {
        registrarServicio();
        addBehaviour(new RecogerFragmentosBehaviour(this));
        System.out.println("[Recomendador] Listo.");
    }

    private void registrarServicio() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("recomendar-ruta");
        sd.setName("Recomendador de rutas turisticas");
        dfd.addServices(sd);
        try { DFService.register(this, dfd); }
        catch (FIPAException e) { e.printStackTrace(); doDelete(); }
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (FIPAException e) { /* ignore */ }
    }
}
```

📄 `RecogerFragmentosBehaviour.java`

```java
package com.sistemainteligentes.recomendador;

import com.sistemainteligentes.comun.*;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.io.IOException;
import java.util.*;

public class RecogerFragmentosBehaviour extends CyclicBehaviour {

    // Filtro bloqueante: REQUEST con ontologia "recomendar-ruta"
    private static final MessageTemplate FILTRO = MessageTemplate.and(
        MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
        MessageTemplate.MatchOntology("recomendar-ruta"));

    // Estado por conversationId: lista de fragmentos recibidos
    private final Map<String, List<InformePercepcion>> sesiones = new HashMap<>();

    public RecogerFragmentosBehaviour(Agent a) { super(a); }

    @Override
    public void action() {
        ACLMessage msg = myAgent.receive(FILTRO);
        if (msg == null) { block(); return; }

        try {
            InformePercepcion frag = (InformePercepcion) msg.getContentObject();
            String conv = msg.getConversationId();
            sesiones.computeIfAbsent(conv, k -> new ArrayList<>()).add(frag);
            System.out.println("[Recomendador] Fragmento " + frag.getFuente()
                + " recibido (conv=" + conv + ")");

            // Cuando tengamos los 4 fragmentos (uno por cada fuente de
            // percepcion), procesamos y enviamos la ruta.
            if (sesiones.get(conv).size() >= 4) {
                List<InformePercepcion> todos = sesiones.remove(conv);
                procesarYEnviar(conv, todos);
            }
        } catch (UnreadableException e) {
            System.err.println("[Recomendador] Contenido invalido: " + e.getMessage());
        }
    }

    private void procesarYEnviar(String conv, List<InformePercepcion> fragmentos) {
        // TODO: aqui va VUESTRA logica de discriminacion/puntuacion.
        // De momento solo construye una ruta "trivial" pegando todo.
        PreferenciasUsuario prefs = fragmentos.get(0).getPreferencias();
        StringBuilder ruta = new StringBuilder("Ruta para " + prefs.getCiudad() + ":\n");
        for (InformePercepcion f : fragmentos) {
            ruta.append("- ").append(f).append("\n");
        }
        // Aplicar reglas: filtrar por presupuesto, ordenar por valoracion,
        // descartar lugares cerrados a la hora prevista, etc.

        enviarAlInterfaz(conv, ruta.toString());
    }

    private void enviarAlInterfaz(String conv, String contenidoRuta) {
        DFAgentDescription plantilla = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("mostrar-ruta");
        plantilla.addServices(sd);
        try {
            DFAgentDescription[] r = DFService.search(myAgent, plantilla);
            if (r.length == 0) {
                System.err.println("[Recomendador] No hay AgenteInterfaz en el DF.");
                return;
            }
            AID interfaz = r[0].getName();
            ACLMessage info = new ACLMessage(ACLMessage.INFORM);
            info.addReceiver(interfaz);
            info.setOntology("mostrar-ruta");
            info.setConversationId(conv);
            // Idealmente enviad un objeto serializable RutaRecomendada
            // (creadla en comun/). Como ejemplo basico mandamos un String:
            info.setContent(contenidoRuta);
            myAgent.send(info);
            System.out.println("[Recomendador] Ruta enviada al interfaz.");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}
```

> **Cuántos fragmentos esperar:** el ejemplo asume 4 (los agentes de
> Julio). Si en algún momento añadimos un quinto agente de percepción,
> cambiad `>= 4` por una consulta dinámica al DF al recibir el primer
> fragmento (`DFService.search` por `fuente-percepcion` y contar). O
> añadid un timeout con un `WakerBehaviour` que dispare la ruta con lo
> que haya llegado pasados, ej., 8 s.

**Mejora recomendada (opcional pero queda mejor en la defensa):** crea
una clase `RutaRecomendada` en `comun/` con los campos que decidáis
(lista de paradas con horario, hotel elegido, costes totales,
descripción del clima, etc.) y enviadla con `setContentObject` en vez
de `setContent(String)`. Así el `AgenteInterfaz` recibe un objeto
estructurado en vez de tener que parsear texto.

### 6.3 Para el equipo del **AgenteInterfaz**

**Qué hace tu agente:**
1. Se registra en el DF como `mostrar-ruta`.
2. Recibe `INFORM` del `AgenteRecomendador` con la ruta final.
3. Muestra la ruta en una ventana Swing (puedes inspirarte en
   `usuario/InterfazPreferenciasFrame.java` para la parte de UI).

📄 `src/main/java/com/sistemainteligentes/interfaz/AgenteInterfaz.java`

```java
package com.sistemainteligentes.interfaz;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import javax.swing.*;
import java.awt.*;

public class AgenteInterfaz extends Agent {

    private JFrame frame;
    private JTextArea texto;

    @Override
    protected void setup() {
        registrarServicio();
        construirVentana();
        addBehaviour(new RecibirRutaBehaviour());
        System.out.println("[Interfaz] Listo.");
    }

    private void registrarServicio() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("mostrar-ruta");
        sd.setName("Visor de rutas turisticas");
        dfd.addServices(sd);
        try { DFService.register(this, dfd); }
        catch (FIPAException e) { e.printStackTrace(); doDelete(); }
    }

    private void construirVentana() {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Ruta recomendada");
            texto = new JTextArea(20, 60);
            texto.setEditable(false);
            frame.add(new JScrollPane(texto), BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.setAlwaysOnTop(true); frame.toFront(); frame.setAlwaysOnTop(false);
        });
    }

    void mostrar(String contenido) {
        SwingUtilities.invokeLater(() ->
            texto.append(contenido + "\n----\n"));
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (FIPAException e) { /* ignore */ }
        if (frame != null) SwingUtilities.invokeLater(frame::dispose);
    }

    private class RecibirRutaBehaviour extends CyclicBehaviour {
        private final MessageTemplate filtro = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchOntology("mostrar-ruta"));

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(filtro);
            if (msg == null) { block(); return; }
            String contenido;
            try {
                Object o = msg.getContentObject();
                contenido = o != null ? o.toString() : msg.getContent();
            } catch (jade.lang.acl.UnreadableException e) {
                contenido = msg.getContent();
            }
            System.out.println("[Interfaz] Ruta recibida.");
            mostrar(contenido);
        }
    }
}
```

### 6.4 Probar tu agente integrado con los de Julio

Mientras desarrollas tu agente en tu rama, lo más cómodo es **mergear
master de vez en cuando** para tener al día los 5 agentes de Julio:

```bash
git fetch origin
git merge origin/master
```

Para lanzar **el sistema completo** desde tu rama (los 5 de Julio + el
tuyo), añade tu agente al comando de `run.sh` o lanza JADE a mano:

```bash
mvn -DskipTests compile
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

Pulsa "Enviar preferencias" en la ventana del usuario. En consola
deberías ver los 4 agentes de percepción consultar sus APIs, mandar sus
fragmentos al recomendador, y al recomendador montar la ruta y
enviársela al interfaz. La ventana del interfaz debe pintarla.

### 6.5 Subir tu rama y mergear a master

Cuando tu agente esté listo:

```bash
git add src/main/java/com/sistemainteligentes/recomendador/   # o /interfaz/
git commit -m "Implementar AgenteRecomendador"                # o AgenteInterfaz
git push origin agente-recomendador                            # o agente-interfaz
```

Y abrid un Pull Request en GitHub contra `master`. Como cada agente
vive en su propio subpaquete, los conflictos deberían ser mínimos o
inexistentes.

### 6.6 Resumen de los 7 agentes y quién toca qué

| Quién | Crear estos paquetes | Llamar a APIs | Registrar en DF | Recibe mensajes |
|---|---|---|---|---|
| Julio (hecho) | `usuario/`, `percepcion/` | sí (las 4) | sí (4 percepción) | sí |
| Equipo Recomendador | `recomendador/` | no | sí (`recomendar-ruta`) | REQUEST con `InformePercepcion` |
| Equipo Interfaz | `interfaz/` | no | sí (`mostrar-ruta`) | INFORM con la ruta |

---

## 7. Cumplimiento de los requisitos del enunciado

- [x] Plataforma JADE 4.6.
- [x] Agente de percepción/adquisición de información externa: **4**
      (`AgenteClima`, `AgenteLugares`, `AgenteHoteles`, `AgenteEventos`),
      cada uno consumiendo una API web real distinta.
- [ ] Agente con cálculo/inteligencia (`AgenteRecomendador`, en otra
      rama, en desarrollo por el equipo).
- [x] Agente con interfaz de usuario de entrada (`AgenteUsuario`,
      ventana Swing).
- [ ] Agente con interfaz de usuario de salida (`AgenteInterfaz`,
      en desarrollo por el equipo).
- [x] Comportamientos JADE (`CyclicBehaviour`, `OneShotBehaviour`,
      `SimpleBehaviour` con filtrado; `BasePercepcionAgent` como
      jerarquía de comportamiento reutilizable).
- [x] Registro y búsqueda en el **Directory Facilitator** (los 4
      agentes de percepción registran 2 servicios cada uno; el
      `AgenteUsuario` hace descubrimiento dinámico para el fan-out).
- [x] Intercambio de mensajes **ACL** con filtros y `block()` en modo
      bloqueante (todos los `Behaviour` ciclicos usan
      `MessageTemplate.and(MatchPerformative, MatchOntology)`).

### 7.1 Verificación de las 4 APIs externas (smoke test del 23-may)

| API | Tiempo | Verificación |
|---|---|---|
| OpenWeatherMap | ~340 ms | `Cielo claro, 27.3 grados`, humedad 28 % (Madrid en tiempo real) |
| OpenTripMap | ~360 ms | 20 lugares reales (Café Lorenzini, Serafina, …) |
| Booking via RapidAPI | ~2.6 s | 10 hoteles reales (Hostal Bernabeu II 101.94 €, Urban Hive Madrid 360 €, Room Gran Vía 46.5 €, …) |
| Ticketmaster Discovery | ~430 ms | 15 eventos reales (Museo Banksy, Bosque Encantado, …) |

Si alguna API falla (rate limit, cambio de schema), el agente
correspondiente cae automáticamente a su catálogo simulado y deja un
mensaje de error en `InformePercepcion.errorMensaje` para que el
recomendador sepa que esa fuente no es fiable en esa sesión.

---

## 8. Declaración de uso de IA

Se ha utilizado un asistente IA (Claude, Anthropic) para:

- Estructurar el repositorio y dividir el trabajo en ramas por agente.
- Redactar este `README.md` y los comentarios de los agentes.
- Sugerir el patrón de `Behaviour` + `MessageTemplate` + `block()` para la
  recepción de mensajes a partir de las transparencias de clase.

Toda la lógica funcional ha sido revisada por los autores y se ajusta al
material visto en clase (ver `Diseño Práctico/` con las transparencias de
JADE 2025‑2026).

---

## 9. Material de prácticas

El paquete `com.sistemainteligentes.practica` contiene los ejemplos
trabajados en las transparencias 2 (Creación de agentes y comportamientos) y
3 (Mensajería y Directorio). Se conservan como material de estudio y
referencia; no forman parte del sistema multiagente entregable.
