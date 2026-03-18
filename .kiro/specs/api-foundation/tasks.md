# Plan de Implementación: API Atletas y Fútbol

## Visión General

Este plan convierte el diseño de la API de Atletas y Fútbol en una serie de tareas de codificación incrementales. Cada tarea construye sobre las anteriores implementando las 13 entidades del dominio deportivo con sus relaciones, validaciones y lógica de negocio específica.

## Tareas

- [x] 1. Configurar dependencias y estructura base del proyecto
  - Actualizar pom.xml con dependencias necesarias (JPA, Validation, OpenAPI, jqwik, UUID)
  - Crear estructura de paquetes según el diseño
  - Configurar perfiles de aplicación (dev, test, prod)
  - _Requisitos: Configuración base_

- [x] 2. Implementar enumeraciones y entidad base
  - [x] 2.1 Crear todas las enumeraciones del dominio
    - Implementar MatchMode, MatchStatus, PlayerRole, EventType, MatchResult
    - _Requisitos: 6.1, 6.5, 5.1, 8.1_

  - [x] 2.2 Crear BaseEntity con campos comunes
    - Implementar clase abstracta con id, timestamps, version
    - _Requisitos: Estructura base_

  - [x] 2.3 Escribir test de propiedad para enumeraciones
    - **Propiedad 6: Validación de partidos**
    - **Valida: Requisitos 6.1, 6.2, 6.3**

- [x] 3. Implementar entidades principales (Athlete, Position)
  - [x] 3.1 Crear entidad Athlete con UUID como PK
    - Implementar con atletaUuid, email único, passwordHash, nombre
    - Incluir validaciones de email y campos requeridos
    - _Requisitos: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 3.2 Crear entidad Position
    - Implementar catálogo fijo de posiciones de fútbol
    - _Requisitos: 3.1_

  - [x] 3.3 Escribir test de propiedad para unicidad de atletas
    - **Propiedad 1: Unicidad de atletas**
    - **Valida: Requisitos 1.1, 1.2**

- [x] 4. Implementar PlayerProfile y relaciones
  - [x] 4.1 Crear entidad PlayerProfile
    - Implementar relación uno-a-uno con Athlete
    - Incluir alias, trustScore con valor por defecto 100
    - _Requisitos: 2.1, 2.2, 2.3, 2.4_

  - [x] 4.2 Crear entidad PlayerPosition
    - Implementar relación many-to-many entre PlayerProfile y Position
    - Incluir prioridad (1,2,3) y XP
    - _Requisitos: 3.2, 3.3, 3.4, 3.5_

  - [x] 4.3 Escribir test de propiedad para integridad de perfiles
    - **Propiedad 2: Integridad de perfiles de jugador**
    - **Valida: Requisitos 2.1, 2.2**

  - [x] 4.4 Escribir test de propiedad para validación de posiciones
    - **Propiedad 3: Validación de posiciones y prioridades**
    - **Valida: Requisitos 3.2, 3.4**

- [x] 5. Checkpoint - Verificar entidades básicas
  - Asegurar que todos los tests pasen, preguntar al usuario si surgen dudas.

- [x] 6. Implementar entidades de equipos
  - [x] 6.1 Crear entidad Team
    - Implementar con nombre único, logoUrl, añoFundacion, creador
    - _Requisitos: 4.1, 4.2, 4.4_

  - [x] 6.2 Crear entidad TeamStats
    - Implementar estadísticas del equipo con valores por defecto en cero
    - Relación uno-a-uno con Team
    - _Requisitos: 4.3_

  - [x] 6.3 Crear entidad TeamMember
    - Implementar relación N—M entre PlayerProfile y Team
    - Incluir roles, estado activo, fecha de ingreso
    - _Requisitos: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 6.4 Escribir test de propiedad para integridad de equipos
    - **Propiedad 4: Integridad de equipos**
    - **Valida: Requisitos 4.1, 4.3, 4.4**

  - [x] 6.5 Escribir test de propiedad para consistencia de membresía
    - **Propiedad 5: Consistencia de membresía**
    - **Valida: Requisitos 5.1, 5.2**

- [-] 7. Implementar entidades de partidos
  - [x] 7.1 Crear entidad Match
    - Implementar con modalidad, fecha/hora, coordenadas, cuota, estado
    - Incluir validaciones de coordenadas y estado inicial
    - _Requisitos: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [x] 7.2 Crear entidad MatchTeam
    - Implementar relación Match 1—2 Teams (exactamente 2 equipos por partido)
    - Incluir validación de equipos local/visitante
    - _Requisitos: Estructura de partidos_

  - [x] 7.3 Crear entidad MatchPlayer
    - Implementar participación de jugadores en partidos con confirmación
    - _Requisitos: 7.1, 7.2, 7.3, 7.4, 7.5_

  - [x] 7.4 Escribir test de propiedad para validación de partidos
    - **Propiedad 6: Validación de partidos**
    - **Valida: Requisitos 6.1, 6.2, 6.3**

  - [x] 7.5 Escribir test de propiedad para integridad de participación
    - **Propiedad 7: Integridad de participación**
    - **Valida: Requisitos 7.1, 7.2, 7.4**

- [x] 8. Implementar eventos y historial
  - [x] 8.1 Crear entidad MatchEvent
    - Implementar eventos de partido (goles, asistencias) con confirmaciones
    - _Requisitos: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [x] 8.2 Crear entidad PlayerHistory como fuente de verdad
    - Implementar historial inmutable de participaciones (anotación @Immutable)
    - Establecer como única fuente de verdad para estadísticas históricas
    - Incluir validaciones de inmutabilidad
    - _Requisitos: 9.1, 9.2, 9.3, 9.4, 9.5_

  - [x] 8.3 Crear entidad TrustLog
    - Implementar registro de cambios de confianza
    - _Requisitos: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [x] 8.4 Escribir test de propiedad para validación de eventos
    - **Propiedad 8: Validación de eventos**
    - **Valida: Requisitos 8.1, 8.2, 8.4**

  - [x] 8.5 Escribir test de propiedad para inmutabilidad del historial como fuente de verdad
    - **Propiedad 9: Inmutabilidad del historial como fuente de verdad**
    - **Valida: Requisitos 9.1, 9.4**

  - [x] 8.6 Escribir test de propiedad para trazabilidad de confianza
    - **Propiedad 10: Trazabilidad de confianza**
    - **Valida: Requisitos 10.1, 10.3, 10.4**

  - [x] 8.7 Escribir test de propiedad para restricción de equipos por partido
    - **Propiedad 11: Restricción de equipos por partido**
    - **Valida: Requisitos 6.1, 6.2**

- [x] 9. Checkpoint - Verificar todas las entidades
  - Asegurar que todos los tests pasen, preguntar al usuario si surgen dudas.

- [-] 10. Implementar repositorios
  - [x] 10.1 Crear repositorios para todas las entidades
    - AthleteRepository, PlayerProfileRepository, TeamRepository, MatchRepository
    - Incluir consultas personalizadas necesarias
    - _Requisitos: Acceso a datos_

  - [x] 10.2 Escribir tests unitarios para repositorios
    - Probar operaciones CRUD y consultas personalizadas
    - _Requisitos: Integridad de datos_

- [x] 11. Implementar DTOs de request y response
  - [x] 11.1 Crear DTOs de request con validaciones
    - CreateAthleteRequest, CreateTeamRequest, CreateMatchRequest, etc.
    - Incluir todas las validaciones de negocio
    - _Requisitos: Validación de datos_

  - [x] 11.2 Crear DTOs de response
    - AthleteResponse, TeamResponse, MatchResponse, etc.
    - _Requisitos: Estructura de respuestas_

- [x] 12. Implementar servicios de negocio
  - [x] 12.1 Crear AthleteService
    - Implementar lógica de registro, autenticación, gestión de atletas
    - _Requisitos: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 12.2 Crear PlayerProfileService
    - Implementar gestión de perfiles, posiciones, trust score
    - _Requisitos: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [x] 12.3 Crear TeamService
    - Implementar gestión de equipos, membresías, estadísticas
    - _Requisitos: 4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 12.4 Crear MatchService
    - Implementar gestión de partidos, participación, eventos
    - _Requisitos: 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5_

  - [x] 12.5 Crear TrustScoreService
    - Implementar lógica de cálculo y actualización de confianza
    - _Requisitos: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [x] 12.6 Escribir tests unitarios para servicios
    - Probar lógica de negocio y validaciones
    - _Requisitos: Lógica de negocio_

- [-] 13. Implementar controladores REST
  - [x] 13.1 Crear AthleteController
    - Endpoints para registro, login, gestión de atletas
    - _Requisitos: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 13.2 Crear PlayerProfileController
    - Endpoints para gestión de perfiles y posiciones
    - _Requisitos: 2.1, 2.2, 2.3, 2.4, 2.5, 3.2, 3.3, 3.4, 3.5_

  - [x] 13.3 Crear TeamController
    - Endpoints para gestión de equipos y membresías
    - _Requisitos: 4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 13.4 Crear MatchController
    - Endpoints para gestión de partidos, participación y eventos
    - _Requisitos: 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5, 8.1, 8.2, 8.3, 8.4, 8.5_

  - [x] 13.5 Escribir tests de integración para controladores
    - Probar endpoints completos con MockMvc
    - _Requisitos: Integración completa_

- [x] 14. Configurar seguridad y documentación
  - [x] 14.1 Configurar SecurityConfig específico para atletas
    - Endpoints públicos (registro) y protegidos (gestión)
    - _Requisitos: Seguridad_

  - [x] 14.2 Configurar OpenAPI con documentación específica
    - Documentar todos los endpoints con ejemplos del dominio
    - _Requisitos: Documentación_

  - [x] 14.3 Crear PositionController y servicio
    - Crear PositionController con endpoints REST
    - Crear PositionService para lógica de negocio
    - Crear PositionResponse DTO
    - Documentar endpoints con OpenAPI

- [ ] 15. Validación y testing final

### 15.1 Verificar funcionamiento completo
- [ ] Probar todos los endpoints con Swagger UI
- [ ] Verificar seguridad en endpoints protegidos
- [ ] Validar documentación OpenAPI
- [ ] Ejecutar suite completa de tests

### 15.2 Optimizaciones finales
- [ ] Revisar logs y configuración
- [ ] Optimizar consultas de base de datos
- [ ] Verificar manejo de errores

- [ ] 16. Integración final y configuración
  - [ ] 15.1 Configurar application.yaml para todos los perfiles
    - Configuraciones específicas para el dominio de atletas
    - _Requisitos: Configuración_

  - [ ] 15.2 Implementar inicialización de datos
    - Crear datos iniciales (posiciones, equipos de ejemplo)
    - _Requisitos: Datos iniciales_

  - [ ] 15.3 Escribir tests de integración end-to-end
    - Probar flujos completos del dominio deportivo
    - _Requisitos: Integración completa_

- [ ] 16. Checkpoint final - Verificar implementación completa
  - Asegurar que todos los tests pasen, preguntar al usuario si surgen dudas.

## Notas

- Las tareas incluyen testing comprehensivo desde el inicio
- Cada tarea referencia requisitos específicos para trazabilidad
- Los checkpoints aseguran validación incremental
- Los tests de propiedades validan propiedades universales de corrección
- Los tests unitarios validan ejemplos específicos y casos límite
- La implementación sigue el dominio específico de atletas y fútbol definido en el diseño