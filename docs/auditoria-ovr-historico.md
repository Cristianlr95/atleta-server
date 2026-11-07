# Diagnóstico de resultados históricos de rating

La consulta `scripts/audit-internal-rating-results.sql` cuenta posibles diferencias entre el resultado registrado en `rating_history` y el marcador y lado guardados para partidos internos finalizados. La consulta `scripts/audit-opposing-team-rating-results.sql` hace el mismo contraste para amistosos y partidos por puntos, usando los goles de los dos equipos y el equipo asignado al jugador. Ambas cuentan por separado los resultados indeterminables, no devuelven identidades y ejecutan una transacción de solo lectura.

Ejecutarla únicamente sobre una copia de PostgreSQL con un usuario de solo lectura, después de comprobar que el esquema corresponde al proyecto:

```powershell
psql -X -v ON_ERROR_STOP=1 -f scripts/audit-internal-rating-results.sql
psql -X -v ON_ERROR_STOP=1 -f scripts/audit-opposing-team-rating-results.sql
```

Configurar la conexión de la copia mediante los mecanismos seguros habituales de PostgreSQL; no incluir contraseñas en la línea de comandos ni en archivos del proyecto. La consulta puede tardar en bases grandes.

Los conteos son candidatos, no un cálculo de impacto ni autorización para corregir datos. Las consultas no detectan ratings que nunca se crearon, goles recibidos erróneos o información histórica perdida. Los lados, equipos y marcadores actuales pueden no representar fielmente el estado al cierre; los marcadores añadidos por la migración V011 tienen valor predeterminado cero. Las versiones previas a V026 no registraban la modalidad de arquero rotativo. Se requiere verificar la evidencia original antes de decidir qué corregir.

Prueba local: PostgreSQL 18 temporal con seis registros sintéticos: cuatro examinados, uno indeterminable y uno discrepante; se excluyeron un partido externo y un registro de arquero rotativo. No se consultó ninguna base real ni se cambió ningún rating.

Prueba adicional de equipos opuestos: siete registros sintéticos, de los cuales se examinaron cinco; dos fueron indeterminables y uno discrepante. Se excluyeron un partido interno y un registro de arquero rotativo. Se cubrieron victoria, empate, equipo incompleto y asignación ajena.
