# Scripts de base de datos

## Proposito
Scripts operativos para backup y restore de la base PostgreSQL de Atleta.

## Scripts disponibles
- `backup-database.sh`: genera backups comprimidos con validaciones, checksum y rotacion.
- `restore-database.sh`: restaura backups con restricciones de seguridad para evitar restauraciones accidentales en produccion.

## Requisitos
- Cliente PostgreSQL (`psql`, `pg_dump`, `pg_restore`).
- `gzip`.
- `sha256sum` o `shasum` para validacion de integridad.

## Uso basico
```bash
chmod +x scripts/backup-database.sh
chmod +x scripts/restore-database.sh

./scripts/backup-database.sh --environment dev --type full
./scripts/restore-database.sh --environment dev backup.sql.gz
```

## Variables de entorno principales
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=atleta_dev
export DB_USER=atleta_user
export DB_PASSWORD=atleta_pass
```

## Seguridad
- Las restauraciones en produccion estan deshabilitadas por diseno.
- Validar siempre el ambiente antes de restaurar.
- No commitear credenciales ni backups reales.
- Probar restores periodicamente en ambientes de desarrollo o staging.

## Estado
Herramientas de apoyo para operacion y CI/CD. No reemplazan una estrategia productiva de backups administrados, monitoreo y retencion fuera del servidor.
