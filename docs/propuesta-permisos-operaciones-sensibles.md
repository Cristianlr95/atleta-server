# Propuesta de permisos para operaciones sensibles

Estado: propuesta para revisión, 2 de noviembre de 2026. El usuario decidió que solo el creador podrá editar o desactivar una cancha; esa decisión se implementó localmente. Las otras restricciones siguen vigentes.

| Operación | Situación comprobada | Propuesta y condición para habilitar |
| --- | --- | --- |
| Rating manual (`POST /api/v1/ratings/update`) y arquero rotativo manual | Bloqueadas. El cierre llama al cálculo interno, con validación del responsable, partido iniciado y mínimo de confirmados. | Mantener bloqueadas las escrituras manuales. Calcular a partir del cierre y de datos confirmados; cualquier nuevo mecanismo debe comprobar el partido, participantes y resultado, y evitar duplicados. |
| XP manual (`PUT /api/v1/player-profiles/{uuid}/positions/{id}/experience`) | Bloqueada. El cierre y la votación MVP tienen procesos internos de XP. | Mantener bloqueada la cantidad elegida por el cliente. Otorgar XP mediante reglas verificables del servidor, una sola vez por evento, con pruebas de concurrencia. |
| Trust score manual (`PUT /api/v1/player-profiles/trust-score`) | Bloqueada. Existen métodos de bonificación y penalización en el servicio, pero no se identificaron llamadas desde otros componentes de producción. | No habilitar la escritura manual ni activar penalizaciones hasta definir hechos verificables, reglas, responsables y prevención de duplicados. Registrar el motivo y evento de cada cambio. |
| Edición de cancha (`PUT /api/v1/fields/{id}`) | Implementada localmente para el creador autenticado; las canchas anteriores sin creador permanecen protegidas. | Solo el creador puede editar o desactivar. El servidor obtiene su identidad de la sesión, no de la solicitud. No se asignan dueños históricos por suposición. |

La inicialización de calificaciones base es una operación distinta: el controlador exige que el perfil corresponda al usuario autenticado y el servicio no reduce ratings existentes. No es autorización para modificar resultados de partidos.

## Implementación y verificación pendientes

- Las canchas nuevas registran el creador autenticado. La migración conserva sin propietario las anteriores; su edición queda bloqueada hasta una asignación verificable y autorizada.
- Antes de levantar cualquier bloqueo, probar sin sesión, otro usuario, usuario autorizado, recurso inexistente, datos inválidos y operaciones repetidas o concurrentes según corresponda. Verificar creación y lectura de canchas y el cierre para evitar regresiones.
- Revisar compatibilidad de consumidores: existen métodos frontend para trust score y edición de cancha, además de pruebas de contrato para las rutas manuales. No se comprobó consumo por integraciones externas; comunicar cualquier retirada o cambio antes de desplegar.
- Los cambios locales no están desplegados. Requieren aprobación y preparación del despliegue correspondiente; no asignar propietarios ni recalcular datos históricos por inferencia.
