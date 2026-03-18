# Documentación del Sistema Atleta

## 📋 Índice de Documentación

### 🎯 Guías para Desarrolladores

#### Frontend
- **[../API-REFERENCE-FRONTEND.md](../API-REFERENCE-FRONTEND.md)** ⭐ **NUEVO** - Referencia completa de API
  - Todos los endpoints con URLs completas
  - Ejemplos de código en JavaScript/Fetch/Axios
  - Ejemplos de integración con React/Vue/Angular
  - API Client helper reutilizable
  - Manejo de errores
  - Códigos de respuesta HTTP

- **[../GUIA-API-FRONTEND.md](../GUIA-API-FRONTEND.md)** - Guía rápida de la API
  - Endpoints principales
  - Ejemplos de requests/responses
  - Códigos de error
  - Flujos de autenticación

#### OAuth2
- **[../GOOGLE-OAUTH-SETUP.md](../GOOGLE-OAUTH-SETUP.md)** - Configuración de Google OAuth2
  - Crear proyecto en Google Cloud Console
  - Configurar credenciales
  - Implementar en frontend
  - Troubleshooting

### ⚽ Sistema de Calificaciones

- **[analisis-sistema-calificaciones-y-flujos.md](analisis-sistema-calificaciones-y-flujos.md)** - Análisis completo
  - Fórmulas de cálculo
  - Flujos de actualización
  - Ejemplos prácticos con números
  - Diagramas de flujo

- **[sistema-hexagono-estadisticas.md](sistema-hexagono-estadisticas.md)** - Hexágono estilo Pokémon
  - 6 roles del hexágono
  - Perfiles de jugador típicos
  - Evolución de estadísticas
  - Visualización

- **[calificacion-general-jugador.md](calificacion-general-jugador.md)** - Sistema OVR
  - 5 métodos de cálculo
  - Fórmula híbrida (recomendada)
  - Métricas adicionales
  - Casos de uso

- **[implementacion-ovr-completa.md](implementacion-ovr-completa.md)** - Implementación técnica
  - Archivos modificados
  - Código implementado
  - Endpoints disponibles
  - Ejemplos de uso

### 🔧 Documentación Técnica

- **[endpoints-y-accesos.md](endpoints-y-accesos.md)** - Referencia completa de API
  - 60+ endpoints documentados
  - Configuración de base de datos
  - Variables de entorno
  - Códigos de respuesta HTTP

- **[ci-cd-configuration.md](ci-cd-configuration.md)** - Configuración CI/CD
  - GitLab CI
  - Jenkins
  - Scripts de automatización

- **[database-security-guide.md](database-security-guide.md)** - Seguridad de BD
  - Configuración SSL
  - Usuarios y permisos
  - Mejores prácticas

- **[database-migration-guide.md](database-migration-guide.md)** - Migraciones
  - Flyway
  - Scripts de migración
  - Rollback

- **[ssl-configuration.md](ssl-configuration.md)** - Configuración SSL
  - Certificados
  - Configuración de servidor
  - Troubleshooting

## 🚀 Inicio Rápido

### Para Desarrolladores Frontend

1. Lee **[../API-REFERENCE-FRONTEND.md](../API-REFERENCE-FRONTEND.md)** para referencia completa con ejemplos de código
2. Consulta **[../GUIA-API-FRONTEND.md](../GUIA-API-FRONTEND.md)** para guía rápida
3. Implementa autenticación siguiendo **[../GOOGLE-OAUTH-SETUP.md](../GOOGLE-OAUTH-SETUP.md)**
4. Consulta **[endpoints-y-accesos.md](endpoints-y-accesos.md)** para documentación técnica detallada

### Para Entender el Sistema de Calificaciones

1. Lee **[analisis-sistema-calificaciones-y-flujos.md](analisis-sistema-calificaciones-y-flujos.md)** para entender las fórmulas
2. Revisa **[sistema-hexagono-estadisticas.md](sistema-hexagono-estadisticas.md)** para visualización
3. Consulta **[calificacion-general-jugador.md](calificacion-general-jugador.md)** para el sistema OVR

### Para Configurar el Backend

1. Lee **[../README.md](../README.md)** para instalación básica
2. Configura OAuth2 con **[../GOOGLE-OAUTH-SETUP.md](../GOOGLE-OAUTH-SETUP.md)**
3. Revisa **[database-security-guide.md](database-security-guide.md)** para producción

## 📊 Diagramas y Visualizaciones

Los siguientes documentos incluyen diagramas visuales:

- **[analisis-sistema-calificaciones-y-flujos.md](analisis-sistema-calificaciones-y-flujos.md)**
  - Flujo de registro de usuario
  - Flujo de creación de partido
  - Flujo de actualización de calificaciones
  - Arquitectura del sistema

- **[sistema-hexagono-estadisticas.md](sistema-hexagono-estadisticas.md)**
  - Hexágonos de estadísticas
  - Perfiles de jugador
  - Evolución temporal

## 🔍 Búsqueda Rápida

### ¿Cómo hacer...?

- **Registrar un usuario:** [../GUIA-API-FRONTEND.md](../GUIA-API-FRONTEND.md#1-registro-y-autenticación-de-usuario)
- **Autenticar con Google:** [../GOOGLE-OAUTH-SETUP.md](../GOOGLE-OAUTH-SETUP.md#flujo-de-autenticación)
- **Crear un partido:** [../GUIA-API-FRONTEND.md](../GUIA-API-FRONTEND.md#3-crear-partido)
- **Calcular calificaciones:** [analisis-sistema-calificaciones-y-flujos.md](analisis-sistema-calificaciones-y-flujos.md#sistema-de-cálculo-de-calificaciones)
- **Obtener OVR de jugador:** [implementacion-ovr-completa.md](implementacion-ovr-completa.md#cómo-usar)

### ¿Qué es...?

- **OVR:** [calificacion-general-jugador.md](calificacion-general-jugador.md#concepto-valor-único-por-jugador)
- **Hexágono de estadísticas:** [sistema-hexagono-estadisticas.md](sistema-hexagono-estadisticas.md#concepto-estrella-de-david--hexágono-de-habilidades)
- **Trust Score:** [../GUIA-API-FRONTEND.md](../GUIA-API-FRONTEND.md#23-trust-score)
- **Prioridades de rol:** [analisis-sistema-calificaciones-y-flujos.md](analisis-sistema-calificaciones-y-flujos.md#5-multiplicador-de-prioridad)

## 📝 Notas

- Todos los documentos están en formato Markdown
- Los ejemplos de código incluyen sintaxis completa
- Las fórmulas están explicadas paso a paso
- Los diagramas usan ASCII art para compatibilidad

## 🔄 Actualizaciones

Este índice se actualiza cuando se agrega nueva documentación. Última actualización: 2024-12-20
