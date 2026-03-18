# Documento de Requisitos: Migración de Base de Datos

## Introducción

Este documento define los requisitos para migrar la configuración de base de datos del proyecto Atleta-Server desde H2 en memoria a PostgreSQL con Flyway para migraciones, configuraciones optimizadas por ambiente, y estrategias de testing robustas.

## Glosario

- **Database_System**: El sistema de gestión de base de datos PostgreSQL
- **Migration_Engine**: Flyway como herramienta de migración de esquemas
- **Connection_Pool**: HikariCP como pool de conexiones optimizado
- **Environment_Config**: Configuraciones específicas por ambiente (dev, test, staging, prod)
- **Test_Container**: Testcontainers para testing aislado con PostgreSQL
- **Schema_Migration**: Scripts SQL versionados para evolución del esquema
- **SSL_Connection**: Conexiones seguras con certificados SSL/TLS

## Requisitos

### Requisito 1: Configuración de PostgreSQL por Ambientes

**Historia de Usuario:** Como desarrollador, quiero configuraciones específicas de PostgreSQL para cada ambiente, para optimizar el rendimiento y seguridad según el contexto de uso.

#### Criterios de Aceptación

1. CUANDO se configure el ambiente de desarrollo, EL Sistema DEBERÁ usar PostgreSQL local con logging detallado habilitado
2. CUANDO se configure el ambiente de testing, EL Sistema DEBERÁ usar Testcontainers con PostgreSQL para aislamiento completo
3. CUANDO se configure el ambiente de staging, EL Sistema DEBERÁ usar PostgreSQL remoto con validaciones estrictas
4. CUANDO se configure el ambiente de producción, EL Sistema DEBERÁ usar PostgreSQL con SSL habilitado y configuraciones optimizadas
5. EL Sistema DEBERÁ validar que todas las configuraciones usen variables de entorno para credenciales

### Requisito 2: Implementación de Flyway para Migraciones

**Historia de Usuario:** Como desarrollador, quiero usar Flyway para gestionar migraciones de esquema, para mantener consistencia entre ambientes y facilitar despliegues.

#### Criterios de Aceptación

1. CUANDO se inicialice la aplicación, EL Sistema DEBERÁ ejecutar automáticamente las migraciones pendientes
2. EL Sistema DEBERÁ mantener un historial de migraciones en la tabla flyway_schema_history
3. CUANDO se cree una migración, EL Sistema DEBERÁ seguir la convención de nomenclatura V{VERSION}__{DESCRIPTION}.sql
4. EL Sistema DEBERÁ validar todas las migraciones antes de aplicarlas
5. CUANDO se ejecute en producción, EL Sistema DEBERÁ deshabilitar la funcionalidad clean de Flyway

### Requisito 3: Optimización de HikariCP

**Historia de Usuario:** Como administrador del sistema, quiero configuraciones optimizadas de pool de conexiones, para maximizar el rendimiento y minimizar el uso de recursos.

#### Criterios de Aceptación

1. CUANDO se configure el pool para desarrollo, EL Sistema DEBERÁ usar un máximo de 10 conexiones
2. CUANDO se configure el pool para testing, EL Sistema DEBERÁ usar un máximo de 5 conexiones
3. CUANDO se configure el pool para staging, EL Sistema DEBERÁ usar un máximo de 20 conexiones
4. CUANDO se configure el pool para producción, EL Sistema DEBERÁ usar un máximo de 50 conexiones
5. EL Sistema DEBERÁ configurar detección de leaks y timeouts apropiados para cada ambiente

### Requisito 4: Creación de Migraciones Iniciales

**Historia de Usuario:** Como desarrollador, quiero migraciones que creen el esquema completo de la aplicación Atleta, para replicar la estructura existente en PostgreSQL.

#### Criterios de Aceptación

1. CUANDO se ejecute la migración inicial, EL Sistema DEBERÁ crear todas las tablas del dominio Atleta
2. EL Sistema DEBERÁ crear índices optimizados para las consultas más frecuentes
3. CUANDO se creen las tablas, EL Sistema DEBERÁ incluir todas las restricciones de integridad referencial
4. EL Sistema DEBERÁ insertar datos maestros iniciales (posiciones, configuraciones)
5. CUANDO se complete la migración, EL Sistema DEBERÁ validar que el esquema sea idéntico al diseño

### Requisito 5: Configuración de Testing con Testcontainers

**Historia de Usuario:** Como desarrollador, quiero tests aislados con PostgreSQL real, para garantizar que los tests reflejen el comportamiento de producción.

#### Criterios de Aceptación

1. CUANDO se ejecuten los tests, EL Sistema DEBERÁ inicializar automáticamente un contenedor PostgreSQL
2. EL Sistema DEBERÁ aplicar todas las migraciones en el contenedor de test
3. CUANDO termine cada test, EL Sistema DEBERÁ limpiar los datos pero mantener el esquema
4. EL Sistema DEBERÁ permitir cargar datos de prueba específicos para cada test
5. CUANDO se complete la suite de tests, EL Sistema DEBERÁ destruir automáticamente el contenedor

### Requisito 6: Configuración de Seguridad de Base de Datos

**Historia de Usuario:** Como administrador de seguridad, quiero configuraciones seguras de base de datos, para proteger los datos sensibles y cumplir con estándares de seguridad.

#### Criterios de Aceptación

1. CUANDO se conecte a producción, EL Sistema DEBERÁ usar conexiones SSL/TLS obligatorias
2. EL Sistema DEBERÁ usar usuarios específicos con permisos mínimos necesarios por ambiente
3. CUANDO se almacenen credenciales, EL Sistema DEBERÁ usar variables de entorno exclusivamente
4. EL Sistema DEBERÁ configurar timeouts de conexión apropiados para prevenir ataques
5. CUANDO se detecten conexiones sospechosas, EL Sistema DEBERÁ registrar eventos de seguridad

### Requisito 7: Monitoreo y Métricas de Base de Datos

**Historia de Usuario:** Como administrador del sistema, quiero métricas detalladas de la base de datos, para monitorear rendimiento y detectar problemas proactivamente.

#### Criterios de Aceptación

1. CUANDO se inicialice la aplicación, EL Sistema DEBERÁ exponer métricas de HikariCP via Actuator
2. EL Sistema DEBERÁ registrar métricas de conexiones activas, idle y totales
3. CUANDO ocurran errores de conexión, EL Sistema DEBERÁ incrementar contadores específicos
4. EL Sistema DEBERÁ medir tiempos de respuesta de queries críticas
5. CUANDO se detecten leaks de conexión, EL Sistema DEBERÁ generar alertas automáticas

### Requisito 8: Scripts de Backup y Recuperación

**Historia de Usuario:** Como administrador de base de datos, quiero scripts automatizados de backup y recuperación, para garantizar la continuidad del negocio.

#### Criterios de Aceptación

1. CUANDO se ejecute un backup, EL Sistema DEBERÁ crear respaldos completos, de esquema y de datos por separado
2. EL Sistema DEBERÁ comprimir y fechar automáticamente los archivos de backup
3. CUANDO se requiera restauración, EL Sistema DEBERÁ proporcionar scripts para diferentes tipos de restore
4. EL Sistema DEBERÁ validar la integridad de los backups después de crearlos
5. CUANDO se programen backups, EL Sistema DEBERÁ mantener rotación automática de archivos antiguos

### Requisito 9: Configuración de Logging de Base de Datos

**Historia de Usuario:** Como desarrollador, quiero logging configurado apropiadamente para cada ambiente, para facilitar debugging sin impactar rendimiento.

#### Criterios de Aceptación

1. CUANDO se ejecute en desarrollo, EL Sistema DEBERÁ mostrar todas las queries SQL con formato
2. CUANDO se ejecute en testing, EL Sistema DEBERÁ mostrar solo errores y warnings de base de datos
3. CUANDO se ejecute en staging, EL Sistema DEBERÁ registrar métricas de performance sin queries
4. CUANDO se ejecute en producción, EL Sistema DEBERÁ registrar solo errores críticos de base de datos
5. EL Sistema DEBERÁ incluir información de contexto (usuario, transacción) en todos los logs

### Requisito 10: Validación de Configuraciones

**Historia de Usuario:** Como desarrollador, quiero validación automática de configuraciones, para detectar problemas de configuración antes del despliegue.

#### Criterios de Aceptación

1. CUANDO se inicie la aplicación, EL Sistema DEBERÁ validar que todas las variables de entorno requeridas estén presentes
2. EL Sistema DEBERÁ verificar conectividad con la base de datos antes de completar el startup
3. CUANDO se detecten configuraciones inválidas, EL Sistema DEBERÁ fallar rápidamente con mensajes claros
4. EL Sistema DEBERÁ validar que las migraciones sean consistentes con el estado actual
5. CUANDO se ejecuten health checks, EL Sistema DEBERÁ verificar el estado de la conexión de base de datos