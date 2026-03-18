# Plan de Implementación: Sistema de Calificación de Jugadores

## Resumen

Este plan implementa el Sistema de Calificación de Jugadores mediante una serie de tareas incrementales que construyen desde las estructuras de datos básicas hasta la funcionalidad completa con testing integral.

## Tareas

- [x] 1. Crear enumeraciones y constantes del sistema
  - Crear enum RoleType con pesos de goles y asistencias
  - Crear enum PriorityLevel con calificaciones base y multiplicadores
  - Crear enum MatchResultType con puntos normales y de arquero rotativo
  - _Requerimientos: 1.1, 1.4, 1.5, 2.1, 2.3, 3.1, 3.2, 3.3, 7.2_

- [ ]* 1.1 Escribir prueba de propiedad para validación de roles y prioridades
  - **Propiedad 1: Validación de Roles y Prioridades**
  - **Valida: Requerimientos 1.1, 2.1**

- [ ]* 1.2 Escribir prueba de ejemplo para valores constantes del sistema
  - **Propiedad 4: Valores Constantes del Sistema**
  - **Valida: Requerimientos 1.4, 1.5, 2.3, 3.1, 3.2, 3.3, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 6.1, 7.2**

- [x] 2. Crear entidades de dominio
  - [x] 2.1 Crear entidad PlayerRating
    - Implementar entidad JPA con relaciones a PlayerProfile
    - Incluir campos para rol, prioridad, calificación actual y metadatos
    - _Requerimientos: 9.1_

  - [x] 2.2 Crear entidad RatingHistory
    - Implementar entidad JPA para auditoría de cambios
    - Incluir todos los campos de rendimiento y cálculo
    - _Requerimientos: 9.2, 9.3_

  - [ ]* 2.3 Escribir prueba de propiedad para persistencia y relaciones
    - **Propiedad 12: Persistencia y Relaciones de Datos**
    - **Valida: Requerimientos 9.1, 9.2, 9.3**

- [x] 3. Crear repositorios de datos
  - Crear PlayerRatingRepository con métodos de consulta personalizados
  - Crear RatingHistoryRepository con consultas de historial
  - Implementar métodos para búsqueda por jugador, rol y período
  - _Requerimientos: 9.1, 9.2_

- [ ] 4. Implementar motor de cálculo de calificaciones
  - [x] 4.1 Crear RatingCalculationEngine
    - Implementar método calculateNewRating con algoritmo principal
    - Implementar método calculateRotativeGoalkeeperRating
    - Implementar métodos auxiliares para bonos defensivos y aplicación de límites
    - _Requerimientos: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6_

  - [ ]* 4.2 Escribir prueba de propiedad para aplicación de pesos por rol
    - **Propiedad 2: Aplicación Correcta de Pesos por Rol**
    - **Valida: Requerimientos 1.2, 4.1, 4.2**

  - [ ]* 4.3 Escribir prueba de propiedad para bonos defensivos
    - **Propiedad 3: Bonos Defensivos Correctos**
    - **Valida: Requerimientos 1.3, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7**

  - [ ]* 4.4 Escribir prueba de propiedad para límites mínimos
    - **Propiedad 5: Aplicación de Límites Mínimos**
    - **Valida: Requerimientos 2.2, 2.5, 8.4**

  - [ ]* 4.5 Escribir prueba de propiedad para algoritmo principal
    - **Propiedad 11: Algoritmo Principal de Cálculo**
    - **Valida: Requerimientos 8.1, 8.2, 8.3, 8.5, 8.6, 2.4, 3.4, 4.5, 5.8, 6.2**

- [ ] 5. Checkpoint - Verificar que el motor de cálculo funciona correctamente
  - Asegurar que todas las pruebas pasen, preguntar al usuario si surgen dudas.

- [x] 6. Crear DTOs y clases de transferencia
  - Crear PlayerPerformanceDto para entrada de datos
  - Crear RatingCalculationRequest para el motor de cálculo
  - Crear RotativeGoalkeeperRequest para modo especial
  - Implementar validaciones de entrada en DTOs
  - _Requerimientos: 4.4, 6.3, 9.4_

- [ ]* 6.1 Escribir prueba de propiedad para validación de entrada
  - **Propiedad 7: Validación de Entrada**
  - **Valida: Requerimientos 4.4, 6.3**

- [x] 7. Implementar servicio principal RatingService
  - [x] 7.1 Crear método updatePlayerRatings
    - Implementar lógica de orquestación para actualización de calificaciones
    - Incluir validación de datos de entrada y manejo de errores
    - _Requerimientos: 9.4, 9.5_

  - [x] 7.2 Crear método updateRotativeGoalkeeperRatings
    - Implementar lógica especial para modo arquero rotativo
    - Asegurar procesamiento independiente del sistema regular
    - _Requerimientos: 7.1, 7.3, 7.4, 7.5_

  - [x] 7.3 Crear métodos de consulta
    - Implementar getPlayerRatings y getRatingHistory
    - Incluir filtros por rol y período de tiempo
    - _Requerimientos: 9.1, 9.2_

  - [ ]* 7.4 Escribir prueba de propiedad para puntos de resultado universales
    - **Propiedad 6: Aplicación Universal de Puntos de Resultado**
    - **Valida: Requerimientos 3.5**

  - [ ]* 7.5 Escribir prueba de propiedad para restricción MVP único
    - **Propiedad 8: Restricción de MVP Único**
    - **Valida: Requerimientos 6.4**

  - [ ]* 7.6 Escribir prueba de propiedad para bono MVP universal
    - **Propiedad 9: Aplicación Universal de Bono MVP**
    - **Valida: Requerimientos 6.5**

  - [ ]* 7.7 Escribir prueba de propiedad para arquero rotativo
    - **Propiedad 10: Comportamiento de Arquero Rotativo**
    - **Valida: Requerimientos 7.1, 7.3, 7.4, 7.5**

- [x] 8. Implementar manejo de errores y excepciones
  - Crear excepciones personalizadas (RatingCalculationException, etc.)
  - Implementar validación robusta en todos los puntos de entrada
  - Agregar logging apropiado para auditoría y debugging
  - _Requerimientos: 9.4_

- [ ]* 8.1 Escribir prueba de propiedad para validación de datos requeridos
  - **Propiedad 13: Validación de Datos Requeridos**
  - **Valida: Requerimientos 9.4**

- [x] 9. Crear controlador REST (opcional)
  - [x] 9.1 Crear RatingController con endpoints básicos
    - Endpoint para actualizar calificaciones manualmente
    - Endpoint para consultar calificaciones de jugador
    - Endpoint para obtener historial de calificaciones
    - Incluir documentación OpenAPI

  - [ ]* 9.2 Escribir pruebas de integración para controlador
    - Probar endpoints con datos válidos e inválidos
    - Verificar respuestas HTTP correctas

- [x] 10. Crear migraciones de base de datos
  - Crear script de migración para tabla player_ratings
  - Crear script de migración para tabla rating_history
  - Incluir índices apropiados para consultas de rendimiento
  - _Requerimientos: 9.1, 9.2_

- [ ] 11. Integración con sistema existente
  - [x] 11.1 Integrar con MatchService para actualizaciones automáticas
    - Modificar flujo de finalización de partidos
    - Agregar llamadas al RatingService cuando un partido termine
    - _Requerimientos: 9.3_

  - [x] 11.2 Agregar campos de calificación a PlayerProfile (si es necesario)
    - Evaluar si se necesitan campos adicionales en PlayerProfile
    - Implementar relaciones bidireccionales si es apropiado

- [ ]* 11.3 Escribir pruebas de integración end-to-end
  - Probar flujo completo desde creación de partido hasta actualización de calificaciones
  - Verificar que todas las relaciones de datos funcionen correctamente

- [ ] 12. Checkpoint final - Verificar integración completa
  - Asegurar que todas las pruebas pasen, preguntar al usuario si surgen dudas.
  - Verificar que el sistema se integre correctamente con la aplicación existente

## Notas

- Las tareas marcadas con `*` son opcionales y pueden omitirse para un MVP más rápido
- Cada tarea referencia requerimientos específicos para trazabilidad
- Los checkpoints aseguran validación incremental
- Las pruebas de propiedades validan propiedades de corrección universales
- Las pruebas unitarias validan ejemplos específicos y casos límite
- El enfoque incremental permite detectar problemas temprano en el desarrollo