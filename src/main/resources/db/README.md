# Base de datos de Atleta Server

## Proposito
Esta carpeta contiene recursos de base de datos para PostgreSQL: migraciones Flyway y datos de prueba.

## Estructura
```text
db/
  migration/   # migraciones principales
  test-data/   # datos especificos para testing
```

## Migraciones
Las migraciones siguen la convencion:

```text
V{numero}__descripcion_en_snake_case.sql
```

Ejemplos:
- `V001__create_initial_schema.sql`
- `V002__add_basic_indexes.sql`
- `V003__create_player_ratings_table.sql`
- `V019__add_athlete_gender.sql`

## Ejecucion local
La aplicacion ejecuta Flyway automaticamente al iniciar. Para revisar o validar manualmente:

```bash
./mvnw flyway:info
./mvnw flyway:validate
```

En Windows:

```powershell
.\mvnw.cmd flyway:info
.\mvnw.cmd flyway:validate
```

## Reglas de mantenimiento
- No editar migraciones ya aplicadas en ambientes compartidos.
- Crear una nueva migracion para cada cambio de esquema.
- Mantener `ddl-auto=validate` para evitar divergencias entre entidades y base.
- Usar `test-data/` solo para datos de prueba.

## Estado
El modelo actual cubre atletas, perfiles, posiciones, equipos, partidos, eventos, ratings, historial, social, canchas y datos complementarios. Las migraciones son una pieza clave para evaluar la madurez tecnica del backend.
