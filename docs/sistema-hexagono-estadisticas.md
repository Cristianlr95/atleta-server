# Sistema de Hexágono de Estadísticas - Estilo Pokémon

## Concepto: Estrella de David / Hexágono de Habilidades

Similar al sistema de estadísticas de Pokémon, cada jugador tiene calificaciones en 6 roles diferentes que forman un hexágono visual.

---

## Los 6 Roles del Hexágono

```
                    ATAQUE (Delantero)
                           ⭐
                          /  \
                         /    \
                        /      \
         CARRILERO ⭐ ─────────── ⭐ MEDIOCAMPO
        (Lateral)    \          /   (Creativo)
                      \        /
                       \      /
                        \    /
                         \  /
                          ⭐
                      DEFENSA
                          |
                          |
         ARQUERO ⭐ ──────┴────── ⭐ DT
        (Portero)              (Director Técnico)
```

### Distribución de Roles

| Rol | Posición | Especialidad | Peso Goles | Peso Asistencias |
|-----|----------|--------------|------------|------------------|
| **ATAQUE** | Delantero | Anotación | 1.0 (100%) | 0.6 (60%) |
| **MEDIOCAMPO** | Centro | Creación | 0.6 (60%) | 1.0 (100%) |
| **CARRILERO** | Lateral | Balance | 0.5 (50%) | 0.7 (70%) |
| **DEFENSA** | Defensor | Protección | 0.3 (30%) | 0.3 (30%) |
| **ARQUERO** | Portero | Última línea | 0.1 (10%) | 0.0 (0%) |
| **DT** | Entrenador | Liderazgo | 0.2 (20%) | 0.2 (20%) |

---

## Ejemplo Visual: Perfil de Jugador Completo

### Jugador: "El Todoterreno"

```
Calificaciones por Rol:
- ATAQUE: 85
- MEDIOCAMPO: 78
- CARRILERO: 72
- DEFENSA: 65
- ARQUERO: 55
- DT: 60
```

### Hexágono de Estadísticas

```
                    ATAQUE (85)
                         ⭐
                        /│\
                       / │ \
                      /  │  \
                     /   │   \
                    /    │    \
                   /     │     \
        CARRILERO ⭐──────┼──────⭐ MEDIOCAMPO
           (72)    \     │     /    (78)
                    \    │    /
                     \   │   /
                      \  │  /
                       \ │ /
                        \│/
                         ⭐
                    DEFENSA (65)
                         │
                         │
        ARQUERO (55) ────┴──── DT (60)
             ⭐                  ⭐


Representación Gráfica (0-100):

ATAQUE      ████████████████████████████████████████████ 85
MEDIOCAMPO  ███████████████████████████████████████      78
CARRILERO   ████████████████████████████████             72
DEFENSA     █████████████████████████████                65
DT          ██████████████████████████                   60
ARQUERO     ███████████████████████                      55
```


---

## Perfiles de Jugador Típicos

### 1. El Goleador Nato

```
Especialización: ATAQUE

                    ATAQUE (92) ⭐⭐⭐
                         ⭐
                        /│\
                       / │ \
                      /  │  \
        CARRILERO ⭐──────┼──────⭐ MEDIOCAMPO
           (68)    \     │     /    (75)
                    \    │    /
                     \   │   /
                      \  │  /
                       \ │ /
                        \│/
                         ⭐
                    DEFENSA (58)
                         │
        ARQUERO (50) ────┴──── DT (55)

Estadísticas:
ATAQUE      ██████████████████████████████████████████████████ 92 ⭐⭐⭐
MEDIOCAMPO  ███████████████████████████████████                75
CARRILERO   ██████████████████████████████                     68
DEFENSA     █████████████████████████                          58
DT          ███████████████████████                            55
ARQUERO     █████████████████████                              50

Fortalezas: Finalización, posicionamiento ofensivo
Debilidades: Defensa, arquería
```

### 2. El Cerebro del Equipo

```
Especialización: MEDIOCAMPO

                    ATAQUE (72)
                         ⭐
                        /│\
                       / │ \
                      /  │  \
        CARRILERO ⭐──────┼──────⭐ MEDIOCAMPO (90) ⭐⭐⭐
           (80)    \     │     /
                    \    │    /
                     \   │   /
                      \  │  /
                       \ │ /
                        \│/
                         ⭐
                    DEFENSA (70)
                         │
        ARQUERO (52) ────┴──── DT (75)

Estadísticas:
MEDIOCAMPO  ████████████████████████████████████████████████ 90 ⭐⭐⭐
CARRILERO   ████████████████████████████████████████         80
DT          ███████████████████████████████████              75
ATAQUE      ████████████████████████████████                 72
DEFENSA     ██████████████████████████████                   70
ARQUERO     ██████████████████████                           52

Fortalezas: Visión de juego, pases, asistencias
Debilidades: Arquería
```

### 3. El Muro Defensivo

```
Especialización: DEFENSA

                    ATAQUE (60)
                         ⭐
                        /│\
                       / │ \
                      /  │  \
        CARRILERO ⭐──────┼──────⭐ MEDIOCAMPO (68)
           (75)    \     │     /
                    \    │    /
                     \   │   /
                      \  │  /
                       \ │ /
                        \│/
                         ⭐
                    DEFENSA (88) ⭐⭐⭐
                         │
        ARQUERO (78) ────┴──── DT (65)

Estadísticas:
DEFENSA     ██████████████████████████████████████████████ 88 ⭐⭐⭐
ARQUERO     ███████████████████████████████████████        78
CARRILERO   ███████████████████████████████████            75
MEDIOCAMPO  ██████████████████████████████                 68
DT          █████████████████████████████                  65
ATAQUE      ████████████████████████                       60

Fortalezas: Marcaje, anticipación, juego aéreo
Debilidades: Finalización
```


### 4. El Guardameta Élite

```
Especialización: ARQUERO

                    ATAQUE (52)
                         ⭐
                        /│\
                       / │ \
                      /  │  \
        CARRILERO ⭐──────┼──────⭐ MEDIOCAMPO (58)
           (62)    \     │     /
                    \    │    /
                     \   │   /
                      \  │  /
                       \ │ /
                        \│/
                         ⭐
                    DEFENSA (82)
                         │
        ARQUERO (95) ────┴──── DT (70) ⭐⭐⭐

Estadísticas:
ARQUERO     ████████████████████████████████████████████████████ 95 ⭐⭐⭐
DEFENSA     ██████████████████████████████████████████           82
DT          ██████████████████████████████████                   70
CARRILERO   ████████████████████████                             62
MEDIOCAMPO  █████████████████████████                            58
ATAQUE      ██████████████████████                               52

Fortalezas: Reflejos, posicionamiento bajo palos, liderazgo defensivo
Debilidades: Juego ofensivo
```

### 5. El Todoterreno Balanceado

```
Especialización: EQUILIBRADO

                    ATAQUE (75)
                         ⭐
                        /│\
                       / │ \
                      /  │  \
        CARRILERO ⭐──────┼──────⭐ MEDIOCAMPO (78)
           (80)    \     │     /
                    \    │    /
                     \   │   /
                      \  │  /
                       \ │ /
                        \│/
                         ⭐
                    DEFENSA (72)
                         │
        ARQUERO (65) ────┴──── DT (70)

Estadísticas:
CARRILERO   ████████████████████████████████████████         80
MEDIOCAMPO  ███████████████████████████████████████          78
ATAQUE      ███████████████████████████████████              75
DEFENSA     ████████████████████████████████                 72
DT          ██████████████████████████████                   70
ARQUERO     █████████████████████████████                    65

Fortalezas: Versatilidad, adaptabilidad
Debilidades: Ninguna especialización dominante
```

### 6. El Estratega (Director Técnico)

```
Especialización: DT

                    ATAQUE (70)
                         ⭐
                        /│\
                       / │ \
                      /  │  \
        CARRILERO ⭐──────┼──────⭐ MEDIOCAMPO (82)
           (78)    \     │     /
                    \    │    /
                     \   │   /
                      \  │  /
                       \ │ /
                        \│/
                         ⭐
                    DEFENSA (75)
                         │
        ARQUERO (68) ────┴──── DT (92) ⭐⭐⭐

Estadísticas:
DT          ████████████████████████████████████████████████ 92 ⭐⭐⭐
MEDIOCAMPO  ██████████████████████████████████████████       82
CARRILERO   ███████████████████████████████████████          78
DEFENSA     ███████████████████████████████████              75
ATAQUE      ██████████████████████████████                   70
ARQUERO     ██████████████████████████████                   68

Fortalezas: Liderazgo, táctica, lectura del juego
Debilidades: Ejecución física
```


---

## Cómo Evolucionan las Estadísticas

### Sistema de Prioridades y Evolución

Cada jugador puede tener hasta 3 prioridades por rol:

```
Ejemplo: Jugador versátil con múltiples roles

ROL: ATAQUE
├─ Prioridad PRINCIPAL:   85 (progresa rápido, multiplicador 1.0)
├─ Prioridad SECUNDARIA:  -- (no asignada)
└─ Prioridad TERCIARIA:   -- (no asignada)

ROL: MEDIOCAMPO
├─ Prioridad PRINCIPAL:   -- (no asignada)
├─ Prioridad SECUNDARIA:  72 (progresa medio, multiplicador 0.7)
└─ Prioridad TERCIARIA:   -- (no asignada)

ROL: CARRILERO
├─ Prioridad PRINCIPAL:   -- (no asignada)
├─ Prioridad SECUNDARIA:  -- (no asignada)
└─ Prioridad TERCIARIA:   65 (progresa lento, multiplicador 0.4)
```

### Velocidad de Progresión

```
Mismo rendimiento en un partido ganado (2 goles, 1 asistencia, MVP):

PRIORIDAD PRINCIPAL (multiplicador 1.0)
Antes:  75.00
Delta:  +5.60
Después: 80.60 ████████████████████ +5.60

PRIORIDAD SECUNDARIA (multiplicador 0.7)
Antes:  75.00
Delta:  +3.92
Después: 78.92 ██████████████ +3.92

PRIORIDAD TERCIARIA (multiplicador 0.4)
Antes:  75.00
Delta:  +2.24
Después: 77.24 ████████ +2.24
```

---

## Factores que Modifican el Hexágono

### ✅ Factores que AUMENTAN las estadísticas

| Factor | Impacto | Roles Beneficiados |
|--------|---------|-------------------|
| **Ganar partidos** | +2.0 puntos base | Todos |
| **Anotar goles** | Variable por rol | ATAQUE (máximo), MEDIOCAMPO, CARRILERO |
| **Dar asistencias** | Variable por rol | MEDIOCAMPO (máximo), CARRILERO, ATAQUE |
| **Ser MVP** | +1.0 punto | Todos |
| **Valla invicta** | +2.5 puntos | ARQUERO (máximo), DEFENSA |
| **Pocos goles recibidos** | +0.7 a +2.0 | ARQUERO, DEFENSA |
| **Prioridad alta** | Multiplicador 1.0 | El rol asignado |

### ❌ Factores que DISMINUYEN las estadísticas

| Factor | Impacto | Roles Afectados |
|--------|---------|-----------------|
| **Perder partidos** | -1.5 puntos base | Todos |
| **Muchos goles recibidos** | Sin bono (0.0) | ARQUERO, DEFENSA |
| **Prioridad baja** | Multiplicador 0.4 | El rol asignado |
| **No participar** | Sin cambio | Todos |


---

## Ejemplo de Evolución: Carrera de un Jugador

### Temporada 1: El Inicio

```
Jugador: "Promesa Joven"
Especialización inicial: ATAQUE (Prioridad PRINCIPAL)

                    ATAQUE (70) ⭐
                         ⭐
                        /│\
                       / │ \
        CARRILERO ⭐──────┼──────⭐ MEDIOCAMPO (60)
           (60)    \     │     /
                    \    │    /
                     \   │   /
                      \  │  /
                       \ │ /
                        \│/
                         ⭐
                    DEFENSA (50)
                         │
        ARQUERO (50) ────┴──── DT (50)

Perfil: Delantero principiante con calificaciones base
```

### Temporada 2: Desarrollo (Después de 20 partidos)

```
Jugador: "Promesa Joven"
Evolución: +15 puntos en ATAQUE, desarrollo en otros roles

                    ATAQUE (85) ⭐⭐
                         ⭐
                        /│\
                       / │ \
        CARRILERO ⭐──────┼──────⭐ MEDIOCAMPO (68)
           (65)    \     │     /
                    \    │    /
                     \   │   /
                      \  │  /
                       \ │ /
                        \│/
                         ⭐
                    DEFENSA (52)
                         │
        ARQUERO (50) ────┴──── DT (55)

Cambios:
ATAQUE:     70 → 85  (+15) ████████████████
MEDIOCAMPO: 60 → 68  (+8)  ████████
CARRILERO:  60 → 65  (+5)  █████
DT:         50 → 55  (+5)  █████
DEFENSA:    50 → 52  (+2)  ██
ARQUERO:    50 → 50  (0)   
```

### Temporada 3: Consolidación (Después de 50 partidos)

```
Jugador: "Goleador Consolidado"
Evolución: Especialización clara, versatilidad mejorada

                    ATAQUE (92) ⭐⭐⭐
                         ⭐
                        /│\
                       / │ \
        CARRILERO ⭐──────┼──────⭐ MEDIOCAMPO (78)
           (72)    \     │     /
                    \    │    /
                     \   │   /
                      \  │  /
                       \ │ /
                        \│/
                         ⭐
                    DEFENSA (58)
                         │
        ARQUERO (52) ────┴──── DT (62)

Cambios totales desde inicio:
ATAQUE:     70 → 92  (+22) ██████████████████████
MEDIOCAMPO: 60 → 78  (+18) ██████████████████
CARRILERO:  60 → 72  (+12) ████████████
DT:         50 → 62  (+12) ████████████
DEFENSA:    50 → 58  (+8)  ████████
ARQUERO:    50 → 52  (+2)  ██

Nivel alcanzado: ÉLITE en ATAQUE
```


---

## Sistema de Clasificación por Nivel

### Rangos de Calificación

```
🏆 LEYENDA      95-100  ████████████████████████████████████████████████████
⭐⭐⭐ ÉLITE     85-94   ████████████████████████████████████████████
⭐⭐ EXPERTO     75-84   ████████████████████████████████████
⭐ AVANZADO      65-74   ████████████████████████████
  INTERMEDIO    55-64   ████████████████████
  PRINCIPIANTE  50-54   ██████████████
  NOVATO        0-49    ██████
```

### Ejemplo de Clasificación Completa

```
Jugador: "El Fenómeno"

ATAQUE      ████████████████████████████████████████████████ 92 ⭐⭐⭐ ÉLITE
MEDIOCAMPO  ███████████████████████████████████████          82 ⭐⭐ EXPERTO
CARRILERO   ████████████████████████████████                 72 ⭐ AVANZADO
DEFENSA     █████████████████████████████                    65 ⭐ AVANZADO
DT          ██████████████████████████                       60   INTERMEDIO
ARQUERO     ███████████████████████                          55   INTERMEDIO

Clasificación General: ÉLITE (basado en rol principal)
Versatilidad: ALTA (4 roles sobre 65 puntos)
```

---

## Comparación de Jugadores

### Duelo: Goleador vs Todoterreno

```
JUGADOR A: "El Goleador"          JUGADOR B: "El Todoterreno"

ATAQUE      ████████████ 92       ATAQUE      ████████ 75
MEDIOCAMPO  ███████ 75             MEDIOCAMPO  ████████ 78
CARRILERO   ██████ 68              CARRILERO   ████████ 80
DEFENSA     █████ 58               DEFENSA     ███████ 72
DT          ████ 55                DT          ███████ 70
ARQUERO     ███ 50                 ARQUERO     ██████ 65

Promedio:   66.3                   Promedio:   73.3
Máximo:     92 (ATAQUE)            Máximo:     80 (CARRILERO)
Mínimo:     50 (ARQUERO)           Mínimo:     65 (ARQUERO)
Rango:      42 puntos              Rango:      15 puntos

Perfil A: Especialista ofensivo
Perfil B: Jugador completo y versátil
```

### Análisis Táctico

```
¿Cuándo elegir al Jugador A (Goleador)?
✅ Necesitas goles urgentemente
✅ Tienes un equipo defensivo sólido
✅ Juegas con un solo delantero
❌ Necesitas ayuda defensiva
❌ Requieres versatilidad posicional

¿Cuándo elegir al Jugador B (Todoterreno)?
✅ Necesitas cubrir múltiples posiciones
✅ Buscas equilibrio en el equipo
✅ Juegas con rotaciones frecuentes
✅ Requieres solidez en todas las líneas
❌ Necesitas un goleador nato
```


---

## Implementación en el Sistema

### Endpoint para Obtener Hexágono Completo

```http
GET /api/v1/ratings/player/{playerProfileId}
Authorization: Bearer <token>
```

**Respuesta:**
```json
{
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "alias": "El Fenómeno",
  "ratings": [
    {
      "roleType": "ATAQUE",
      "priorityLevel": "PRINCIPAL",
      "currentRating": 92.00,
      "matchesPlayed": 45,
      "classification": "ÉLITE"
    },
    {
      "roleType": "MEDIOCAMPO",
      "priorityLevel": "SECUNDARIA",
      "currentRating": 82.00,
      "matchesPlayed": 30,
      "classification": "EXPERTO"
    },
    {
      "roleType": "CARRILERO",
      "priorityLevel": "TERCIARIA",
      "currentRating": 72.00,
      "matchesPlayed": 15,
      "classification": "AVANZADO"
    },
    {
      "roleType": "DEFENSA",
      "priorityLevel": "TERCIARIA",
      "currentRating": 65.00,
      "matchesPlayed": 10,
      "classification": "AVANZADO"
    },
    {
      "roleType": "DT",
      "priorityLevel": "SECUNDARIA",
      "currentRating": 60.00,
      "matchesPlayed": 8,
      "classification": "INTERMEDIO"
    },
    {
      "roleType": "ARQUERO",
      "priorityLevel": "TERCIARIA",
      "currentRating": 55.00,
      "matchesPlayed": 5,
      "classification": "INTERMEDIO"
    }
  ],
  "statistics": {
    "averageRating": 71.0,
    "maxRating": 92.0,
    "minRating": 55.0,
    "totalMatches": 113,
    "versatilityScore": 0.75
  }
}
```

### Cálculo de Versatilidad

```
Versatilidad = (Número de roles > 65 puntos) / 6

Ejemplos:
- 6 roles > 65: Versatilidad = 1.00 (Máxima)
- 4 roles > 65: Versatilidad = 0.67 (Alta)
- 2 roles > 65: Versatilidad = 0.33 (Baja)
- 1 rol > 65:   Versatilidad = 0.17 (Especialista)
```


---

## Visualización Frontend (Sugerencia)

### Código HTML/CSS/JavaScript para Hexágono

```html
<!-- Ejemplo de implementación con Chart.js -->
<canvas id="playerHexagon"></canvas>

<script>
const ctx = document.getElementById('playerHexagon').getContext('2d');
const hexagonChart = new Chart(ctx, {
    type: 'radar',
    data: {
        labels: ['ATAQUE', 'MEDIOCAMPO', 'CARRILERO', 'DEFENSA', 'ARQUERO', 'DT'],
        datasets: [{
            label: 'El Fenómeno',
            data: [92, 82, 72, 65, 55, 60],
            backgroundColor: 'rgba(54, 162, 235, 0.2)',
            borderColor: 'rgba(54, 162, 235, 1)',
            borderWidth: 2,
            pointBackgroundColor: 'rgba(54, 162, 235, 1)',
            pointBorderColor: '#fff',
            pointHoverBackgroundColor: '#fff',
            pointHoverBorderColor: 'rgba(54, 162, 235, 1)'
        }]
    },
    options: {
        scales: {
            r: {
                min: 0,
                max: 100,
                ticks: {
                    stepSize: 20
                }
            }
        },
        plugins: {
            legend: {
                display: true,
                position: 'top'
            }
        }
    }
});
</script>
```

### Colores Sugeridos por Clasificación

```css
/* Colores para niveles */
.leyenda    { color: #FFD700; } /* Dorado */
.elite      { color: #FF6B6B; } /* Rojo */
.experto    { color: #4ECDC4; } /* Turquesa */
.avanzado   { color: #95E1D3; } /* Verde agua */
.intermedio { color: #A8E6CF; } /* Verde claro */
.principiante { color: #DCEDC1; } /* Verde pálido */
.novato     { color: #C7CEEA; } /* Azul pálido */
```

---

## Resumen del Sistema Hexagonal

### Ventajas del Sistema

✅ **Visual e Intuitivo**: Fácil de entender de un vistazo
✅ **Completo**: Muestra todas las habilidades del jugador
✅ **Comparable**: Permite comparar jugadores fácilmente
✅ **Motivador**: Los jugadores ven su progreso claramente
✅ **Estratégico**: Ayuda a tomar decisiones tácticas
✅ **Flexible**: Permite especialización o versatilidad

### Cómo se Construye

1. **Cada jugador tiene 6 calificaciones** (una por rol)
2. **Cada calificación evoluciona independientemente** según el rendimiento
3. **Las prioridades determinan la velocidad** de progresión
4. **Los límites mínimos protegen** contra caídas drásticas
5. **El historial registra** toda la evolución

### Interpretación Rápida

```
Hexágono PUNTIAGUDO (una punta muy alta):
→ Especialista en un rol específico
→ Ideal para posiciones fijas
→ Ejemplo: Goleador nato, Arquero élite

Hexágono REDONDO (todas las puntas similares):
→ Jugador versátil y completo
→ Ideal para rotaciones
→ Ejemplo: Todoterreno, Comodín

Hexágono IRREGULAR (puntas variadas):
→ Jugador en desarrollo
→ Potencial de especialización
→ Ejemplo: Promesa joven
```

---

## Conclusión

El sistema de hexágono de estadísticas tipo Pokémon permite:

🎯 **Visualizar** el perfil completo de cada jugador
📊 **Comparar** jugadores de forma objetiva
🎮 **Gamificar** la experiencia del usuario
⚽ **Optimizar** la formación de equipos
📈 **Motivar** el desarrollo de habilidades
🏆 **Reconocer** la evolución y logros

Cada jugador es único, con su propio hexágono de habilidades que refleja su estilo de juego, experiencia y especialización.

