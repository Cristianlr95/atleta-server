# Revisión del motor XP independiente

Revisión local del 17/11/2026. El cierre actual usa `MatchXpCalculator` desde `MatchPlayerHistoryService`; la vista previa de `MatchService` usa el mismo cálculo. No se encontraron consumidores de producción de `XPService.calculateXp`; las llamadas encontradas pertenecen a sus pruebas.

Las reglas difieren: una victoria con un gol de delantero y ninguna asistencia genera 14 XP con el cálculo actual (5 participación, 5 victoria, 4 gol), frente a 30 XP con `XPService` (10, 10, 10). El cálculo actual limita XP base a 25; el motor independiente no aplica ese límite, añade XP por asistencias y premios de arquero, y usa otra escala. No se deben intercambiar sin una decisión de producto y evidencia de balance.

El motor independiente exige participación confirmada y devuelve cero en partido inválido. Antes de conectarlo deben definirse contratos y límites: operaciones con enteros grandes pueden desbordarse; los valores negativos se convierten en cero, lo que puede convertir goles recibidos inválidos en valla invicta; nombres de posición no reconocidos, como los laterales del catálogo, no reciben XP por gol. Se necesitan validación de entrada, aritmética segura, reglas de posiciones, pruebas de límites y concurrencia/idempotencia para cualquier persistencia. El motor no implementa por sí mismo la persistencia ni previene duplicación.

Las reglas actuales no se modificaron ni se recalculó historial. El balance y la decisión de conectar o retirar el motor permanecen pendientes de datos reales y aprobación de reglas; la revisión no valida su uso en producción.
