# Análisis del Sistema de Calificaciones y Flujos Principales

## Tabla de Contenidos
1. [Sistema de Cálculo de Calificaciones](#sistema-de-cálculo-de-calificaciones)
2. [Flujo de Registro de Usuario](#flujo-de-registro-de-usuario)
3. [Flujo de Creación y Finalización de Partido](#flujo-de-creación-y-finalización-de-partido)
4. [Flujo de Actualización de Calificaciones](#flujo-de-actualización-de-calificaciones)
5. [Ejemplos Prácticos con Cálculos](#ejemplos-prácticos-con-cálculos)

---

## Sistema de Cálculo de Calificaciones

### Fórmula General

```
CALIFICACIÓN_NUEVA = CALIFICACIÓN_ACTUAL + (DELTA_BASE × MULTIPLICADOR_PRIORIDAD)

Donde:
DELTA_BASE = PUNTOS_RESULTADO + GOLES_PONDERADOS + ASISTENCIAS_PONDERADAS + BONO_DEFENSIVO + BONO_MVP
```

### Componentes del Cálculo

#### 1. Puntos de Resultado del Partido

| Resultado | Puntos Normales | Puntos Arquero Rotativo |
|-----------|----------------|------------------------|
| GANADO    | +2.0           | +1.5                   |
| EMPATE    | +0.5           | +0.3                   |
| PERDIDO   | -1.5           | -1.2                   |

#### 2. Pesos por Rol (para Goles y Asistencias)

| Rol         | Peso Goles | Peso Asistencias | Descripción |
|-------------|-----------|------------------|-------------|
| ATAQUE      | 1.0       | 0.6              | Máximo peso en goles |
| MEDIOCAMPO  | 0.6       | 1.0              | Máximo peso en asistencias |
| CARRILERO   | 0.5       | 0.7              | Balance medio-alto |
| DEFENSA     | 0.3       | 0.3              | Peso bajo en ambos |
| ARQUERO     | 0.1       | 0.0              | Peso mínimo |
| DT          | 0.2       | 0.2              | Peso bajo |


#### 3. Bono Defensivo (solo para DEFENSA y ARQUERO)

**Para DEFENSA:**
| Goles Recibidos | Bono |
|----------------|------|
| 0              | +2.0 |
| 1              | +1.0 |
| 2              | +0.5 |
| 3 o más        | 0.0  |

**Para ARQUERO:**
| Goles Recibidos | Bono |
|----------------|------|
| 0              | +2.5 |
| 1              | +1.2 |
| 2              | +0.7 |
| 3 o más        | 0.0  |

#### 4. Bono MVP

```
Si el jugador es MVP: +1.0 puntos
Si no es MVP: 0.0 puntos
```

#### 5. Multiplicador de Prioridad

| Prioridad   | Calificación Base Mínima | Multiplicador |
|-------------|-------------------------|---------------|
| PRINCIPAL   | 70                      | 1.0 (100%)    |
| SECUNDARIA  | 60                      | 0.7 (70%)     |
| TERCIARIA   | 50                      | 0.4 (40%)     |

### Algoritmo Paso a Paso

```
PASO 1: Calcular Delta Base
  delta = 0
  delta += puntos_resultado
  delta += (goles_anotados × peso_goles_del_rol)
  delta += (asistencias × peso_asistencias_del_rol)
  delta += bono_defensivo (si aplica)
  delta += bono_mvp (si aplica)

PASO 2: Aplicar Multiplicador de Prioridad
  delta_ajustado = delta × multiplicador_prioridad

PASO 3: Calcular Nueva Calificación
  nueva_calificacion = calificacion_actual + delta_ajustado

PASO 4: Aplicar Límite Mínimo
  si nueva_calificacion < calificacion_base_minima:
    nueva_calificacion = calificacion_base_minima
```


---

## Flujo de Registro de Usuario

### Diagrama de Flujo

```
┌─────────────────────────────────────────────────────────────────┐
│                    REGISTRO DE USUARIO                          │
└─────────────────────────────────────────────────────────────────┘

1. Cliente envía POST /api/v1/athletes/register
   ↓
   {
     "nombre": "Juan Pérez",
     "email": "juan@example.com",
     "password": "password123"
   }

2. AthleteService.registerAthlete()
   ↓
   ┌─────────────────────────────────────┐
   │ Validar email único                 │
   │ ¿Email ya existe?                   │
   └─────────────────────────────────────┘
          │ NO                    │ SI
          ↓                       ↓
   ┌──────────────┐        ┌──────────────┐
   │ Continuar    │        │ Error 409    │
   └──────────────┘        │ Conflict     │
          ↓                └──────────────┘
   ┌─────────────────────────────────────┐
   │ Hashear contraseña con BCrypt       │
   │ (Seguridad: almacena solo hash)     │
   └─────────────────────────────────────┘
          ↓
   ┌─────────────────────────────────────┐
   │ Crear entidad Athlete               │
   │ - UUID generado automáticamente     │
   │ - Fecha creación automática         │
   │ - Estado: ACTIVO                    │
   └─────────────────────────────────────┘
          ↓
   ┌─────────────────────────────────────┐
   │ Guardar en base de datos            │
   └─────────────────────────────────────┘
          ↓
   ┌─────────────────────────────────────┐
   │ Retornar AthleteResponse            │
   │ Status: 201 Created                 │
   └─────────────────────────────────────┘
```

### Ejemplo de Respuesta

```json
{
  "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
  "email": "juan@example.com",
  "nombre": "Juan Pérez",
  "createdAt": "2024-12-20T10:30:00"
}
```


---

## Flujo de Creación y Finalización de Partido

### Diagrama Completo

```
┌──────────────────────────────────────────────────────────────────────┐
│                  CICLO DE VIDA DE UN PARTIDO                         │
└──────────────────────────────────────────────────────────────────────┘

FASE 1: CREACIÓN
═══════════════════════════════════════════════════════════════════════

POST /api/v1/matches
{
  "creadorUuid": "uuid",
  "modalidad": "CINCO_VS_CINCO",
  "fechaHoraProgramada": "2024-12-25T18:00:00",
  "latitud": -34.603722,
  "longitud": -58.381592,
  "cuota": 500.0
}
    ↓
┌─────────────────────────────────────┐
│ MatchService.createMatch()          │
│ - Validar creador existe            │
│ - Estado inicial: CREADO            │
│ - Guardar en BD                     │
└─────────────────────────────────────┘
    ↓
[Match ID: 1, Estado: CREADO]


FASE 2: AGREGAR EQUIPOS (Exactamente 2)
═══════════════════════════════════════════════════════════════════════

POST /api/v1/matches/1/teams/1?esLocal=true
    ↓
┌─────────────────────────────────────┐
│ Agregar Equipo Local                │
│ - Validar no hay más de 2 equipos   │
│ - Crear MatchTeam                   │
└─────────────────────────────────────┘

POST /api/v1/matches/1/teams/2?esLocal=false
    ↓
┌─────────────────────────────────────┐
│ Agregar Equipo Visitante            │
│ - Validar no hay más de 2 equipos   │
│ - Crear MatchTeam                   │
└─────────────────────────────────────┘


FASE 3: JUGADORES SE UNEN
═══════════════════════════════════════════════════════════════════════

POST /api/v1/matches/join
{
  "matchId": 1,
  "playerUuid": "uuid-jugador",
  "teamId": 1,
  "positionId": 3,
  "role": "JUGADOR"
}
    ↓
┌─────────────────────────────────────┐
│ MatchService.joinMatch()            │
│ - Validar jugador no duplicado      │
│ - Validar equipo participa          │
│ - Crear MatchPlayer                 │
│ - Estado: confirmado=false          │
└─────────────────────────────────────┘

PUT /api/v1/matches/1/players/{uuid}/confirm
    ↓
┌─────────────────────────────────────┐
│ Confirmar participación             │
│ - confirmado=true                   │
└─────────────────────────────────────┘


FASE 4: INICIAR PARTIDO
═══════════════════════════════════════════════════════════════════════

PUT /api/v1/matches/1/status?status=INICIADO
    ↓
┌─────────────────────────────────────┐
│ Cambiar estado a INICIADO           │
│ - Registrar startedAt               │
│ - Validar transición válida         │
└─────────────────────────────────────┘
    ↓
[Match ID: 1, Estado: INICIADO]


FASE 5: REGISTRAR EVENTOS
═══════════════════════════════════════════════════════════════════════

POST /api/v1/matches/events
{
  "matchId": 1,
  "playerUuid": "uuid-goleador",
  "teamId": 1,
  "eventType": "GOL",
  "assistPlayerUuid": "uuid-asistente",
  "registeredByUuid": "uuid-registrador"
}
    ↓
┌─────────────────────────────────────┐
│ Crear MatchEvent                    │
│ - Tipo: GOL o ASISTENCIA            │
│ - Estado: pendiente confirmación    │
└─────────────────────────────────────┘

PUT /api/v1/matches/events/1/confirm?confirmingPlayerUuid={uuid}&isLocalTeam=true
    ↓
┌─────────────────────────────────────┐
│ Confirmar por equipo local          │
│ - confirmedByHome=true              │
└─────────────────────────────────────┘

PUT /api/v1/matches/events/1/confirm?confirmingPlayerUuid={uuid}&isLocalTeam=false
    ↓
┌─────────────────────────────────────┐
│ Confirmar por equipo visitante      │
│ - confirmedByAway=true              │
│ - Si ambos confirmados:             │
│   actualizar goles del equipo       │
└─────────────────────────────────────┘


FASE 6: FINALIZAR PARTIDO (¡CLAVE!)
═══════════════════════════════════════════════════════════════════════

PUT /api/v1/matches/1/status?status=FINALIZADO
    ↓
┌─────────────────────────────────────────────────────────────────┐
│ MatchService.changeMatchStatus()                                │
│ - Cambiar estado a FINALIZADO                                   │
│ - TRIGGER AUTOMÁTICO: updatePlayerRatingsAfterMatch()           │
└─────────────────────────────────────────────────────────────────┘
    ↓
    ↓ [ACTUALIZACIÓN AUTOMÁTICA DE CALIFICACIONES]
    ↓
    └──→ Ver siguiente sección: "Flujo de Actualización de Calificaciones"

```


---

## Flujo de Actualización de Calificaciones

### Diagrama Detallado del Proceso Automático

```
┌──────────────────────────────────────────────────────────────────────┐
│         ACTUALIZACIÓN AUTOMÁTICA AL FINALIZAR PARTIDO                │
└──────────────────────────────────────────────────────────────────────┘

TRIGGER: Partido cambia a estado FINALIZADO
    ↓
┌─────────────────────────────────────────────────────────────────────┐
│ MatchService.updatePlayerRatingsAfterMatch(match)                   │
└─────────────────────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────────────────────┐
│ PASO 1: Obtener equipos del partido                                 │
│ - Validar que hay exactamente 2 equipos                             │
│ - Obtener goles de cada equipo                                      │
└─────────────────────────────────────────────────────────────────────┘
    ↓
    Equipo Local: 5 goles
    Equipo Visitante: 3 goles
    ↓
┌─────────────────────────────────────────────────────────────────────┐
│ PASO 2: Determinar resultado del partido                            │
│ - Comparar goles local vs visitante                                 │
│ - Asignar resultado por equipo                                      │
└─────────────────────────────────────────────────────────────────────┘
    ↓
    Equipo Local: GANADO
    Equipo Visitante: PERDIDO
    ↓
┌─────────────────────────────────────────────────────────────────────┐
│ PASO 3: Obtener todos los jugadores confirmados                     │
│ - Filtrar solo jugadores con confirmado=true                        │
└─────────────────────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────────────────────┐
│ PASO 4: Obtener eventos del partido                                 │
│ - Goles confirmados por ambos equipos                               │
│ - Asistencias confirmadas                                           │
└─────────────────────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────────────────────┐
│ PASO 5: Para cada jugador, recopilar datos de rendimiento           │
│                                                                      │
│ collectPlayerPerformance(jugador, eventos, resultado, equipos)      │
│   ├─ Contar goles del jugador                                       │
│   ├─ Contar asistencias del jugador                                 │
│   ├─ Determinar si fue MVP (más goles + asistencias)                │
│   ├─ Mapear posición a rol (RoleType)                               │
│   ├─ Determinar nivel de prioridad                                  │
│   └─ Calcular goles recibidos (para defensas/arqueros)              │
└─────────────────────────────────────────────────────────────────────┘
    ↓
    Lista de PlayerPerformanceDto creada
    ↓
┌─────────────────────────────────────────────────────────────────────┐
│ PASO 6: Enviar al servicio de calificaciones                        │
│                                                                      │
│ RatingService.updatePlayerRatings(matchId, performanceData)         │
└─────────────────────────────────────────────────────────────────────┘


### Proceso Interno del RatingService

```
┌──────────────────────────────────────────────────────────────────────┐
│ RatingService.updatePlayerRatings(matchId, performances)            │
└──────────────────────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────────────────────┐
│ VALIDACIONES                                                         │
│ - matchId no nulo                                                    │
│ - performances no vacío                                              │
│ - Solo un MVP en el partido                                          │
│ - Cada performance tiene datos completos                             │
└─────────────────────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────────────────────┐
│ Para cada jugador:                                                   │
│   processPlayerRatingUpdate(match, performance)                      │
└─────────────────────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────────────────────┐
│ PASO A: Obtener o crear PlayerRating                                │
│ - Buscar por: playerUuid + roleType + priorityLevel                 │
│ - Si no existe: crear con calificación base según prioridad         │
└─────────────────────────────────────────────────────────────────────┘
    ↓
    Ejemplo: PlayerRating encontrado
    - Calificación actual: 75.50
    - Rol: ATAQUE
    - Prioridad: PRINCIPAL
    - Partidos jugados: 10
    ↓
┌─────────────────────────────────────────────────────────────────────┐
│ PASO B: Preparar solicitud de cálculo                               │
│                                                                      │
│ RatingCalculationRequest:                                            │
│   - currentRating: 75.50                                             │
│   - roleType: ATAQUE                                                 │
│   - priorityLevel: PRINCIPAL                                         │
│   - matchResult: GANADO                                              │
│   - goalsScored: 2                                                   │
│   - assistsMade: 1                                                   │
│   - goalsConceded: null (no aplica para ATAQUE)                      │
│   - wasMvp: true                                                     │
└─────────────────────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────────────────────┐
│ PASO C: Calcular nueva calificación                                 │
│                                                                      │
│ RatingCalculationEngine.calculateNewRating(request)                 │
└─────────────────────────────────────────────────────────────────────┘
    ↓
    [Ver sección siguiente: Cálculo Detallado]
    ↓
    Nueva calificación: 81.10
    Delta: +5.60
    ↓
┌─────────────────────────────────────────────────────────────────────┐
│ PASO D: Actualizar PlayerRating                                     │
│ - Guardar nueva calificación                                        │
│ - Incrementar contador de partidos                                  │
│ - Actualizar timestamp                                              │
│ - Manejo de concurrencia (OptimisticLocking)                        │
└─────────────────────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────────────────────┐
│ PASO E: Crear registro en RatingHistory                             │
│ - Guardar todos los detalles del cálculo                            │
│ - Componentes individuales (goles, asistencias, bonos)              │
│ - Para auditoría y análisis posterior                               │
└─────────────────────────────────────────────────────────────────────┘

```


### Motor de Cálculo - Paso a Paso

```
┌──────────────────────────────────────────────────────────────────────┐
│ RatingCalculationEngine.calculateNewRating()                        │
└──────────────────────────────────────────────────────────────────────┘

ENTRADA:
  currentRating: 75.50
  roleType: ATAQUE
  priorityLevel: PRINCIPAL
  matchResult: GANADO
  goalsScored: 2
  assistsMade: 1
  wasMvp: true

═══════════════════════════════════════════════════════════════════════
PASO 1: CALCULAR DELTA BASE
═══════════════════════════════════════════════════════════════════════

1.1 Puntos de Resultado
    matchResult = GANADO
    resultPoints = 2.0
    delta = 0 + 2.0 = 2.0

1.2 Goles Ponderados
    goalsScored = 2
    roleType = ATAQUE → goalWeight = 1.0
    weightedGoals = 2 × 1.0 = 2.0
    delta = 2.0 + 2.0 = 4.0

1.3 Asistencias Ponderadas
    assistsMade = 1
    roleType = ATAQUE → assistWeight = 0.6
    weightedAssists = 1 × 0.6 = 0.6
    delta = 4.0 + 0.6 = 4.6

1.4 Bono Defensivo
    roleType = ATAQUE → no aplica
    defensiveBonus = 0.0
    delta = 4.6 + 0.0 = 4.6

1.5 Bono MVP
    wasMvp = true
    mvpBonus = 1.0
    delta = 4.6 + 1.0 = 5.6

DELTA BASE = 5.6

═══════════════════════════════════════════════════════════════════════
PASO 2: APLICAR MULTIPLICADOR DE PRIORIDAD
═══════════════════════════════════════════════════════════════════════

    priorityLevel = PRINCIPAL → multiplier = 1.0
    adjustedDelta = 5.6 × 1.0 = 5.60

═══════════════════════════════════════════════════════════════════════
PASO 3: SUMAR A CALIFICACIÓN ACTUAL
═══════════════════════════════════════════════════════════════════════

    newRating = 75.50 + 5.60 = 81.10

═══════════════════════════════════════════════════════════════════════
PASO 4: APLICAR LÍMITE MÍNIMO
═══════════════════════════════════════════════════════════════════════

    priorityLevel = PRINCIPAL → baseRating = 70
    newRating (81.10) >= baseRating (70) ✓
    
    finalRating = 81.10

SALIDA: 81.10
```


---

## Ejemplos Prácticos con Cálculos

### Ejemplo 1: Delantero Estrella (Partido Ganado)

**Contexto:**
- Jugador: Carlos "El Goleador"
- Rol: ATAQUE
- Prioridad: PRINCIPAL
- Calificación actual: 80.00
- Resultado del partido: GANADO (5-3)

**Rendimiento:**
- Goles anotados: 3
- Asistencias: 1
- MVP: Sí
- Goles recibidos: N/A

**Cálculo:**

```
PASO 1: Delta Base
  Puntos resultado:     GANADO = +2.0
  Goles ponderados:     3 × 1.0 (ATAQUE) = +3.0
  Asistencias ponderadas: 1 × 0.6 (ATAQUE) = +0.6
  Bono defensivo:       N/A = 0.0
  Bono MVP:             Sí = +1.0
  ─────────────────────────────────────
  Delta base:           6.6

PASO 2: Multiplicador
  6.6 × 1.0 (PRINCIPAL) = 6.60

PASO 3: Nueva calificación
  80.00 + 6.60 = 86.60

PASO 4: Límite mínimo
  86.60 >= 70 ✓

RESULTADO FINAL: 86.60 (+6.60)
```

**Registro en RatingHistory:**
```json
{
  "previousRating": 80.00,
  "newRating": 86.60,
  "ratingDelta": 6.60,
  "goalsScored": 3,
  "assistsMade": 1,
  "wasMvp": true,
  "matchResult": "GANADO",
  "resultPoints": 2.0,
  "weightedGoalPoints": 3.0,
  "weightedAssistPoints": 0.6,
  "defensiveBonus": 0.0,
  "mvpBonus": 1.0,
  "priorityMultiplier": 1.0
}
```


### Ejemplo 2: Arquero Héroe (Valla Invicta)

**Contexto:**
- Jugador: Miguel "El Muro"
- Rol: ARQUERO
- Prioridad: PRINCIPAL
- Calificación actual: 72.00
- Resultado del partido: GANADO (3-0)

**Rendimiento:**
- Goles anotados: 0
- Asistencias: 0
- MVP: No
- Goles recibidos: 0 (¡Valla invicta!)

**Cálculo:**

```
PASO 1: Delta Base
  Puntos resultado:     GANADO = +2.0
  Goles ponderados:     0 × 0.1 (ARQUERO) = 0.0
  Asistencias ponderadas: 0 × 0.0 (ARQUERO) = 0.0
  Bono defensivo:       0 goles recibidos (ARQUERO) = +2.5
  Bono MVP:             No = 0.0
  ─────────────────────────────────────
  Delta base:           4.5

PASO 2: Multiplicador
  4.5 × 1.0 (PRINCIPAL) = 4.50

PASO 3: Nueva calificación
  72.00 + 4.50 = 76.50

PASO 4: Límite mínimo
  76.50 >= 70 ✓

RESULTADO FINAL: 76.50 (+4.50)
```

**Análisis:**
- El arquero recibe una buena bonificación por mantener la valla invicta (+2.5)
- Aunque no anotó ni asistió, su desempeño defensivo es recompensado
- El bono defensivo es crucial para arqueros y defensas


### Ejemplo 3: Mediocampista Creativo (Partido Empatado)

**Contexto:**
- Jugador: Ana "La Maestra"
- Rol: MEDIOCAMPO
- Prioridad: PRINCIPAL
- Calificación actual: 78.00
- Resultado del partido: EMPATE (2-2)

**Rendimiento:**
- Goles anotados: 1
- Asistencias: 2
- MVP: Sí (por asistencias)
- Goles recibidos: N/A

**Cálculo:**

```
PASO 1: Delta Base
  Puntos resultado:     EMPATE = +0.5
  Goles ponderados:     1 × 0.6 (MEDIOCAMPO) = +0.6
  Asistencias ponderadas: 2 × 1.0 (MEDIOCAMPO) = +2.0
  Bono defensivo:       N/A = 0.0
  Bono MVP:             Sí = +1.0
  ─────────────────────────────────────
  Delta base:           4.1

PASO 2: Multiplicador
  4.1 × 1.0 (PRINCIPAL) = 4.10

PASO 3: Nueva calificación
  78.00 + 4.10 = 82.10

PASO 4: Límite mínimo
  82.10 >= 70 ✓

RESULTADO FINAL: 82.10 (+4.10)
```

**Análisis:**
- El mediocampista tiene peso máximo (1.0) en asistencias
- Aunque el partido fue empate, las asistencias compensan
- El rol de mediocampo favorece el juego de creación


### Ejemplo 4: Defensa en Partido Perdido (Muchos Goles Recibidos)

**Contexto:**
- Jugador: Roberto "El Muro Roto"
- Rol: DEFENSA
- Prioridad: PRINCIPAL
- Calificación actual: 68.00
- Resultado del partido: PERDIDO (1-5)

**Rendimiento:**
- Goles anotados: 0
- Asistencias: 0
- MVP: No
- Goles recibidos: 5

**Cálculo:**

```
PASO 1: Delta Base
  Puntos resultado:     PERDIDO = -1.5
  Goles ponderados:     0 × 0.3 (DEFENSA) = 0.0
  Asistencias ponderadas: 0 × 0.3 (DEFENSA) = 0.0
  Bono defensivo:       5 goles recibidos (DEFENSA) = 0.0
  Bono MVP:             No = 0.0
  ─────────────────────────────────────
  Delta base:           -1.5

PASO 2: Multiplicador
  -1.5 × 1.0 (PRINCIPAL) = -1.50

PASO 3: Nueva calificación
  68.00 + (-1.50) = 66.50

PASO 4: Límite mínimo
  66.50 < 70 ✗
  Aplicar límite: 70.00

RESULTADO FINAL: 70.00 (+2.00)
```

**Análisis:**
- El jugador pierde puntos por la derrota (-1.5)
- No recibe bono defensivo (5 goles recibidos)
- La calificación caería a 66.50, pero el límite mínimo la protege
- El límite mínimo de PRINCIPAL (70) evita caídas drásticas


### Ejemplo 5: Comparación de Prioridades (Mismo Rendimiento)

**Contexto:**
Tres jugadores con el mismo rendimiento pero diferentes prioridades

**Rendimiento común:**
- Rol: ATAQUE
- Resultado: GANADO
- Goles: 2
- Asistencias: 1
- MVP: No

**Jugador A - Prioridad PRINCIPAL:**
```
Delta base: 2.0 + 2.0 + 0.6 + 0.0 + 0.0 = 4.6
Multiplicador: 4.6 × 1.0 = 4.60
Calificación: 75.00 → 79.60 (+4.60)
Límite mínimo: 70
```

**Jugador B - Prioridad SECUNDARIA:**
```
Delta base: 2.0 + 2.0 + 0.6 + 0.0 + 0.0 = 4.6
Multiplicador: 4.6 × 0.7 = 3.22
Calificación: 65.00 → 68.22 (+3.22)
Límite mínimo: 60
```

**Jugador C - Prioridad TERCIARIA:**
```
Delta base: 2.0 + 2.0 + 0.6 + 0.0 + 0.0 = 4.6
Multiplicador: 4.6 × 0.4 = 1.84
Calificación: 55.00 → 56.84 (+1.84)
Límite mínimo: 50
```

**Comparación Visual:**

```
Incremento de Calificación por Prioridad
(Mismo rendimiento)

PRINCIPAL    ████████████████████ +4.60 (100%)
SECUNDARIA   ██████████████       +3.22 (70%)
TERCIARIA    ████████             +1.84 (40%)
```

**Análisis:**
- El multiplicador de prioridad afecta significativamente la ganancia
- Jugadores con prioridad PRINCIPAL progresan más rápido
- El sistema incentiva jugar en posiciones prioritarias


### Ejemplo 6: Modo Arquero Rotativo

**Contexto:**
En partidos con modo arquero rotativo, todos los jugadores rotan en la posición de arquero. El sistema calcula calificaciones de arquero para todos.

**Partido:**
- Resultado: GANADO
- Todos los jugadores participan como arquero

**Jugador con Prioridad PRINCIPAL en Arquero:**
```
Calificación actual de arquero: 65.00
Resultado: GANADO
Puntos arquero rotativo: +1.5 (diferente a puntos normales)

Cálculo:
  Delta = 1.5 (solo puntos de resultado)
  Multiplicador: 1.5 × 1.0 = 1.50
  Nueva calificación: 65.00 + 1.50 = 66.50
  Límite mínimo: 70
  
RESULTADO FINAL: 70.00 (+5.00)
```

**Jugador con Prioridad SECUNDARIA en Arquero:**
```
Calificación actual de arquero: 58.00
Resultado: GANADO
Puntos arquero rotativo: +1.5

Cálculo:
  Delta = 1.5
  Multiplicador: 1.5 × 0.7 = 1.05
  Nueva calificación: 58.00 + 1.05 = 59.05
  Límite mínimo: 60
  
RESULTADO FINAL: 60.00 (+2.00)
```

**Características del Modo Rotativo:**
- Solo se aplican puntos de resultado (modificados)
- No se consideran goles, asistencias ni bonos
- Todos los jugadores actualizan su calificación de ARQUERO
- Puntos reducidos vs modo normal (1.5 vs 2.0 para GANADO)


---

## Diagrama de Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────────────┐
│                        ARQUITECTURA GENERAL                          │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────┐
│   Cliente    │
│  (Frontend)  │
└──────┬───────┘
       │ HTTP/REST
       ↓
┌──────────────────────────────────────────────────────────────────────┐
│                         CAPA DE CONTROLADORES                         │
├──────────────────────────────────────────────────────────────────────┤
│  AthleteController  │  MatchController  │  RatingController          │
│  TeamController     │  PlayerProfileController                       │
└──────────────────────────────────────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────────────────────────────┐
│                         CAPA DE SERVICIOS                             │
├──────────────────────────────────────────────────────────────────────┤
│  AthleteService     │  MatchService     │  RatingService             │
│  TeamService        │  PlayerProfileService                          │
│                     │  RatingCalculationEngine                       │
└──────────────────────────────────────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────────────────────────────┐
│                      CAPA DE REPOSITORIOS                             │
├──────────────────────────────────────────────────────────────────────┤
│  AthleteRepository  │  MatchRepository  │  PlayerRatingRepository    │
│  TeamRepository     │  MatchEventRepository                          │
│  PlayerProfileRepository  │  RatingHistoryRepository                 │
└──────────────────────────────────────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────────────────────────────┐
│                         BASE DE DATOS                                 │
│                      PostgreSQL 15+                                   │
├──────────────────────────────────────────────────────────────────────┤
│  Tablas principales:                                                  │
│  - athletes                                                           │
│  - player_profiles                                                    │
│  - matches                                                            │
│  - match_players                                                      │
│  - match_events                                                       │
│  - player_ratings                                                     │
│  - rating_history                                                     │
└──────────────────────────────────────────────────────────────────────┘
```


## Flujo de Datos: Calificación Completa

```
┌─────────────────────────────────────────────────────────────────────┐
│              FLUJO COMPLETO DE ACTUALIZACIÓN DE RATING               │
└─────────────────────────────────────────────────────────────────────┘

1. TRIGGER
   ┌──────────────────────────────────┐
   │ Partido finaliza                 │
   │ Estado: FINALIZADO               │
   └──────────────────────────────────┘
              ↓
2. RECOPILACIÓN
   ┌──────────────────────────────────┐
   │ MatchService                     │
   │ - Obtener equipos y resultado    │
   │ - Obtener jugadores confirmados  │
   │ - Obtener eventos del partido    │
   │ - Calcular estadísticas          │
   └──────────────────────────────────┘
              ↓
3. PREPARACIÓN
   ┌──────────────────────────────────┐
   │ PlayerPerformanceDto             │
   │ - playerUuid                     │
   │ - roleType                       │
   │ - priorityLevel                  │
   │ - goalsScored                    │
   │ - assistsMade                    │
   │ - goalsConceded                  │
   │ - wasMvp                         │
   │ - matchResult                    │
   └──────────────────────────────────┘
              ↓
4. VALIDACIÓN
   ┌──────────────────────────────────┐
   │ RatingService                    │
   │ - Validar datos completos        │
   │ - Validar solo un MVP            │
   │ - Validar partido existe         │
   └──────────────────────────────────┘
              ↓
5. OBTENCIÓN/CREACIÓN
   ┌──────────────────────────────────┐
   │ PlayerRating                     │
   │ - Buscar existente               │
   │ - O crear nuevo con base rating  │
   └──────────────────────────────────┘
              ↓
6. CÁLCULO
   ┌──────────────────────────────────┐
   │ RatingCalculationEngine          │
   │ - Calcular delta base            │
   │ - Aplicar multiplicador          │
   │ - Sumar a calificación actual    │
   │ - Aplicar límite mínimo          │
   └──────────────────────────────────┘
              ↓
7. PERSISTENCIA
   ┌──────────────────────────────────┐
   │ Base de Datos                    │
   │ - Actualizar player_ratings      │
   │ - Insertar rating_history        │
   │ - Control de concurrencia        │
   └──────────────────────────────────┘
              ↓
8. RESPUESTA
   ┌──────────────────────────────────┐
   │ Calificación actualizada         │
   │ Historial registrado             │
   └──────────────────────────────────┘
```


---

## Características Importantes del Sistema

### 1. Actualización Automática
- Las calificaciones se actualizan automáticamente cuando un partido finaliza
- No requiere intervención manual
- Proceso transaccional y atómico

### 2. Manejo de Concurrencia
- Usa OptimisticLocking para evitar conflictos
- Si dos procesos intentan actualizar la misma calificación simultáneamente:
  - El primero tiene éxito
  - El segundo recibe ConcurrentRatingUpdateException
  - Se puede reintentar la operación

### 3. Auditoría Completa
- Cada cambio de calificación se registra en `rating_history`
- Se guardan todos los componentes del cálculo:
  - Puntos de resultado
  - Goles ponderados
  - Asistencias ponderadas
  - Bonos defensivos
  - Bono MVP
  - Multiplicador de prioridad
- Permite análisis histórico y debugging

### 4. Protección con Límites Mínimos
- Cada prioridad tiene una calificación base mínima
- Evita caídas drásticas por malos resultados
- PRINCIPAL: mínimo 70
- SECUNDARIA: mínimo 60
- TERCIARIA: mínimo 50

### 5. Diferenciación por Rol
- Cada rol tiene pesos diferentes para goles y asistencias
- Delanteros: máximo peso en goles
- Mediocampistas: máximo peso en asistencias
- Defensas/Arqueros: bonos defensivos especiales

### 6. Sistema de Prioridades
- Permite que un jugador tenga múltiples calificaciones
- Ejemplo: Un jugador puede ser ATAQUE-PRINCIPAL y MEDIOCAMPO-SECUNDARIA
- Cada combinación rol-prioridad tiene su propia calificación
- Progresión más rápida en posiciones prioritarias


---

## Endpoints Clave para Calificaciones

### Consultar Calificaciones

```http
GET /api/v1/ratings/player/{playerProfileId}
Authorization: Bearer <token>
```
Retorna todas las calificaciones del jugador (todos los roles y prioridades)

```http
GET /api/v1/ratings/player/{playerProfileId}/role/{roleType}
Authorization: Bearer <token>
```
Filtra por rol específico (ATAQUE, DEFENSA, MEDIOCAMPO, etc.)

```http
GET /api/v1/ratings/player/{playerProfileId}/priority/{priorityLevel}
Authorization: Bearer <token>
```
Filtra por nivel de prioridad (PRINCIPAL, SECUNDARIA, TERCIARIA)

### Consultar Historial

```http
GET /api/v1/ratings/player/{playerProfileId}/history
Authorization: Bearer <token>
```
Retorna historial completo de cambios de calificación

```http
GET /api/v1/ratings/player/{playerProfileId}/history/period?startDate={fecha}&endDate={fecha}
Authorization: Bearer <token>
```
Filtra historial por período de tiempo

### Actualización Manual (Opcional)

```http
POST /api/v1/ratings/update
Authorization: Bearer <token>
Content-Type: application/json

{
  "matchId": 1,
  "performances": [
    {
      "playerProfileId": "uuid",
      "roleType": "ATAQUE",
      "priorityLevel": "PRINCIPAL",
      "goalsScored": 2,
      "assistsMade": 1,
      "goalsConceded": null,
      "wasMvp": true,
      "matchResult": "GANADO"
    }
  ]
}
```

Permite actualizar calificaciones manualmente (útil para correcciones o procesamiento batch)


---

## Resumen Ejecutivo

### ¿Cómo funciona el sistema?

1. **Los jugadores se registran** y crean perfiles
2. **Se crean partidos** con equipos y jugadores
3. **Durante el partido** se registran eventos (goles, asistencias)
4. **Al finalizar el partido** (estado FINALIZADO):
   - El sistema recopila automáticamente las estadísticas
   - Calcula el rendimiento de cada jugador
   - Actualiza las calificaciones usando el motor de cálculo
   - Registra todo en el historial para auditoría

### Fórmula Simplificada

```
NUEVA_CALIFICACIÓN = ACTUAL + (
  (RESULTADO + GOLES×PESO + ASISTENCIAS×PESO + BONO_DEFENSIVO + BONO_MVP)
  × MULTIPLICADOR_PRIORIDAD
)
```

### Factores que Aumentan la Calificación

✅ Ganar partidos (+2.0)
✅ Anotar goles (peso según rol)
✅ Dar asistencias (peso según rol)
✅ Ser MVP (+1.0)
✅ Mantener valla invicta (defensas/arqueros: +2.0 a +2.5)
✅ Tener prioridad PRINCIPAL (multiplicador 1.0)

### Factores que Disminuyen la Calificación

❌ Perder partidos (-1.5)
❌ Recibir muchos goles (sin bono defensivo)
❌ Tener prioridad baja (multiplicador 0.4 a 0.7)

### Protecciones del Sistema

🛡️ Límites mínimos por prioridad (50, 60, 70)
🛡️ Control de concurrencia (OptimisticLocking)
🛡️ Validación exhaustiva de datos
🛡️ Auditoría completa en historial
🛡️ Transacciones atómicas

---

## Conclusión

El sistema de calificaciones de jugadores es un componente sofisticado que:

- **Automatiza** la evaluación del rendimiento
- **Diferencia** entre roles y prioridades
- **Protege** contra caídas drásticas
- **Audita** todos los cambios
- **Escala** con manejo de concurrencia

El diseño modular permite ajustar fácilmente los pesos, bonos y multiplicadores según las necesidades del negocio, mientras mantiene la integridad y trazabilidad de todos los cálculos.

