#!/bin/bash

# =============================================================================
# Database Restore Script for Atleta Application
# =============================================================================
# This script restores PostgreSQL database backups with safety validations:
# - Supports different backup types (full, schema, data)
# - Environment-specific safety checks (only dev and staging allowed)
# - Backup integrity validation before restore
# - Optional database recreation
# - Progress reporting and clear error messages
# =============================================================================

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${SCRIPT_DIR}/../backups"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Help function
show_help() {
    cat << EOF
Database Restore Script for Atleta Application

Usage: $0 [OPTIONS] BACKUP_FILE

ARGUMENTS:
    BACKUP_FILE             Path to the backup file to restore

OPTIONS:
    -e, --environment ENV   Target environment (dev|staging) [default: dev]
    -f, --force            Skip confirmation prompts
    -r, --recreate         Drop and recreate database before restore
    -c, --clean            Clean existing data before restore (for data-only backups)
    -h, --help             Show this help message

EXAMPLES:
    $0 atleta_dev_full_20240115_143022.sql.gz
    $0 --environment staging --recreate backup.sql.gz
    $0 -e dev -f -c atleta_dev_data_20240115_143022.sql.gz

SAFETY NOTES:
    - Production restores are NOT ALLOWED for safety reasons
    - Always verify backup integrity before restore
    - Use --recreate with caution as it will drop the entire database
    - Schema-only restores will not affect existing data
    - Data-only restores can be combined with --clean to replace all data

ENVIRONMENT VARIABLES:
    Development (dev):
        DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
    
    Staging (staging):
        STAGING_DB_HOST, STAGING_DB_PORT, STAGING_DB_NAME, 
        STAGING_DB_USER, STAGING_DB_PASSWORD

EOF
}

# Parse command line arguments
ENVIRONMENT="dev"
FORCE=false
RECREATE=false
CLEAN=false
BACKUP_FILE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -f|--force)
            FORCE=true
            shift
            ;;
        -r|--recreate)
            RECREATE=true
            shift
            ;;
        -c|--clean)
            CLEAN=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        -*)
            error "Unknown option: $1"
            show_help
            exit 1
            ;;
        *)
            if [[ -z "$BACKUP_FILE" ]]; then
                BACKUP_FILE="$1"
            else
                error "Multiple backup files specified. Please provide only one."
                exit 1
            fi
            shift
            ;;
    esac
done

# Validate required arguments
if [[ -z "$BACKUP_FILE" ]]; then
    error "Backup file is required"
    show_help
    exit 1
fi

# Validate environment (production not allowed)
if [[ ! "$ENVIRONMENT" =~ ^(dev|staging)$ ]]; then
    error "Invalid environment: $ENVIRONMENT"
    error "Only 'dev' and 'staging' environments are allowed for restore operations"
    error "Production restores must be performed manually with proper authorization"
    exit 1
fi

# Load environment-specific database configuration
load_db_config() {
    case "$ENVIRONMENT" in
        dev)
            DB_HOST="${DB_HOST:-localhost}"
            DB_PORT="${DB_PORT:-5432}"
            DB_NAME="${DB_NAME:-atleta_dev}"
            DB_USER="${DB_USER:-atleta_user}"
            DB_PASSWORD="${DB_PASSWORD:-atleta_pass}"
            POSTGRES_DB="${POSTGRES_DB:-postgres}"
            POSTGRES_USER="${POSTGRES_USER:-postgres}"
            POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-postgres}"
            ;;
        staging)
            DB_HOST="${STAGING_DB_HOST:-}"
            DB_PORT="${STAGING_DB_PORT:-5432}"
            DB_NAME="${STAGING_DB_NAME:-atleta_staging}"
            DB_USER="${STAGING_DB_USER:-}"
            DB_PASSWORD="${STAGING_DB_PASSWORD:-}"
            POSTGRES_DB="${STAGING_POSTGRES_DB:-postgres}"
            POSTGRES_USER="${STAGING_POSTGRES_USER:-postgres}"
            POSTGRES_PASSWORD="${STAGING_POSTGRES_PASSWORD:-}"
            ;;
    esac

    # Validate required variables
    if [[ -z "$DB_HOST" || -z "$DB_USER" || -z "$DB_PASSWORD" ]]; then
        error "Missing required database configuration for environment: $ENVIRONMENT"
        error "Please set the appropriate environment variables."
        exit 1
    fi
}

# Check dependencies
check_dependencies() {
    local missing_deps=()
    
    if ! command -v psql &> /dev/null; then
        missing_deps+=("psql")
    fi
    
    if ! command -v pg_restore &> /dev/null; then
        missing_deps+=("pg_restore")
    fi
    
    if ! command -v gzip &> /dev/null; then
        missing_deps+=("gzip")
    fi
    
    if [[ ${#missing_deps[@]} -gt 0 ]]; then
        error "Missing required dependencies: ${missing_deps[*]}"
        error "Please install PostgreSQL client tools"
        exit 1
    fi
}

# Validate backup file
validate_backup_file() {
    local backup_path="$1"
    
    # Check if file exists
    if [[ ! -f "$backup_path" ]]; then
        error "Backup file not found: $backup_path"
        exit 1
    fi
    
    # Check if file is readable
    if [[ ! -r "$backup_path" ]]; then
        error "Backup file is not readable: $backup_path"
        exit 1
    fi
    
    # Check if file is not empty
    if [[ ! -s "$backup_path" ]]; then
        error "Backup file is empty: $backup_path"
        exit 1
    fi
    
    # Validate gzip integrity if it's a compressed file
    if [[ "$backup_path" == *.gz ]]; then
        log "Validating backup file integrity..."
        if ! gzip -t "$backup_path" 2>/dev/null; then
            error "Backup file is corrupted (gzip test failed): $backup_path"
            exit 1
        fi
        success "Backup file integrity validated"
    fi
    
    # Validate checksum if available
    local checksum_file="${backup_path}.sha256"
    if [[ -f "$checksum_file" ]]; then
        log "Validating backup checksum..."
        if command -v sha256sum &> /dev/null; then
            if sha256sum -c "$checksum_file" &>/dev/null; then
                success "Backup checksum validated"
            else
                error "Backup checksum validation failed"
                exit 1
            fi
        elif command -v shasum &> /dev/null; then
            if shasum -a 256 -c "$checksum_file" &>/dev/null; then
                success "Backup checksum validated"
            else
                error "Backup checksum validation failed"
                exit 1
            fi
        fi
    else
        warning "No checksum file found for validation"
    fi
}

# Detect backup type from filename or content
detect_backup_type() {
    local backup_path="$1"
    local filename=$(basename "$backup_path")
    
    if [[ "$filename" == *"_schema_"* ]]; then
        echo "schema"
    elif [[ "$filename" == *"_data_"* ]]; then
        echo "data"
    else
        echo "full"
    fi
}

# Test database connectivity
test_connectivity() {
    log "Testing database connectivity..."
    
    export PGPASSWORD="$DB_PASSWORD"
    
    if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" &>/dev/null; then
        success "Database connection successful"
    else
        error "Cannot connect to database: $DB_HOST:$DB_PORT/$DB_NAME"
        error "Please check your database configuration and ensure the database is running"
        exit 1
    fi
    
    unset PGPASSWORD
}

# Confirm restore operation
confirm_restore() {
    local backup_path="$1"
    local backup_type="$2"
    
    if [[ "$FORCE" == true ]]; then
        return 0
    fi
    
    echo
    warning "=== RESTORE CONFIRMATION ==="
    echo "Environment: $ENVIRONMENT"
    echo "Database: $DB_HOST:$DB_PORT/$DB_NAME"
    echo "Backup file: $backup_path"
    echo "Backup type: $backup_type"
    echo "Recreate database: $RECREATE"
    echo "Clean existing data: $CLEAN"
    echo
    
    if [[ "$RECREATE" == true ]]; then
        warning "WARNING: This will DROP and RECREATE the entire database!"
        warning "ALL EXISTING DATA WILL BE LOST!"
    elif [[ "$CLEAN" == true && "$backup_type" == "data" ]]; then
        warning "WARNING: This will DELETE all existing data before restore!"
    fi
    
    echo
    read -p "Are you sure you want to proceed? (yes/no): " -r
    echo
    
    if [[ ! "$REPLY" =~ ^[Yy][Ee][Ss]$ ]]; then
        log "Restore operation cancelled by user"
        exit 0
    fi
}

# Drop and recreate database
recreate_database() {
    log "Recreating database: $DB_NAME"
    
    export PGPASSWORD="$POSTGRES_PASSWORD"
    
    # Connect to postgres database to drop and create
    log "Dropping existing database..."
    if ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
         -c "DROP DATABASE IF EXISTS \"$DB_NAME\";" 2>/dev/null; then
        error "Failed to drop database: $DB_NAME"
        exit 1
    fi
    
    log "Creating new database..."
    if ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
         -c "CREATE DATABASE \"$DB_NAME\" OWNER \"$DB_USER\";" 2>/dev/null; then
        error "Failed to create database: $DB_NAME"
        exit 1
    fi
    
    unset PGPASSWORD
    success "Database recreated successfully"
}

# Clean existing data (for data-only restores)
clean_existing_data() {
    log "Cleaning existing data from database..."
    
    export PGPASSWORD="$DB_PASSWORD"
    
    # Get list of tables to truncate (excluding flyway schema history)
    local tables
    tables=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
        "SELECT string_agg(schemaname||'.'||tablename, ', ') 
         FROM pg_tables 
         WHERE schemaname = 'public' 
         AND tablename != 'flyway_schema_history';" 2>/dev/null | tr -d ' ')
    
    if [[ -n "$tables" && "$tables" != "" ]]; then
        log "Truncating tables: $tables"
        if ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
             -c "TRUNCATE TABLE $tables RESTART IDENTITY CASCADE;" 2>/dev/null; then
            warning "Some tables could not be truncated (this may be normal)"
        fi
    else
        log "No tables found to clean"
    fi
    
    unset PGPASSWORD
    success "Data cleaning completed"
}

# Perform restore operation
perform_restore() {
    local backup_path="$1"
    local backup_type="$2"
    
    log "Starting restore operation..."
    log "Backup type: $backup_type"
    
    export PGPASSWORD="$DB_PASSWORD"
    
    # Determine restore command based on file type and backup type
    local restore_cmd=""
    
    if [[ "$backup_path" == *.gz ]]; then
        # Compressed file - need to decompress first
        if [[ "$backup_type" == "full" ]]; then
            # Custom format backup
            restore_cmd="gunzip -c '$backup_path' | pg_restore -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME --verbose --no-owner --no-privileges"
        else
            # SQL format backup
            restore_cmd="gunzip -c '$backup_path' | psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME"
        fi
    else
        # Uncompressed file
        if [[ "$backup_type" == "full" ]]; then
            restore_cmd="pg_restore -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME --verbose --no-owner --no-privileges '$backup_path'"
        else
            restore_cmd="psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f '$backup_path'"
        fi
    fi
    
    log "Executing restore command..."
    if eval "$restore_cmd"; then
        success "Restore completed successfully"
    else
        error "Restore operation failed"
        exit 1
    fi
    
    unset PGPASSWORD
}

# Verify restore success
verify_restore() {
    log "Verifying restore success..."
    
    export PGPASSWORD="$DB_PASSWORD"
    
    # Check if we can connect and query basic information
    local table_count
    table_count=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null | tr -d ' ')
    
    if [[ -n "$table_count" && "$table_count" -gt 0 ]]; then
        success "Restore verification successful - found $table_count tables"
    else
        warning "Restore verification: no tables found (this may be normal for schema-only restores)"
    fi
    
    # Check Flyway schema history if it exists
    if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c \
       "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;" &>/dev/null; then
        local latest_version
        latest_version=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
            "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;" 2>/dev/null | tr -d ' ')
        log "Latest Flyway migration version: $latest_version"
    fi
    
    unset PGPASSWORD
}

# Main execution
main() {
    log "=== Atleta Database Restore Script ==="
    log "Environment: $ENVIRONMENT"
    log "Backup file: $BACKUP_FILE"
    
    # Resolve backup file path
    local backup_path="$BACKUP_FILE"
    if [[ ! "$backup_path" =~ ^/ ]]; then
        # Relative path - check in backup directory first
        if [[ -f "${BACKUP_DIR}/${backup_path}" ]]; then
            backup_path="${BACKUP_DIR}/${backup_path}"
        elif [[ ! -f "$backup_path" ]]; then
            backup_path="${BACKUP_DIR}/${backup_path}"
        fi
    fi
    
    check_dependencies
    load_db_config
    validate_backup_file "$backup_path"
    
    local backup_type
    backup_type=$(detect_backup_type "$backup_path")
    log "Detected backup type: $backup_type"
    
    test_connectivity
    confirm_restore "$backup_path" "$backup_type"
    
    # Perform pre-restore operations
    if [[ "$RECREATE" == true ]]; then
        recreate_database
    elif [[ "$CLEAN" == true && "$backup_type" == "data" ]]; then
        clean_existing_data
    fi
    
    perform_restore "$backup_path" "$backup_type"
    verify_restore
    
    success "=== Restore process completed successfully ==="
    log "Database: $DB_HOST:$DB_PORT/$DB_NAME"
}

# Execute main function
main "$@"