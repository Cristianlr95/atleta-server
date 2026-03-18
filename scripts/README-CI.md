# Scripts de CI/CD

## Configuración

### Linux/macOS
```bash
# Hacer ejecutables los scripts
chmod +x scripts/ci-local.sh
chmod +x scripts/backup-database.sh
chmod +x scripts/restore-database.sh
```

### Windows
Los scripts están listos para usar. Para ejecutar el script de CI local:

```powershell
# Usando Git Bash (recomendado)
bash scripts/ci-local.sh

# O usando WSL
wsl bash scripts/ci-local.sh
```

## Scripts Disponibles

### `ci-local.sh`
Ejecuta el pipeline de CI completo localmente:
- Inicia PostgreSQL con Docker
- Ejecuta todos los tests
- Valida migraciones
- Construye la aplicación
- Limpia recursos

### Uso
```bash
./scripts/ci-local.sh
```

## Requisitos
- Docker instalado y ejecutándose
- Java 21
- Maven (o usar ./mvnw)

## Troubleshooting

### Error: "Docker not found"
Asegurar que Docker esté instalado y ejecutándose:
```bash
docker --version
docker info
```

### Error: "PostgreSQL timeout"
Aumentar el timeout o verificar recursos del sistema:
```bash
# Verificar logs
docker-compose -f docker-compose.ci.yml logs postgres-ci
```