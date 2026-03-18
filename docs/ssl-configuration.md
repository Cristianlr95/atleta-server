# Configuración SSL para Base de Datos PostgreSQL

## Visión General

Este documento describe la configuración SSL/TLS para conexiones seguras a PostgreSQL en el ambiente de producción del proyecto Atleta.

## Configuración de Certificados

### Variables de Entorno Requeridas

Para habilitar SSL en producción, configure las siguientes variables de entorno:

```bash
# Configuración SSL obligatoria para producción
SSL_CERT_PATH=/path/to/client-cert.pem          # Certificado del cliente
SSL_KEY_PATH=/path/to/client-key.pem            # Clave privada del cliente
SSL_ROOT_CERT_PATH=/path/to/ca-cert.pem         # Certificado de la CA raíz
SSL_PASSWORD=your_ssl_password                  # Contraseña del certificado (opcional)

# Configuración de base de datos
DB_HOST=your-production-db-host.com
DB_PORT=5432
DB_NAME=atleta_production
DB_USERNAME=atleta_prod_user
DB_PASSWORD=your_secure_password
```

### Tipos de Certificados

#### 1. Certificado del Cliente (client-cert.pem)
- Identifica la aplicación ante el servidor PostgreSQL
- Debe estar firmado por una CA confiable
- Formato: PEM

#### 2. Clave Privada del Cliente (client-key.pem)
- Clave privada correspondiente al certificado del cliente
- Debe mantenerse segura y con permisos restrictivos (600)
- Formato: PEM

#### 3. Certificado de la CA Raíz (ca-cert.pem)
- Certificado de la Autoridad Certificadora que firmó el certificado del servidor
- Utilizado para verificar la autenticidad del servidor PostgreSQL
- Formato: PEM

## Configuración del Servidor PostgreSQL

### postgresql.conf
```ini
# Habilitar SSL
ssl = on
ssl_cert_file = 'server.crt'
ssl_key_file = 'server.key'
ssl_ca_file = 'ca.crt'

# Configuraciones de seguridad SSL
ssl_ciphers = 'HIGH:MEDIUM:+3DES:!aNULL'
ssl_prefer_server_ciphers = on
ssl_protocols = 'TLSv1.2,TLSv1.3'
```

### pg_hba.conf
```ini
# Requerir SSL para conexiones remotas
hostssl all atleta_prod_user 0.0.0.0/0 cert
hostssl all atleta_prod_user ::/0 cert
```

## Modos SSL Disponibles

| Modo | Descripción | Seguridad |
|------|-------------|-----------|
| `disable` | Sin SSL | Ninguna |
| `allow` | SSL si está disponible | Baja |
| `prefer` | Preferir SSL | Media |
| `require` | SSL obligatorio | Alta |
| `verify-ca` | SSL + verificar CA | Muy Alta |
| `verify-full` | SSL + verificar CA + hostname | Máxima |

**Producción usa `require`**: SSL obligatorio pero sin verificación de certificados del servidor.

## Configuración de HikariCP

La configuración SSL se aplica tanto en la URL de conexión como en las propiedades del DataSource:

```yaml
spring:
  datasource:
    hikari:
      data-source-properties:
        ssl: true
        sslmode: require
        sslcert: ${SSL_CERT_PATH}
        sslkey: ${SSL_KEY_PATH}
        sslrootcert: ${SSL_ROOT_CERT_PATH}
        connectTimeout: 30000
        socketTimeout: 60000
        tcpKeepAlive: true
```

## Verificación de SSL

### Comando de Verificación
```bash
# Verificar conexión SSL
psql "host=your-db-host.com port=5432 dbname=atleta_production user=atleta_prod_user sslmode=require sslcert=client-cert.pem sslkey=client-key.pem sslrootcert=ca-cert.pem" -c "SELECT version();"
```

### Verificar Estado SSL en PostgreSQL
```sql
-- Verificar conexiones SSL activas
SELECT pid, usename, application_name, client_addr, ssl, cipher, bits, compression
FROM pg_stat_ssl 
JOIN pg_stat_activity USING (pid)
WHERE ssl = true;
```

## Troubleshooting

### Errores Comunes

#### 1. "SSL connection has been closed unexpectedly"
- **Causa**: Certificados inválidos o expirados
- **Solución**: Verificar validez y configuración de certificados

#### 2. "FATAL: no pg_hba.conf entry for host"
- **Causa**: Configuración incorrecta en pg_hba.conf
- **Solución**: Agregar entrada hostssl apropiada

#### 3. "SSL error: certificate verify failed"
- **Causa**: Certificado de CA no confiable
- **Solución**: Verificar certificado de CA raíz

### Logs de Diagnóstico

Habilitar logging SSL en PostgreSQL:
```ini
log_connections = on
log_disconnections = on
log_statement = 'all'
```

## Seguridad Adicional

### Permisos de Archivos
```bash
# Configurar permisos seguros para certificados
chmod 600 client-key.pem
chmod 644 client-cert.pem
chmod 644 ca-cert.pem

# Propietario correcto
chown app:app *.pem
```

### Rotación de Certificados

1. **Planificación**: Rotar certificados antes del vencimiento
2. **Proceso**: 
   - Generar nuevos certificados
   - Actualizar variables de entorno
   - Reiniciar aplicación
   - Verificar conectividad

### Monitoreo

- Monitorear fecha de expiración de certificados
- Alertas automáticas 30 días antes del vencimiento
- Verificación periódica de conectividad SSL

## Referencias

- [PostgreSQL SSL Documentation](https://www.postgresql.org/docs/current/ssl-tcp.html)
- [HikariCP SSL Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [PostgreSQL JDBC SSL](https://jdbc.postgresql.org/documentation/ssl/)