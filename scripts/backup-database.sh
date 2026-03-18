#!/bin/bash

# =============================================================================
# Database Backup Script for Atleta Application
# =============================================================================
# This script creates backups of the PostgreSQL database with different options:
# - Full backup (schema + data)
# - Schema-only backup
# - Data-only backup
# 
# Features:
# - Automatic compression with gzip
# - Integrity validation using checksums
# - Automatic rotation of old backup files
# - Support for different environments (dev, staging, prod)
# =============================================================================

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${SCRIPT_DIR}/../backups"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
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
Database Backup Script for Atleta Application

Usage: $0 [OPTIONS]

OPTIONS:
    -e, --environment ENV    Environment (dev|staging|prod) [default: dev]
    -t, --type TYPE         Backup type (full|schema|data) [default: full]
    -o, --output DIR        Output directory [default: ../backups]
    -r, --retention DAYS    Retention period in days [default: 30]
    -h, --help              Show this help message

EXAMPLES:
    $0 --environment dev --type full
    $0 -e staging -t schema -o /custom/backup/path
    $0 --environment prod --type data --retention 90

ENVIRONMENT VARIABLES:
    For each environment, set the following variables:
    
    Development (dev):
        DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
    
    Staging (staging):
        STAGING_DB_HOST, STAGING_DB_PORT, STAGING_DB_NAME, 
        STAGING_DB_USER, STAGING_DB_PASSWORD
    
    Production (prod):
        PROD_DB_HOST, PROD_DB_PORT, PROD_DB_NAME,
        PROD_DB_USER, PROD_DB_PASSWORD

EOF
}

# Parse command line arguments
ENVIRONMENT="dev"
BACKUP_TYPE="full"
CUSTOM_OUTPUT=""
CUSTOM_RETENTION=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -t|--type)
            BACKUP_TYPE="$2"
            shift 2
            ;;
        -o|--output)
            CUSTOM_OUTPUT="$2"
            shift 2
            ;;
        -r|--retention)
            CUSTOM_RETENTION="$2"
            shift 2
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Validate environment
if [[ ! "$ENVIRONMENT" =~ ^(dev|staging|prod)$ ]]; then
    error "Invalid environment: $ENVIRONMENT. Must be dev, staging, or prod."
    exit 1
fi

# Validate backup type
if [[ ! "$BACKUP_TYPE" =~ ^(full|schema|data)$ ]]; then
    error "Invalid backup type: $BACKUP_TYPE. Must be full, schema, or data."
    exit 1
fi

# Set custom values if provided
if [[ -n "$CUSTOM_OUTPUT" ]]; then
    BACKUP_DIR="$CUSTOM_OUTPUT"
fi

if [[ -n "$CUSTOM_RETENTION" ]]; then
    RETENTION_DAYS="$CUSTOM_RETENTION"
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
            ;;
        staging)
            DB_HOST="${STAGING_DB_HOST:-}"
            DB_PORT="${STAGING_DB_PORT:-5432}"
            DB_NAME="${STAGING_DB_NAME:-atleta_staging}"
            DB_USER="${STAGING_DB_USER:-}"
            DB_PASSWORD="${STAGING_DB_PASSWORD:-}"
            ;;
        prod)
            DB_HOST="${PROD_DB_HOST:-}"
            DB_PORT="${PROD_DB_PORT:-5432}"
            DB_NAME="${PROD_DB_NAME:-atleta_prod}"
            DB_USER="${PROD_DB_USER:-}"
            DB_PASSWORD="${PROD_DB_PASSWORD:-}"
            ;;
    esac

    # Validate required variables
    if [[ -z "$DB_HOST" || -z "$DB_USER" || -z "$DB_PASSWORD" ]]; then
        error "Missing required database configuration for environment: $ENVIRONMENT"
        error "Please set the appropriate environment variables."
        exit 1
    fi
}

# Check if pg_dump is available
check_dependencies() {
    if ! command -v pg_dump &> /dev/null; then
        error "pg_dump is not installed or not in PATH"
        error "Please install PostgreSQL client tools"
        exit 1
    fi

    if ! command -v gzip &> /dev/null; then
        error "gzip is not installed or not in PATH"
        exit 1
    fi
}

# Create backup directory
create_backup_dir() {
    if [[ ! -d "$BACKUP_DIR" ]]; then
        log "Creating backup directory: $BACKUP_DIR"
        mkdir -p "$BACKUP_DIR"
    fi
}

# Generate backup filename
generate_filename() {
    local suffix=""
    case "$BACKUP_TYPE" in
        schema) suffix="_schema" ;;
        data) suffix="_data" ;;
    esac
    
    echo "${DB_NAME}_${ENVIRONMENT}${suffix}_${DATE}.sql.gz"
}

# Perform backup
perform_backup() {
    local filename="$1"
    local filepath="${BACKUP_DIR}/${filename}"
    
    log "Starting $BACKUP_TYPE backup for $ENVIRONMENT environment"
    log "Database: $DB_HOST:$DB_PORT/$DB_NAME"
    log "Output file: $filepath"
    
    # Set PGPASSWORD for authentication
    export PGPASSWORD="$DB_PASSWORD"
    
    # Build pg_dump command based on backup type
    local pg_dump_cmd="pg_dump -h $DB_HOST -p $DB_PORT -U $DB_USER"
    
    case "$BACKUP_TYPE" in
        full)
            pg_dump_cmd="$pg_dump_cmd --verbose --format=custom --no-owner --no-privileges"
            ;;
        schema)
            pg_dump_cmd="$pg_dump_cmd --verbose --schema-only --no-owner --no-privileges"
            ;;
        data)
            pg_dump_cmd="$pg_dump_cmd --verbose --data-only --no-owner --no-privileges"
            ;;
    esac
    
    pg_dump_cmd="$pg_dump_cmd $DB_NAME"
    
    # Execute backup with compression
    if eval "$pg_dump_cmd" | gzip > "$filepath"; then
        success "Backup completed successfully: $filepath"
    else
        error "Backup failed"
        rm -f "$filepath" 2>/dev/null || true
        exit 1
    fi
    
    # Clear password from environment
    unset PGPASSWORD
}

# Validate backup integrity
validate_backup() {
    local filepath="$1"
    
    log "Validating backup integrity..."
    
    # Check if file exists and is not empty
    if [[ ! -f "$filepath" || ! -s "$filepath" ]]; then
        error "Backup file is missing or empty: $filepath"
        return 1
    fi
    
    # Test gzip integrity
    if ! gzip -t "$filepath" 2>/dev/null; then
        error "Backup file is corrupted (gzip test failed): $filepath"
        return 1
    fi
    
    # Generate and save checksum
    local checksum_file="${filepath}.sha256"
    if command -v sha256sum &> /dev/null; then
        sha256sum "$filepath" > "$checksum_file"
        success "Checksum saved: $checksum_file"
    elif command -v shasum &> /dev/null; then
        shasum -a 256 "$filepath" > "$checksum_file"
        success "Checksum saved: $checksum_file"
    else
        warning "No checksum utility available (sha256sum or shasum)"
    fi
    
    # Get file size
    local file_size
    if command -v stat &> /dev/null; then
        file_size=$(stat -f%z "$filepath" 2>/dev/null || stat -c%s "$filepath" 2>/dev/null)
        log "Backup size: $(numfmt --to=iec "$file_size" 2>/dev/null || echo "$file_size bytes")"
    fi
    
    success "Backup validation completed"
}

# Clean old backups
cleanup_old_backups() {
    log "Cleaning up backups older than $RETENTION_DAYS days..."
    
    local deleted_count=0
    
    # Find and delete old backup files
    while IFS= read -r -d '' file; do
        rm -f "$file"
        rm -f "${file}.sha256" 2>/dev/null || true
        ((deleted_count++))
        log "Deleted old backup: $(basename "$file")"
    done < <(find "$BACKUP_DIR" -name "*.sql.gz" -type f -mtime +$RETENTION_DAYS -print0 2>/dev/null)
    
    if [[ $deleted_count -eq 0 ]]; then
        log "No old backups to clean up"
    else
        success "Cleaned up $deleted_count old backup(s)"
    fi
}

# Main execution
main() {
    log "=== Atleta Database Backup Script ==="
    log "Environment: $ENVIRONMENT"
    log "Backup type: $BACKUP_TYPE"
    log "Retention: $RETENTION_DAYS days"
    
    check_dependencies
    load_db_config
    create_backup_dir
    
    local filename
    filename=$(generate_filename)
    local filepath="${BACKUP_DIR}/${filename}"
    
    perform_backup "$filename"
    validate_backup "$filepath"
    cleanup_old_backups
    
    success "=== Backup process completed successfully ==="
    log "Backup location: $filepath"
}

# Execute main function
main "$@"