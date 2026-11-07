# Feedback System para Futuras Tareas en Atleta Backend

## Protocolo obligatorio antes de cambiar codigo

1. Leer `docs/memory.md`
2. Leer `docs/funcionalidades.md`
3. Leer `docs/architecture.md`
4. Leer `docs/deployment.md` si la tarea toca seguridad, secretos, CI/CD, infraestructura, observabilidad o base de datos

## Reglas de trabajo

- Validar primero si la logica ya existe en controladores, servicios o repositorios.
- Extender antes que duplicar.
- No asumir que README o docs viejas siguen vigentes; la fuente real es el codigo.
- No marcar funcionalidades como terminadas si solo existen migraciones, DTOs o servicios sin exposición real.
- No romper contratos REST actuales sin registrar impacto.

## Checklist tecnico obligatorio

### Al iniciar

- Revisar el modulo afectado y sus dependencias directas.
- Confirmar si hay tests del area en `src/test/java`.
- Confirmar si la tarea toca seguridad por identidad real o solo por autenticacion.

### Antes de implementar

- Buscar endpoints similares.
- Buscar queries o servicios con logica equivalente.
- Revisar migraciones existentes para no reinventar esquema.
- Revisar DTOs y responses ya usados por frontend.

### Durante la implementacion

- Respetar el modelo `Athlete -> PlayerProfile -> Team/Match`.
- Mantener `PlayerHistory` como fuente historica.
- Evitar meter mas responsabilidad en `MatchService` o `RatingService` si se puede extraer.
- Si se agrega endpoint sensible, amarrar actor del request al JWT.

### Al terminar

- Actualizar `docs/memory.md` si cambian decisiones o riesgos.
- Actualizar `docs/funcionalidades.md` si cambia cobertura funcional.
- Actualizar `docs/architecture.md` si cambia estructura, patrones o servicios.
- Actualizar `docs/deployment.md` si cambian perfiles, secretos, pipeline, monitoreo o infraestructura.
- Registrar el cambio relevante en el documento vivo correspondiente.

## Reglas de seguridad obligatorias

- No dejar escritura publica en rutas tipo `permitAll`.
- No confiar en `actorUuid` del body/query sin validar contra JWT.
- No introducir secretos en YAML versionado.
- No exponer actuator completo fuera de entornos controlados.

## Regla de calidad documental

Cada tarea futura debe dejar el repositorio en un estado mas entendible que antes. Si se detecta deuda no abordada, documentarla; si se corrige, reflejarlo.
