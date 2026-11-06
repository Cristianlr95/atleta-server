# Revisión acotada de permisos — 1 de noviembre de 2026

Se revisaron operaciones de calificaciones, confianza, experiencia por posición y edición del catálogo de canchas. La comprobación se ejecutó con dos cuentas sintéticas y una base H2 temporal; no se consultaron ni alteraron datos reales. No es una auditoría completa de todos los endpoints.

## Hallazgos confirmados

1. **Rating manual:** una cuenta autenticada invocó `POST /api/v1/ratings/update` para sí misma con estadísticas inventadas y un partido creado por la otra cuenta. El partido estaba en estado `CREADO` y el jugador no era participante. La operación respondió 200 y quedó una calificación de 99 con un partido contabilizado. La comprobación de identidad del controlador no valida participación confirmada ni resultado verificado antes de actualizar.
2. **Trust score:** una cuenta autenticada invocó `PUT /api/v1/player-profiles/trust-score` con un cambio elegido por ella. Respondió 200 y su puntuación bajó de 100 a 90. El controlador fija el jugador al usuario autenticado, pero no exige que el cambio proceda de una regla de negocio verificada.
3. **Experiencia:** una cuenta autenticada invocó `PUT /api/v1/player-profiles/{uuid}/positions/{id}/experience?xp=10000` para su propia posición. Respondió 200 y la consulta posterior mostró 10 000 XP. La comprobación de identidad no verifica que la experiencia derive de un partido.
4. **Canchas compartidas:** una cuenta creó una cancha y la otra la editó y desactivó con `PUT /api/v1/fields/{id}`. Respondió 200 y el cambio persistió al consultar también las canchas inactivas. El modelo revisado no identifica propietario y el controlador solo exige autenticación.

## Restricción temporal aplicada localmente

Actualización del 2 de noviembre: el usuario eligió que solo el creador edite o desactive su cancha. Se implementó localmente esa regla; las canchas antiguas sin creador permanecen protegidas. Los otros bloqueos descritos abajo siguen vigentes. El texto siguiente conserva la evidencia de la mitigación inicial.

El filtro de seguridad rechaza temporalmente las cuatro escrituras HTTP con 403 para usuarios autenticados y 401 sin sesión. El cálculo interno al cerrar partidos, la lectura y la creación de canchas siguen disponibles. Las pruebas dirigidas abarcan esos comportamientos y el cierre con arquero rotativo. No se han cambiado datos reales ni desplegado esta configuración.

Las integraciones que usan las cuatro rutas manuales dejarán de funcionar al desplegar el cambio. Para habilitarlas se requiere una operación ligada a resultados y participantes verificados, o una política de propietario/administración para canchas, y pruebas de autorización correspondientes. La revisión no cubre todos los endpoints.
