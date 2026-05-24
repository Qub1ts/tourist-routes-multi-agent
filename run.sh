#!/usr/bin/env bash
# ============================================================
# Script de arranque del sistema multiagente turístico
# ============================================================
#
# Lanza:
#   - AgenteUsuario
#   - AgenteClima
#   - AgenteLugares
#   - AgenteHoteles
#   - AgenteEventos
#   - AgenteRecomendador
#   - AgenteInterfaz
#
# Uso:
#   ./run.sh
#   ./run.sh --headless
#
# Las API keys se toman de:
#   1. Variables de entorno
#   2. apikeys.properties
#   3. Catálogo simulado de respaldo
# ============================================================

set -e

cd "$(dirname "$0")"

# ============================================================
# 1. Compilar automáticamente si hace falta
# ============================================================

if [ ! -d "target/classes" ]; then
  echo "[run.sh] Compilando proyecto con Maven..."
  mvn -q -DskipTests compile
fi

# ============================================================
# 2. Verificar clases principales
# ============================================================

CLASES=(
  "usuario/AgenteUsuario"
  "percepcion/AgenteClima"
  "percepcion/AgenteLugares"
  "percepcion/AgenteHoteles"
  "percepcion/AgenteEventos"
  "recomendador/AgenteRecomendador"
  "interfaz/AgenteInterfaz"
)

for clase in "${CLASES[@]}"; do
  if [ ! -f "target/classes/com/sistemainteligentes/$clase.class" ]; then
    echo "[run.sh] ERROR: No existe:"
    echo "         target/classes/com/sistemainteligentes/$clase.class"
    echo ""
    echo "Compila el proyecto:"
    echo "    mvn -DskipTests compile"
    exit 1
  fi
done

# ============================================================
# 3. Localizar org.json
# ============================================================

JSON_JAR="$HOME/.m2/repository/org/json/json/20231013/json-20231013.jar"

if [ ! -f "$JSON_JAR" ]; then
  echo "[run.sh] Resolviendo dependencia org.json..."
  mvn -q dependency:resolve
fi

# ============================================================
# 4. Construir classpath
# ============================================================

CP="target/classes:lib/jade.jar:$JSON_JAR"

# ============================================================
# 5. Definir agentes
# ============================================================

PERCEPCION="clima:com.sistemainteligentes.percepcion.AgenteClima"
PERCEPCION="$PERCEPCION;lugares:com.sistemainteligentes.percepcion.AgenteLugares"
PERCEPCION="$PERCEPCION;hoteles:com.sistemainteligentes.percepcion.AgenteHoteles"
PERCEPCION="$PERCEPCION;eventos:com.sistemainteligentes.percepcion.AgenteEventos"

RECOMENDADOR="recomendador:com.sistemainteligentes.recomendador.AgenteRecomendador"

INTERFAZ="interfaz:com.sistemainteligentes.interfaz.AgenteInterfaz"

USUARIO="usuario:com.sistemainteligentes.usuario.AgenteUsuario"

# ============================================================
# 6. Headless o modo gráfico
# ============================================================

if [ "$1" = "--headless" ]; then

  AGENTES="$PERCEPCION;$RECOMENDADOR;$INTERFAZ"

  echo "[run.sh] Ejecutando en modo headless..."

else

  AGENTES="$PERCEPCION;$RECOMENDADOR;$INTERFAZ;$USUARIO"

fi

# ============================================================
# 7. Aviso sobre APIs
# ============================================================

if [ -z "$OPENWEATHER_API_KEY" ] \
   && [ -z "$OPENTRIPMAP_API_KEY" ] \
   && [ -z "$RAPIDAPI_KEY" ] \
   && [ -z "$TICKETMASTER_API_KEY" ] \
   && [ ! -f "apikeys.properties" ]; then

  echo "[run.sh] AVISO:"
  echo "No se encontraron API keys."
  echo "El sistema usará catálogos simulados."
  echo ""
  echo "Crea:"
  echo "    apikeys.properties"
  echo ""
  echo "o exporta variables de entorno."
fi

# ============================================================
# 8. Lanzar JADE
# ============================================================

echo ""
echo "[run.sh] Lanzando plataforma JADE..."
echo ""
echo "$AGENTES"
echo ""

exec java -cp "$CP" jade.Boot -gui -agents "$AGENTES"