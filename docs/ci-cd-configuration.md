# Configuración de CI/CD para Migración PostgreSQL

## Introducción

Este documento proporciona configuraciones de ejemplo para integrar la migración de base de datos PostgreSQL en pipelines de CI/CD. Incluye configuraciones para GitHub Actions, GitLab CI, y Jenkins.

## Variables de Entorno Requeridas

### Variables Comunes por Ambiente

```bash
# Testing (manejado por Testcontainers automáticamente)
# No requiere configuración adicional

# Staging
STAGING_DB_HOST=staging-db.atleta.com
STAGING_DB_PORT=5432
STAGING_DB_NAME=atleta_staging
STAGING_DB_USERNAME=atleta_staging
STAGING_DB_PASSWORD=<password_seguro>

# Producción
PROD_DB_HOST=<prod-db-host>
PROD_DB_PORT=5432
PROD_DB_NAME=<prod-db-name>
PROD_DB_USERNAME=<prod-username>
PROD_DB_PASSWORD=<password_seguro>
```

## GitHub Actions

### Archivo: `.github/workflows/ci.yml`

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  JAVA_VERSION: '21'
  MAVEN_OPTS: '-Xmx1024m'

jobs:
  test:
    name: Tests y Validación
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: atleta_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

    steps:
    - name: Checkout código
      uses: actions/checkout@v4

    - name: Configurar JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Cache dependencias Maven
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        restore-keys: ${{ runner.os }}-m2

    - name: Ejecutar tests unitarios
      run: mvn test -Dspring.profiles.active=test

    - name: Ejecutar tests de integración
      run: mvn test -Dtest="*IntegrationTest" -Dspring.profiles.active=test

    - name: Ejecutar tests de migración
      run: mvn test -Dtest="*MigrationTest,FlywayIntegrationTest" -Dspring.profiles.active=test

    - name: Validar migraciones Flyway
      run: mvn flyway:validate -Dspring.profiles.active=test

    - name: Generar reporte de cobertura
      run: mvn jacoco:report

    - name: Subir reporte de cobertura
      uses: codecov/codecov-action@v3
      with:
        file: ./target/site/jacoco/jacoco.xml

  build:
    name: Build y Empaquetado
    runs-on: ubuntu-latest
    needs: test
    
    steps:
    - name: Checkout código
      uses: actions/checkout@v4

    - name: Configurar JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Cache dependencias Maven
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        restore-keys: ${{ runner.os }}-m2

    - name: Compilar aplicación
      run: mvn clean compile

    - name: Empaquetar aplicación
      run: mvn package -DskipTests

    - name: Subir artefacto
      uses: actions/upload-artifact@v3
      with:
        name: atleta-server-jar
        path: target/*.jar

  deploy-staging:
    name: Deploy a Staging
    runs-on: ubuntu-latest
    needs: build
    if: github.ref == 'refs/heads/develop'
    environment: staging
    
    steps:
    - name: Checkout código
      uses: actions/checkout@v4

    - name: Descargar artefacto
      uses: actions/download-artifact@v3
      with:
        name: atleta-server-jar
        path: target/

    - name: Configurar JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Validar migraciones en staging
      run: mvn flyway:info flyway:validate -Dspring.profiles.active=staging
      env:
        DB_HOST: ${{ secrets.STAGING_DB_HOST }}
        DB_USERNAME: ${{ secrets.STAGING_DB_USERNAME }}
        DB_PASSWORD: ${{ secrets.STAGING_DB_PASSWORD }}
        DB_NAME: ${{ secrets.STAGING_DB_NAME }}

    - name: Ejecutar migraciones en staging
      run: mvn flyway:migrate -Dspring.profiles.active=staging
      env:
        DB_HOST: ${{ secrets.STAGING_DB_HOST }}
        DB_USERNAME: ${{ secrets.STAGING_DB_USERNAME }}
        DB_PASSWORD: ${{ secrets.STAGING_DB_PASSWORD }}
        DB_NAME: ${{ secrets.STAGING_DB_NAME }}

    - name: Backup antes del deploy
      run: ./scripts/backup-database.sh staging
      env:
        STAGING_DB_HOST: ${{ secrets.STAGING_DB_HOST }}
        STAGING_DB_USERNAME: ${{ secrets.STAGING_DB_USERNAME }}
        STAGING_DB_PASSWORD: ${{ secrets.STAGING_DB_PASSWORD }}
        STAGING_DB_NAME: ${{ secrets.STAGING_DB_NAME }}

    # Aquí iría el deploy real a staging
    - name: Deploy a staging
      run: echo "Deploy to staging server"

  deploy-production:
    name: Deploy a Producción
    runs-on: ubuntu-latest
    needs: build
    if: github.ref == 'refs/heads/main'
    environment: production
    
    steps:
    - name: Checkout código
      uses: actions/checkout@v4

    - name: Descargar artefacto
      uses: actions/download-artifact@v3
      with:
        name: atleta-server-jar
        path: target/

    - name: Configurar JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Validar migraciones en producción
      run: mvn flyway:info flyway:validate -Dspring.profiles.active=prod
      env:
        DB_HOST: ${{ secrets.PROD_DB_HOST }}
        DB_USERNAME: ${{ secrets.PROD_DB_USERNAME }}
        DB_PASSWORD: ${{ secrets.PROD_DB_PASSWORD }}
        DB_NAME: ${{ secrets.PROD_DB_NAME }}

    - name: Backup completo antes del deploy
      run: ./scripts/backup-database.sh prod
      env:
        PROD_DB_HOST: ${{ secrets.PROD_DB_HOST }}
        PROD_DB_USERNAME: ${{ secrets.PROD_DB_USERNAME }}
        PROD_DB_PASSWORD: ${{ secrets.PROD_DB_PASSWORD }}
        PROD_DB_NAME: ${{ secrets.PROD_DB_NAME }}

    - name: Ejecutar migraciones en producción
      run: mvn flyway:migrate -Dspring.profiles.active=prod
      env:
        DB_HOST: ${{ secrets.PROD_DB_HOST }}
        DB_USERNAME: ${{ secrets.PROD_DB_USERNAME }}
        DB_PASSWORD: ${{ secrets.PROD_DB_PASSWORD }}
        DB_NAME: ${{ secrets.PROD_DB_NAME }}

    # Aquí iría el deploy real a producción
    - name: Deploy a producción
      run: echo "Deploy to production server"

    - name: Verificar health check post-deploy
      run: |
        sleep 30
        curl -f http://your-prod-server/actuator/health || exit 1
```

## GitLab CI

### Archivo: `.gitlab-ci.yml`

```yaml
stages:
  - test
  - build
  - deploy-staging
  - deploy-production

variables:
  JAVA_VERSION: "21"
  MAVEN_OPTS: "-Xmx1024m"
  POSTGRES_DB: "atleta_test"
  POSTGRES_USER: "test"
  POSTGRES_PASSWORD: "test"

# Template para jobs con PostgreSQL
.postgres-template: &postgres-template
  services:
    - name: postgres:16
      alias: postgres
  variables:
    POSTGRES_HOST_AUTH_METHOD: trust

test:
  <<: *postgres-template
  stage: test
  image: openjdk:21-jdk
  before_script:
    - apt-get update -qq && apt-get install -y -qq postgresql-client
    - ./mvnw --version
  script:
    - ./mvnw test -Dspring.profiles.active=test
    - ./mvnw test -Dtest="*IntegrationTest" -Dspring.profiles.active=test
    - ./mvnw test -Dtest="*MigrationTest,FlywayIntegrationTest" -Dspring.profiles.active=test
    - ./mvnw flyway:validate -Dspring.profiles.active=test
  artifacts:
    reports:
      junit:
        - target/surefire-reports/TEST-*.xml
    paths:
      - target/site/jacoco/
  coverage: '/Total.*?([0-9]{1,3})%/'

build:
  stage: build
  image: openjdk:21-jdk
  script:
    - ./mvnw clean package -DskipTests
  artifacts:
    paths:
      - target/*.jar
    expire_in: 1 hour

deploy-staging:
  stage: deploy-staging
  image: openjdk:21-jdk
  before_script:
    - apt-get update -qq && apt-get install -y -qq postgresql-client
  script:
    # Validar migraciones
    - ./mvnw flyway:info flyway:validate -Dspring.profiles.active=staging
    # Backup antes del deploy
    - ./scripts/backup-database.sh staging
    # Ejecutar migraciones
    - ./mvnw flyway:migrate -Dspring.profiles.active=staging
    # Deploy (placeholder)
    - echo "Deploying to staging..."
  environment:
    name: staging
    url: https://staging.atleta.com
  only:
    - develop

deploy-production:
  stage: deploy-production
  image: openjdk:21-jdk
  before_script:
    - apt-get update -qq && apt-get install -y -qq postgresql-client
  script:
    # Validar migraciones
    - ./mvnw flyway:info flyway:validate -Dspring.profiles.active=prod
    # Backup completo
    - ./scripts/backup-database.sh prod
    # Ejecutar migraciones
    - ./mvnw flyway:migrate -Dspring.profiles.active=prod
    # Deploy (placeholder)
    - echo "Deploying to production..."
    # Health check
    - sleep 30
    - curl -f https://api.atleta.com/actuator/health
  environment:
    name: production
    url: https://api.atleta.com
  when: manual
  only:
    - main
```

## Jenkins

### Archivo: `Jenkinsfile`

```groovy
pipeline {
    agent any
    
    environment {
        JAVA_VERSION = '21'
        MAVEN_OPTS = '-Xmx1024m'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Test') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        sh './mvnw test -Dspring.profiles.active=test'
                    }
                    post {
                        always {
                            junit 'target/surefire-reports/*.xml'
                        }
                    }
                }
                
                stage('Integration Tests') {
                    steps {
                        sh './mvnw test -Dtest="*IntegrationTest" -Dspring.profiles.active=test'
                    }
                }
                
                stage('Migration Tests') {
                    steps {
                        sh './mvnw test -Dtest="*MigrationTest,FlywayIntegrationTest" -Dspring.profiles.active=test'
                        sh './mvnw flyway:validate -Dspring.profiles.active=test'
                    }
                }
            }
        }
        
        stage('Build') {
            steps {
                sh './mvnw clean package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
        
        stage('Deploy to Staging') {
            when {
                branch 'develop'
            }
            environment {
                DB_HOST = credentials('staging-db-host')
                DB_USERNAME = credentials('staging-db-username')
                DB_PASSWORD = credentials('staging-db-password')
                DB_NAME = credentials('staging-db-name')
            }
            steps {
                script {
                    // Validar migraciones
                    sh './mvnw flyway:info flyway:validate -Dspring.profiles.active=staging'
                    
                    // Backup antes del deploy
                    sh '''
                        export STAGING_DB_HOST=${DB_HOST}
                        export STAGING_DB_USERNAME=${DB_USERNAME}
                        export STAGING_DB_PASSWORD=${DB_PASSWORD}
                        export STAGING_DB_NAME=${DB_NAME}
                        ./scripts/backup-database.sh staging
                    '''
                    
                    // Ejecutar migraciones
                    sh './mvnw flyway:migrate -Dspring.profiles.active=staging'
                    
                    // Deploy (placeholder)
                    echo 'Deploying to staging...'
                }
            }
        }
        
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            environment {
                DB_HOST = credentials('prod-db-host')
                DB_USERNAME = credentials('prod-db-username')
                DB_PASSWORD = credentials('prod-db-password')
                DB_NAME = credentials('prod-db-name')
            }
            steps {
                script {
                    // Solicitar aprobación manual
                    input message: 'Deploy to production?', ok: 'Deploy'
                    
                    // Validar migraciones
                    sh './mvnw flyway:info flyway:validate -Dspring.profiles.active=prod'
                    
                    // Backup completo
                    sh '''
                        export PROD_DB_HOST=${DB_HOST}
                        export PROD_DB_USERNAME=${DB_USERNAME}
                        export PROD_DB_PASSWORD=${DB_PASSWORD}
                        export PROD_DB_NAME=${DB_NAME}
                        ./scripts/backup-database.sh prod
                    '''
                    
                    // Ejecutar migraciones
                    sh './mvnw flyway:migrate -Dspring.profiles.active=prod'
                    
                    // Deploy (placeholder)
                    echo 'Deploying to production...'
                    
                    // Health check
                    sh '''
                        sleep 30
                        curl -f https://api.atleta.com/actuator/health
                    '''
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
            // Aquí se podrían agregar notificaciones
        }
    }
}
```

## Docker Compose para CI/CD Local

### Archivo: `docker-compose.ci.yml`

```yaml
version: '3.8'

services:
  postgres-ci:
    image: postgres:16
    environment:
      POSTGRES_DB: atleta_test
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    ports:
      - "5432:5432"
    volumes:
      - postgres_ci_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U test -d atleta_test"]
      interval: 10s
      timeout: 5s
      retries: 5

  atleta-app:
    build: .
    depends_on:
      postgres-ci:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: test
      DB_HOST: postgres-ci
      DB_USERNAME: test
      DB_PASSWORD: test
      DB_NAME: atleta_test
    ports:
      - "8080:8080"

volumes:
  postgres_ci_data:
```

## Scripts de Automatización

### Script de CI Local: `scripts/ci-local.sh`

```bash
#!/bin/bash
# Script para ejecutar CI localmente

set -e

echo "🚀 Iniciando CI local..."

# Limpiar contenedores anteriores
docker-compose -f docker-compose.ci.yml down -v

# Iniciar PostgreSQL
echo "📦 Iniciando PostgreSQL..."
docker-compose -f docker-compose.ci.yml up -d postgres-ci

# Esperar a que PostgreSQL esté listo
echo "⏳ Esperando PostgreSQL..."
timeout 60 bash -c 'until docker-compose -f docker-compose.ci.yml exec postgres-ci pg_isready -U test -d atleta_test; do sleep 2; done'

# Ejecutar tests
echo "🧪 Ejecutando tests..."
./mvnw test -Dspring.profiles.active=test

echo "🧪 Ejecutando tests de integración..."
./mvnw test -Dtest="*IntegrationTest" -Dspring.profiles.active=test

echo "🧪 Ejecutando tests de migración..."
./mvnw test -Dtest="*MigrationTest,FlywayIntegrationTest" -Dspring.profiles.active=test

# Validar migraciones
echo "✅ Validando migraciones..."
./mvnw flyway:validate -Dspring.profiles.active=test

# Build
echo "🔨 Construyendo aplicación..."
./mvnw clean package -DskipTests

# Limpiar
docker-compose -f docker-compose.ci.yml down -v

echo "✅ CI local completado exitosamente!"
```

## Configuración de Secrets

### GitHub Actions Secrets

```bash
# Staging
STAGING_DB_HOST
STAGING_DB_USERNAME
STAGING_DB_PASSWORD
STAGING_DB_NAME

# Production
PROD_DB_HOST
PROD_DB_USERNAME
PROD_DB_PASSWORD
PROD_DB_NAME
```

### GitLab CI Variables

```bash
# Staging (Protected, Masked)
STAGING_DB_HOST
STAGING_DB_USERNAME
STAGING_DB_PASSWORD
STAGING_DB_NAME

# Production (Protected, Masked)
PROD_DB_HOST
PROD_DB_USERNAME
PROD_DB_PASSWORD
PROD_DB_NAME
```

### Jenkins Credentials

```bash
# Tipo: Secret text
staging-db-host
staging-db-username
staging-db-password
staging-db-name

prod-db-host
prod-db-username
prod-db-password
prod-db-name
```

## Mejores Prácticas

### 1. Validación de Migraciones
- Siempre validar migraciones antes de ejecutarlas
- Usar `flyway:info` para verificar estado
- Ejecutar `flyway:validate` en cada build

### 2. Backups Automáticos
- Backup antes de cada deploy a staging/producción
- Mantener rotación de backups
- Verificar integridad de backups

### 3. Testing Robusto
- Tests unitarios con cobertura mínima
- Tests de integración con Testcontainers
- Tests específicos de migración

### 4. Rollback Strategy
- Mantener scripts de rollback manual
- Backups automáticos antes de cambios
- Plan de recuperación documentado

### 5. Monitoreo Post-Deploy
- Health checks automáticos
- Verificación de métricas
- Alertas en caso de fallos

## Troubleshooting CI/CD

### Error: "Testcontainers could not find Docker"
```bash
# Asegurar que Docker esté disponible en el runner
- name: Setup Docker
  uses: docker/setup-buildx-action@v2
```

### Error: "Migration checksum mismatch"
```bash
# Reparar checksums en pipeline
./mvnw flyway:repair -Dspring.profiles.active=test
```

### Error: "Connection timeout"
```bash
# Aumentar timeout en configuración
spring:
  datasource:
    hikari:
      connection-timeout: 60000
```

### Error: "Out of memory"
```bash
# Ajustar memoria en pipeline
export MAVEN_OPTS="-Xmx2048m"
```

## Recursos Adicionales

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [GitLab CI/CD Documentation](https://docs.gitlab.com/ee/ci/)
- [Jenkins Pipeline Documentation](https://www.jenkins.io/doc/book/pipeline/)
- [Flyway CI/CD Best Practices](https://flywaydb.org/documentation/concepts/migrations#best-practices)
- [Testcontainers CI Documentation](https://www.testcontainers.org/supported_docker_environments/continuous_integration/)
