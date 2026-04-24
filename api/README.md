# API Atleta

## Proposito
Documentacion por secciones para integrar clientes frontend con Atleta Server. Todas las rutas son relativas a:

```text
http://localhost:8080/api/v1
```

## Secciones
- [`00-getting-started.md`](00-getting-started.md): configuracion inicial y convenciones.
- [`01-autenticacion.md`](01-autenticacion.md): registro, login local y Google OAuth.
- [`02-perfiles-jugadores.md`](02-perfiles-jugadores.md): perfiles, alias y posiciones.
- [`03-equipos.md`](03-equipos.md): equipos y miembros.
- [`04-partidos.md`](04-partidos.md): partidos, invitaciones, eventos y cierre.
- [`05-calificaciones.md`](05-calificaciones.md): ratings, OVR, historial y leaderboard.
- [`06-utilidades.md`](06-utilidades.md): helpers, errores y ejemplos.

## Flujos principales
1. Registro o login.
2. Creacion de perfil de jugador.
3. Seleccion de posiciones.
4. Creacion o asociacion a equipo.
5. Creacion de partido e invitaciones.
6. Confirmacion, cierre, MVP y actualizacion de ratings.

## Convenciones
- Usar `Authorization: Bearer <token>` en endpoints privados.
- Enviar y recibir JSON.
- Usar fechas ISO 8601.
- Los identificadores de atletas/jugadores suelen ser UUID.
- Algunos agregados usan IDs numericos (`Long`).

## Estado
Referencia funcional para desarrollo. Si hay diferencias con Swagger o el codigo, considerar el codigo fuente y OpenAPI generado como fuente de verdad.
