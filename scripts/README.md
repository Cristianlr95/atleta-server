# Database Backup and Restore Scripts

This directory contains scripts for backing up and restoring the Atleta PostgreSQL database.

## Scripts Overview

### backup-database.sh
Creates compressed backups of the PostgreSQL database with integrity validation and automatic cleanup.

**Features:**
- Full, schema-only, or data-only backups
- Automatic gzip compression
- SHA256 checksum generation for integrity validation
- Automatic rotation of old backup files
- Support for multiple environments (dev, staging, prod)

### restore-database.sh
Restores database backups with safety validations and environment restrictions.

**Features:**
- Supports all backup types (full, schema, data)
- Safety restrictions (no production restores)
- Backup integrity validation before restore
- Optional database recreation
- Progress reporting and clear error messages

## Prerequisites

### Required Software
- PostgreSQL client tools (`psql`, `pg_dump`, `pg_restore`)
- `gzip` for compression/decompression
- `sha256sum` or `shasum` for checksum validation (optional)

### Script Permissions
Make sure the scripts are executable:
```bash
chmod +x scripts/backup-database.sh
chmod +x scripts/restore-database.sh
```

### Environment Variables

Set the following environment variables for each environment:

#### Development (dev)
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=atleta_dev
export DB_USER=atleta_user
export DB_PASSWORD=atleta_pass
export POSTGRES_DB=postgres
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=postgres
```

#### Staging (staging)
```bash
export STAGING_DB_HOST=staging-db.example.com
export STAGING_DB_PORT=5432
export STAGING_DB_NAME=atleta_staging
export STAGING_DB_USER=atleta_staging_user
export STAGING_DB_PASSWORD=secure_staging_password
export STAGING_POSTGRES_DB=postgres
export STAGING_POSTGRES_USER=postgres
export STAGING_POSTGRES_PASSWORD=postgres_admin_password
```

#### Production (prod)
```bash
export PROD_DB_HOST=prod-db.example.com
export PROD_DB_PORT=5432
export PROD_DB_NAME=atleta_prod
export PROD_DB_USER=atleta_prod_user
export PROD_DB_PASSWORD=very_secure_prod_password
```

## Usage Examples

### Backup Operations

#### Create a full backup for development
```bash
./backup-database.sh --environment dev --type full
```

#### Create a schema-only backup for staging
```bash
./backup-database.sh --environment staging --type schema
```

#### Create a data-only backup with custom retention
```bash
./backup-database.sh --environment dev --type data --retention 90
```

#### Create a backup to a custom directory
```bash
./backup-database.sh --environment dev --output /custom/backup/path
```

### Restore Operations

#### Restore a full backup to development
```bash
./restore-database.sh --environment dev atleta_dev_full_20240115_143022.sql.gz
```

#### Restore with database recreation (destructive)
```bash
./restore-database.sh --environment dev --recreate --force backup.sql.gz
```

#### Restore data-only backup with existing data cleanup
```bash
./restore-database.sh --environment staging --clean atleta_staging_data_20240115_143022.sql.gz
```

## Safety Features

### Backup Script Safety
- Validates database connectivity before backup
- Creates checksums for integrity validation
- Automatically rotates old backups to prevent disk space issues
- Provides detailed logging and error messages

### Restore Script Safety
- **Production restores are completely disabled** for safety
- Only allows restores to `dev` and `staging` environments
- Validates backup file integrity before restore
- Requires confirmation for destructive operations
- Tests database connectivity before proceeding

## File Naming Convention

Backup files follow this naming pattern:
```
{database_name}_{environment}_{type}_{timestamp}.sql.gz
```

Examples:
- `atleta_dev_full_20240115_143022.sql.gz` - Full backup
- `atleta_staging_schema_20240115_143022.sql.gz` - Schema-only backup
- `atleta_dev_data_20240115_143022.sql.gz` - Data-only backup

## Backup Types

### Full Backup (`--type full`)
- Contains both schema and data
- Uses PostgreSQL custom format for efficiency
- Recommended for complete database restoration

### Schema-only Backup (`--type schema`)
- Contains only database structure (tables, indexes, constraints)
- Useful for setting up new environments
- Does not affect existing data when restored

### Data-only Backup (`--type data`)
- Contains only table data
- Useful for data migration or testing with production data
- Can be combined with `--clean` flag to replace existing data

## Troubleshooting

### Common Issues

#### "pg_dump not found"
Install PostgreSQL client tools:
```bash
# Ubuntu/Debian
sudo apt-get install postgresql-client

# macOS with Homebrew
brew install postgresql

# Windows
# Download and install PostgreSQL from postgresql.org
```

#### "Connection refused"
- Verify database is running
- Check host, port, and credentials
- Ensure firewall allows connections
- Verify network connectivity

#### "Permission denied"
- Check database user permissions
- Ensure user has backup/restore privileges
- Verify file system permissions for backup directory

#### "Backup file corrupted"
- Check available disk space during backup
- Verify network stability for remote backups
- Use checksum files to validate integrity

### Getting Help

Use the `--help` flag with either script for detailed usage information:
```bash
./backup-database.sh --help
./restore-database.sh --help
```

## Best Practices

### Backup Strategy
1. **Regular Backups**: Schedule daily full backups for production
2. **Multiple Locations**: Store backups in multiple locations
3. **Test Restores**: Regularly test backup restoration procedures
4. **Monitor Space**: Monitor backup directory disk usage
5. **Retention Policy**: Set appropriate retention periods for each environment

### Security Considerations
1. **Environment Variables**: Use environment variables for credentials
2. **File Permissions**: Restrict access to backup files
3. **Network Security**: Use SSL connections for remote databases
4. **Audit Trail**: Log all backup and restore operations

### Performance Tips
1. **Off-peak Hours**: Schedule backups during low-usage periods
2. **Compression**: Always use compression for large databases
3. **Parallel Operations**: Consider parallel dumps for very large databases
4. **Network Bandwidth**: Consider network impact for remote backups

## Integration with CI/CD

These scripts can be integrated into CI/CD pipelines for automated backup and restore operations:

```yaml
# Example GitHub Actions workflow
- name: Backup Database
  run: |
    ./scripts/backup-database.sh --environment staging --type full
    
- name: Restore Test Data
  run: |
    ./scripts/restore-database.sh --environment dev --force test-data.sql.gz
```

## Monitoring and Alerting

Consider implementing monitoring for:
- Backup success/failure notifications
- Backup file size trends
- Disk space usage in backup directory
- Backup duration monitoring
- Failed restore attempt alerts

## Script Configuration

### Default Settings
- **Backup Directory**: `../backups` (relative to script location)
- **Retention Period**: 30 days
- **Default Environment**: `dev`
- **Default Backup Type**: `full`
- **Compression**: Always enabled (gzip)

### Customization
Both scripts can be customized by modifying the configuration variables at the top of each file:

```bash
# In backup-database.sh
BACKUP_DIR="${SCRIPT_DIR}/../backups"
RETENTION_DAYS=30

# In restore-database.sh  
BACKUP_DIR="${SCRIPT_DIR}/../backups"
```

## Database Schema Compatibility

These scripts are designed to work with the Atleta application's PostgreSQL database schema, which includes:
- Flyway migration management
- Standard application tables
- Audit logging tables
- User management and roles

The scripts preserve:
- Database schema structure
- Indexes and constraints
- Flyway migration history
- Data integrity and relationships

## Backup Directory Structure

The backup directory will be organized as follows:
```
backups/
├── atleta_dev_full_20240115_143022.sql.gz
├── atleta_dev_full_20240115_143022.sql.gz.sha256
├── atleta_staging_schema_20240116_090000.sql.gz
├── atleta_staging_schema_20240116_090000.sql.gz.sha256
└── ...
```

Each backup includes:
- **`.sql.gz`** - Compressed backup file
- **`.sql.gz.sha256`** - Checksum file for integrity validation (when available)