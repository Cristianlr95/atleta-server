# Documento de Requerimientos

## Introducción

El Sistema de Calificación de Jugadores calcula y actualiza las calificaciones de los jugadores basándose en su rendimiento en los partidos. El sistema considera los roles de los jugadores, prioridades, resultados de partidos, métricas de rendimiento individual (goles, asistencias), rendimiento defensivo y estatus de MVP para ajustar dinámicamente las calificaciones de los jugadores manteniendo umbrales mínimos.

## Glosario

- **Sistema_Calificacion**: El sistema central que calcula y actualiza las calificaciones de los jugadores
- **Rol_Jugador**: La posición que ocupa un jugador durante un partido (ATAQUE, DEFENSA, MEDIOCAMPO, CARRILERO, ARQUERO, DT). Los laterales izquierdo y derecho se clasifican como CARRILERO
- **Nivel_Prioridad**: El nivel de importancia de un rol para un jugador (Principal, Secundaria, Terciaria)
- **Calificacion_Base**: El valor mínimo de calificación por debajo del cual un jugador no puede caer para cada nivel de prioridad
- **Resultado_Partido**: El resultado de un partido desde la perspectiva de un jugador (GANADO, EMPATE, PERDIDO)
- **Metricas_Rendimiento**: Estadísticas individuales como goles anotados, asistencias realizadas, goles recibidos
- **Estatus_MVP**: Designación de Jugador Más Valioso por rendimiento excepcional
- **Arquero_Rotativo**: Un modo de partido donde todos los jugadores reciben actualizaciones de calificación de arquero

## Requerimientos

### Requerimiento 1: Gestión de Roles de Jugadores

**Historia de Usuario:** Como administrador del sistema, quiero definir roles de jugadores con reglas específicas de cálculo de calificación, para que diferentes posiciones sean evaluadas apropiadamente basándose en sus responsabilidades.

#### Criterios de Aceptación

1. EL Sistema_Calificacion DEBERÁ soportar exactamente cinco roles de jugador: ATAQUE, DEFENSA, MEDIOCAMPO, CARRILERO (incluye laterales izquierdo y derecho), ARQUERO, DT
2. CUANDO se calculen calificaciones, EL Sistema_Calificacion DEBERÁ aplicar pesos específicos por rol para goles y asistencias
3. CUANDO un jugador tenga rol DEFENSA o ARQUERO, EL Sistema_Calificacion DEBERÁ aplicar bonos defensivos basados en goles recibidos
4. EL Sistema_Calificacion DEBERÁ mantener pesos de goles separados para cada rol: ATAQUE (1.0), MEDIOCAMPO (0.6), CARRILERO (0.5), DEFENSA (0.3), ARQUERO (0.1), DT (0.2)
5. EL Sistema_Calificacion DEBERÁ mantener pesos de asistencias separados para cada rol: MEDIOCAMPO (1.0), CARRILERO (0.7), ATAQUE (0.6), DEFENSA (0.3), ARQUERO (0.0), DT (0.2)

### Requerimiento 2: Sistema de Niveles de Prioridad

**Historia de Usuario:** Como jugador, quiero tener diferentes niveles de prioridad para diferentes roles, para que mi posición principal tenga más impacto en mi calificación general que las posiciones secundarias.

#### Criterios de Aceptación

1. EL Sistema_Calificacion DEBERÁ soportar exactamente tres niveles de prioridad: Principal, Secundaria, Terciaria
2. EL Sistema_Calificacion DEBERÁ aplicar calificaciones base mínimas: Principal (70), Secundaria (60), Terciaria (50)
3. EL Sistema_Calificacion DEBERÁ aplicar multiplicadores de prioridad: Principal (1.0), Secundaria (0.7), Terciaria (0.4)
4. CUANDO se calculen nuevas calificaciones, EL Sistema_Calificacion DEBERÁ aplicar el multiplicador de prioridad al delta de calificación antes de agregarlo a la calificación actual
5. CUANDO una calificación calculada caiga por debajo del mínimo base, EL Sistema_Calificacion DEBERÁ establecer la calificación al valor mínimo base

### Requerimiento 3: Impacto del Resultado del Partido

**Historia de Usuario:** Como jugador, quiero que mi calificación refleje el rendimiento del equipo, para que ganar contribuya positivamente a mi calificación mientras que perder tenga un impacto negativo.

#### Criterios de Aceptación

1. CUANDO el resultado de un partido sea GANADO, EL Sistema_Calificacion DEBERÁ agregar 2.0 puntos al delta de calificación
2. CUANDO el resultado de un partido sea EMPATE, EL Sistema_Calificacion DEBERÁ agregar 0.5 puntos al delta de calificación
3. CUANDO el resultado de un partido sea PERDIDO, EL Sistema_Calificacion DEBERÁ restar 1.5 puntos del delta de calificación
4. EL Sistema_Calificacion DEBERÁ aplicar puntos de resultado de partido antes de aplicar multiplicadores de prioridad
5. EL Sistema_Calificacion DEBERÁ aplicar el impacto del resultado del partido a todos los jugadores independientemente de su rol

### Requerimiento 4: Métricas de Rendimiento Individual

**Historia de Usuario:** Como jugador, quiero que mi rendimiento individual (goles y asistencias) impacte mi calificación, para que las contribuciones individuales excepcionales sean recompensadas apropiadamente.

#### Criterios de Aceptación

1. CUANDO se calculen calificaciones, EL Sistema_Calificacion DEBERÁ multiplicar los goles anotados por el peso de goles específico del rol
2. CUANDO se calculen calificaciones, EL Sistema_Calificacion DEBERÁ multiplicar las asistencias realizadas por el peso de asistencias específico del rol
3. EL Sistema_Calificacion DEBERÁ agregar los goles y asistencias ponderados al delta de calificación
4. EL Sistema_Calificacion DEBERÁ aceptar valores enteros cero o positivos para goles y asistencias
5. EL Sistema_Calificacion DEBERÁ aplicar métricas de rendimiento individual antes de aplicar multiplicadores de prioridad

### Requerimiento 5: Bonos de Rendimiento Defensivo

**Historia de Usuario:** Como jugador defensivo o arquero, quiero recibir bonos por mantener la portería en cero o limitar los goles recibidos, para que la excelencia defensiva sea recompensada apropiadamente.

#### Criterios de Aceptación

1. CUANDO un jugador tenga rol DEFENSA y los goles recibidos sean igual a 0, EL Sistema_Calificacion DEBERÁ agregar 2.0 puntos de bono
2. CUANDO un jugador tenga rol DEFENSA y los goles recibidos sean igual a 1, EL Sistema_Calificacion DEBERÁ agregar 1.0 puntos de bono
3. CUANDO un jugador tenga rol DEFENSA y los goles recibidos sean igual a 2, EL Sistema_Calificacion DEBERÁ agregar 0.5 puntos de bono
4. CUANDO un jugador tenga rol ARQUERO y los goles recibidos sean igual a 0, EL Sistema_Calificacion DEBERÁ agregar 2.5 puntos de bono
5. CUANDO un jugador tenga rol ARQUERO y los goles recibidos sean igual a 1, EL Sistema_Calificacion DEBERÁ agregar 1.2 puntos de bono
6. CUANDO un jugador tenga rol ARQUERO y los goles recibidos sean igual a 2, EL Sistema_Calificacion DEBERÁ agregar 0.7 puntos de bono
7. CUANDO los goles recibidos sean 3 o más, EL Sistema_Calificacion DEBERÁ agregar 0 puntos de bono tanto para roles DEFENSA como ARQUERO
8. EL Sistema_Calificacion DEBERÁ aplicar bonos defensivos antes de aplicar multiplicadores de prioridad

### Requerimiento 6: Reconocimiento MVP

**Historia de Usuario:** Como jugador, quiero recibir puntos adicionales de calificación cuando sea seleccionado como MVP, para que el rendimiento general excepcional sea reconocido más allá de las estadísticas individuales.

#### Criterios de Aceptación

1. CUANDO un jugador sea designado como MVP, EL Sistema_Calificacion DEBERÁ agregar 1.0 puntos de bono al delta de calificación
2. EL Sistema_Calificacion DEBERÁ aplicar el bono MVP antes de aplicar multiplicadores de prioridad
3. EL Sistema_Calificacion DEBERÁ aceptar el estatus MVP como un valor booleano
4. EL Sistema_Calificacion DEBERÁ permitir solo un MVP por partido
5. EL Sistema_Calificacion DEBERÁ aplicar el bono MVP independientemente del rol del jugador

### Requerimiento 7: Modo Arquero Rotativo

**Historia de Usuario:** Como organizador de partidos, quiero habilitar el modo arquero rotativo, para que todos los jugadores reciban actualizaciones de calificación de arquero cuando este modo especial esté activo.

#### Criterios de Aceptación

1. CUANDO el modo arquero rotativo esté habilitado, EL Sistema_Calificacion DEBERÁ calcular calificaciones de arquero para TODOS los jugadores en el partido
2. CUANDO esté en modo arquero rotativo, EL Sistema_Calificacion DEBERÁ usar puntos de resultado modificados: GANADO (+1.5), EMPATE (+0.3), PERDIDO (-1.2)
3. CUANDO esté en modo arquero rotativo, EL Sistema_Calificacion DEBERÁ aplicar las mismas reglas de mínimo base para la prioridad de arquero
4. CUANDO esté en modo arquero rotativo, EL Sistema_Calificacion DEBERÁ aplicar las mismas reglas de multiplicador de prioridad para la prioridad de arquero
5. EL Sistema_Calificacion DEBERÁ procesar las calificaciones de arquero rotativo independientemente de las calificaciones de rol regular

### Requerimiento 8: Motor de Cálculo de Calificaciones

**Historia de Usuario:** Como sistema, quiero calcular nuevas calificaciones de jugadores usando un algoritmo consistente, para que todas las actualizaciones de calificación sigan las mismas reglas matemáticas y mantengan la integridad de los datos.

#### Criterios de Aceptación

1. EL Sistema_Calificacion DEBERÁ calcular el delta de calificación sumando: puntos de resultado de partido + goles ponderados + asistencias ponderadas + bono defensivo + bono MVP
2. EL Sistema_Calificacion DEBERÁ multiplicar el delta de calificación por el multiplicador de prioridad
3. EL Sistema_Calificacion DEBERÁ agregar el delta ajustado a la calificación actual para obtener la nueva calificación
4. CUANDO la nueva calificación esté por debajo del mínimo base, EL Sistema_Calificacion DEBERÁ establecer la nueva calificación al mínimo base
5. EL Sistema_Calificacion DEBERÁ retornar la calificación final calculada como un número decimal
6. EL Sistema_Calificacion DEBERÁ procesar todos los componentes de calificación en el orden especificado: resultado → rendimiento individual → defensivo → MVP → multiplicador de prioridad → aplicación de mínimo base

### Requerimiento 9: Persistencia de Datos e Integración

**Historia de Usuario:** Como desarrollador, quiero que el sistema de calificación se integre con las entidades existentes de jugador y partido, para que las calificaciones sean almacenadas apropiadamente y puedan ser recuperadas para análisis.

#### Criterios de Aceptación

1. EL Sistema_Calificacion DEBERÁ almacenar las calificaciones de jugadores asociadas con su entidad PlayerProfile
2. EL Sistema_Calificacion DEBERÁ crear registros de historial de calificaciones para cada actualización de calificación
3. EL Sistema_Calificacion DEBERÁ vincular las actualizaciones de calificación a partidos y roles específicos
4. EL Sistema_Calificacion DEBERÁ validar que todos los datos de entrada requeridos existan antes de calcular calificaciones
5. EL Sistema_Calificacion DEBERÁ manejar actualizaciones concurrentes de calificación de manera segura usando mecanismos de bloqueo apropiados