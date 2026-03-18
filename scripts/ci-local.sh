#!/bin/bash
# Script para ejecutar CI localmente
# Uso: ./scripts/ci-local.sh

set -e

echo "🚀 Iniciando CI local para Atleta Server..."

# Verificar que Docker esté disponible
if ! command -v docker &> /dev/null; then
    echo "❌ Docker no está instalado o no está en el PATH"
    exit 1
fi

if ! docker info &> /dev/null; then
    echo "❌ Docker no está ejecutándose"
    exit 1
fi

# Limpiar contenedores anteriores
echo "🧹 Limpiando contenedores anteriores..."
docker-compose -f docker-compose.ci.yml down -v 2>/dev/null || true

# Iniciar PostgreSQL
echo "📦 Iniciando PostgreSQL para testing..."
docker-compose -f docker-compose.ci.yml up -d postgres-ci

# Esperar a que PostgreSQL esté listo
echo "⏳ Esperando a que PostgreSQL esté listo..."
timeout 60 bash -c 'until docker-compose -f docker-compose.ci.yml exec -T postgres-ci pg_isready -U test -d atleta_test; do sleep 2; done'

if [ $? -ne 0 ]; then
    echo "❌ PostgreSQL no se inició correctamente"
    docker-compose -f docker-compose.ci.yml logs postgres-ci
    docker-compose -f docker-compose.ci.yml down -v
    exit 1
fi

echo "✅ PostgreSQL listo"

# Configurar variables de entorno para tests
export SPRING_PROFILES_ACTIVE=test
export DB_HOST=localhost
export DB_PORT=5432
export DB_USERNAME=test
export DB_PASSWORD=test
export DB_NAME=atleta_test

# Ejecutar tests unitarios
echo "🧪 Ejecutando tests unitarios..."
if ! ./mvnw test -Dspring.profiles.active=test; then
    echo "❌ Tests unitarios fallaron"
    docker-compose -f docker-compose.ci.yml down -v
    exit 1
fi

echo "✅ Tests unitarios pasaron"

# Ejecutar tests de integración
echo "🧪 Ejecutando tests de integración..."
if ! ./mvnw test -Dtest="*IntegrationTest" -Dspring.profiles.active=test; then
    echo "❌ Tests de integración fallaron"
    docker-compose -f docker-compose.ci.yml down -v
    exit 1
fi

echo "✅ Tests de integración pasaron"

# Ejecutar tests de migración
echo "🧪 Ejecutando tests de migración..."
if ! ./mvnw test -Dtest="*MigrationTest,FlywayIntegrationTest" -Dspring.profiles.active=test; then
    echo "❌ Tests de migración fallaron"
    docker-compose -f docker-compose.ci.yml down -v
    exit 1
fi

echo "✅ Tests de migración pasaron"

# Validar migraciones Flyway
echo "✅ Validando migraciones Flyway..."
if ! ./mvnw flyway:validate -Dspring.profiles.active=test; then
    echo "❌ Validación de migraciones falló"
    docker-compose -f docker-compose.ci.yml down -v
    exit 1
fi

echo "✅ Migraciones validadas correctamente"

# Build de la aplicación
echo "🔨 Construyendo aplicación..."
if ! ./mvnw clean package -DskipTests; then
    echo "❌ Build falló"
    docker-compose -f docker-compose.ci.yml down -v
    exit 1
fi

echo "✅ Aplicación construida exitosamente"

# Verificar que el JAR se creó
JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "❌ No se encontró el archivo JAR"
    docker-compose -f docker-compose.ci.yml down -v
    exit 1
fi

echo "✅ JAR creado: $JAR_FILE"

# Limpiar contenedores
echo "🧹 Limpiando contenedores..."
docker-compose -f docker-compose.ci.yml down -v

echo ""
echo "🎉 CI local completado exitosamente!"
echo "📦 Artefacto generado: $JAR_FILE"
echo ""
echo "Próximos pasos:"
echo "  - Revisar cobertura de tests en target/site/jacoco/"
echo "  - Ejecutar análisis de calidad de código"
echo "  - Preparar para deploy"