#!/usr/bin/env bash
# Script de arranque del sistema multiagente.
#
# Lanza la plataforma JADE con todos los agentes implementados en este
# repo (AgenteUsuario + 4 agentes de percepcion). Cuando los companeros
# integren AgenteRecomendador y AgenteInterfaz solo hay que anadirlos a
# la variable AGENTES de mas abajo.
#
# Uso:
#   ./run.sh             # Lanza usuario + los 4 de percepcion (modo grafico)
#   ./run.sh --headless  # Solo los 4 de percepcion (servidores sin display)
#
# Las API keys se toman de:
#   1. Variables de entorno (OPENWEATHER_API_KEY, OPENTRIPMAP_API_KEY,
#      AMADEUS_CLIENT_ID, AMADEUS_CLIENT_SECRET, TICKETMASTER_API_KEY).
#   2. Si no estan, del fichero apikeys.properties en la raiz (gitignored).
#   3. Si tampoco estan, cada agente usa su catalogo simulado de respaldo.

set -e

cd "$(dirname "$0")"

# 1. Verificar que tenemos las clases compiladas.
if [ ! -d "target/classes" ]; then
  echo "[run.sh] Compilando proyecto con Maven..."
  mvn -q -DskipTests compile
fi

if [ ! -f "target/classes/com/sistemainteligentes/usuario/AgenteUsuario.class" ]; then
  echo "[run.sh] AVISO: AgenteUsuario no esta compilado."
  echo "         Estas en la rama '$(git branch --show-current)'?"
  echo "         La rama 'master' contiene TODOS los agentes."
  echo "         Para cambiar: git checkout master"
  exit 1
fi

for clase in AgenteClima AgenteLugares AgenteHoteles AgenteEventos; do
  if [ ! -f "target/classes/com/sistemainteligentes/percepcion/$clase.class" ]; then
    echo "[run.sh] AVISO: $clase no esta compilado."
    echo "         Estas en la rama '$(git branch --show-current)'?"
    echo "         La rama 'master' contiene los cuatro agentes de percepcion."
    exit 1
  fi
done

# El AgenteRecomendador y AgenteInterfaz son opcionales: el sistema arranca
# igualmente y los agentes de percepcion mostraran FAILURE hasta que esten.
RECOMENDADOR_OK=true
INTERFAZ_OK=true
[ -f "target/classes/com/sistemainteligentes/recomendador/AgenteRecomendador.class" ] || RECOMENDADOR_OK=false
[ -f "target/classes/com/sistemainteligentes/interfaz/AgenteInterfaz.class" ] || INTERFAZ_OK=false

# 2. Localizar org.json en el repositorio local de Maven.
JSON_JAR="$HOME/.m2/repository/org/json/json/20231013/json-20231013.jar"
if [ ! -f "$JSON_JAR" ]; then
  echo "[run.sh] Descargando org.json desde Maven Central..."
  mvn -q -DskipTests dependency:resolve >/dev/null
fi

# 3. Construir el classpath.
CP="target/classes:lib/jade.jar:$JSON_JAR"

# 4. Decidir que agentes lanzar.
#    OJO: JADE espera la lista de agentes en UN UNICO argumento separados
#    por ';' (no por espacios), precedido por '-agents'. Ver tema 4 de
#    las transparencias.
PERCEPCION="clima:com.sistemainteligentes.percepcion.AgenteClima"
PERCEPCION="$PERCEPCION;lugares:com.sistemainteligentes.percepcion.AgenteLugares"
PERCEPCION="$PERCEPCION;hoteles:com.sistemainteligentes.percepcion.AgenteHoteles"
PERCEPCION="$PERCEPCION;eventos:com.sistemainteligentes.percepcion.AgenteEventos"

EXTRA=""
if [ "$INTERFAZ_OK" = "true" ]; then
  # IMPORTANTE: el interfaz se registra ANTES que el recomendador para que
  # cuando el recomendador haga su primer DFService.search ya lo encuentre.
  EXTRA="$EXTRA;interfaz:com.sistemainteligentes.interfaz.AgenteInterfaz"
fi
if [ "$RECOMENDADOR_OK" = "true" ]; then
  EXTRA="$EXTRA;recomendador:com.sistemainteligentes.recomendador.AgenteRecomendador"
fi

if [ "$1" = "--headless" ]; then
  AGENTES="$PERCEPCION$EXTRA"
  echo "[run.sh] Modo headless: percepcion + recomendador/interfaz si existen."
else
  AGENTES="$PERCEPCION$EXTRA;usuario:com.sistemainteligentes.usuario.AgenteUsuario"
fi

if [ "$RECOMENDADOR_OK" = "false" ]; then
  echo "[run.sh] (info) AgenteRecomendador no compilado, se lanza el sistema sin el."
fi
if [ "$INTERFAZ_OK" = "false" ]; then
  echo "[run.sh] (info) AgenteInterfaz no compilado, se lanza el sistema sin el."
fi

# 5. Aviso si las claves no estan configuradas (no es bloqueante).
if [ -z "$OPENWEATHER_API_KEY" ] && [ -z "$OPENTRIPMAP_API_KEY" ] \
   && [ -z "$RAPIDAPI_KEY" ] && [ -z "$TICKETMASTER_API_KEY" ] \
   && [ ! -f "apikeys.properties" ]; then
  echo "[run.sh] (info) No hay API keys configuradas. Cada agente caera"
  echo "         a su catalogo simulado (Madrid/Barcelona). Para usar"
  echo "         las APIs reales: copia apikeys.properties.example a"
  echo "         apikeys.properties y rellena las claves, o exporta las"
  echo "         variables de entorno (ver README seccion 4.6)."
fi

# 6. Lanzar JADE.
echo "[run.sh] Lanzando JADE con:"
echo "         $AGENTES"
exec java -cp "$CP" jade.Boot -gui -agents "$AGENTES"
