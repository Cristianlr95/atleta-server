param(
    [string]$DbHost = "localhost",
    [int]$DbPort = 5432,
    [string]$DbName = "atleta_dev",
    [string]$DbUsername = "postgres",
    [string]$DbPassword = "12345",
    [string]$ServerPort = "8080",
    [string]$GoogleClientId = "",
    [string]$GoogleClientSecret = "",
    [switch]$NoRun,
    [switch]$PersistUserEnv,
    [switch]$CreateDb,
    [string]$PostgresAdminUser = "postgres",
    [string]$PostgresAdminPassword = ""
)

$ErrorActionPreference = "Stop"

function Write-Step([string]$Message) {
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Set-SessionVar([string]$Name, [string]$Value) {
    Set-Item -Path "Env:$Name" -Value $Value
    if ($PersistUserEnv) {
        [Environment]::SetEnvironmentVariable($Name, $Value, "User")
    }
}

function Ensure-Command([string]$CommandName, [string]$InstallHint) {
    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        throw "No se encontro '$CommandName'. $InstallHint"
    }
}

Write-Step "Validando prerequisitos"
Ensure-Command -CommandName "java" -InstallHint "Instala JDK 21 y agrega java al PATH."

$repoRoot = Split-Path -Parent $PSScriptRoot
$mavenWrapper = Join-Path $repoRoot "mvnw.cmd"
if (-not (Test-Path $mavenWrapper)) {
    throw "No se encontro mvnw.cmd en el repositorio: $mavenWrapper"
}

if ($CreateDb) {
    Write-Step "Creando/verificando base de datos en PostgreSQL"
    Ensure-Command -CommandName "psql" -InstallHint "Instala PostgreSQL client tools y agrega psql al PATH."

    if ([string]::IsNullOrWhiteSpace($PostgresAdminPassword)) {
        $securePassword = Read-Host "Password del usuario administrador PostgreSQL ($PostgresAdminUser)" -AsSecureString
        $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
        try {
            $PostgresAdminPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
        }
        finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
        }
    }

    $env:PGPASSWORD = $PostgresAdminPassword

    $sql = @"
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '$DbUsername') THEN
        EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', '$DbUsername', '$DbPassword');
    END IF;
END
\$\$;

SELECT 'CREATE DATABASE $DbName OWNER $DbUsername'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$DbName')\gexec

GRANT ALL PRIVILEGES ON DATABASE "$DbName" TO "$DbUsername";
"@

    $sql | psql -h $DbHost -p $DbPort -U $PostgresAdminUser -d postgres | Out-Null
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}

Write-Step "Configurando variables de entorno para Spring Boot (perfil dev)"
Set-SessionVar -Name "DB_HOST" -Value $DbHost
Set-SessionVar -Name "DB_PORT" -Value "$DbPort"
Set-SessionVar -Name "DB_NAME" -Value $DbName
Set-SessionVar -Name "DB_USERNAME" -Value $DbUsername
Set-SessionVar -Name "DB_PASSWORD" -Value $DbPassword
Set-SessionVar -Name "SERVER_PORT" -Value $ServerPort

if (-not [string]::IsNullOrWhiteSpace($GoogleClientId)) {
    Set-SessionVar -Name "GOOGLE_CLIENT_ID" -Value $GoogleClientId
}
if (-not [string]::IsNullOrWhiteSpace($GoogleClientSecret)) {
    Set-SessionVar -Name "GOOGLE_CLIENT_SECRET" -Value $GoogleClientSecret
}

Write-Step "Resumen de configuracion"
Write-Host "DB_HOST=$DbHost"
Write-Host "DB_PORT=$DbPort"
Write-Host "DB_NAME=$DbName"
Write-Host "DB_USERNAME=$DbUsername"
Write-Host "SERVER_PORT=$ServerPort"
if ($PersistUserEnv) {
    Write-Host "Variables persistidas para el usuario actual: SI"
}
else {
    Write-Host "Variables persistidas para el usuario actual: NO (solo sesion actual)"
}

if ($NoRun) {
    Write-Step "Configuracion finalizada (sin iniciar servidor por -NoRun)"
    exit 0
}

Write-Step "Iniciando servidor local con Maven Wrapper"
Push-Location $repoRoot
try {
    & $mavenWrapper spring-boot:run
}
finally {
    Pop-Location
}
