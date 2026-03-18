# Calificación General del Jugador (OVR - Overall Rating)

## Concepto: Valor Único por Jugador

Similar al sistema de FIFA/EA Sports, podemos calcular un **OVR (Overall Rating)** que represente la habilidad general del jugador considerando todos sus roles.

---

## Métodos de Cálculo del OVR

### Método 1: Promedio Simple

El más básico - promedio de todas las calificaciones del jugador.

```
OVR = (ATAQUE + MEDIOCAMPO + CARRILERO + DEFENSA + ARQUERO + DT) / 6
```

**Ejemplo:**
```
Jugador: "El Fenómeno"
- ATAQUE: 92
- MEDIOCAMPO: 82
- CARRILERO: 72
- DEFENSA: 65
- ARQUERO: 55
- DT: 60

OVR = (92 + 82 + 72 + 65 + 55 + 60) / 6
OVR = 426 / 6
OVR = 71.0
```

**Ventajas:** Simple, fácil de entender
**Desventajas:** No considera la especialización del jugador

---

### Método 2: Promedio Ponderado por Prioridad (RECOMENDADO)

Considera las prioridades del jugador - los roles principales pesan más.

```
OVR = (Σ(calificación × peso_prioridad)) / Σ(peso_prioridad)

Pesos:
- PRINCIPAL: 1.0
- SECUNDARIA: 0.7
- TERCIARIA: 0.4
- Sin asignar: 0.1 (peso mínimo)
```

**Ejemplo:**
```
Jugador: "El Goleador"

Rol          | Calificación | Prioridad   | Peso | Contribución
-------------|--------------|-------------|------|-------------
ATAQUE       | 92           | PRINCIPAL   | 1.0  | 92.0
MEDIOCAMPO   | 75           | SECUNDARIA  | 0.7  | 52.5
CARRILERO    | 68           | TERCIARIA   | 0.4  | 27.2
DEFENSA      | 58           | Sin asignar | 0.1  | 5.8
ARQUERO      | 50           | Sin asignar | 0.1  | 5.0
DT           | 55           | Sin asignar | 0.1  | 5.5

Suma de contribuciones: 188.0
Suma de pesos: 2.3

OVR = 188.0 / 2.3 = 81.7
```

**Ventajas:** Refleja la especialización del jugador
**Desventajas:** Requiere conocer las prioridades

---

### Método 3: Top 3 Roles (Estilo FIFA)

Solo considera los 3 mejores roles del jugador.

```
OVR = (Rol_Mejor + Rol_Segundo + Rol_Tercero) / 3
```

**Ejemplo:**
```
Jugador: "El Fenómeno"
Calificaciones ordenadas:
1. ATAQUE: 92
2. MEDIOCAMPO: 82
3. CARRILERO: 72
4. DEFENSA: 65
5. DT: 60
6. ARQUERO: 55

OVR = (92 + 82 + 72) / 3
OVR = 246 / 3
OVR = 82.0
```

**Ventajas:** Ignora debilidades, enfoca fortalezas
**Desventajas:** Puede sobrevalorar especialistas


### Método 4: Ponderado por Posición Principal

Considera la posición principal del jugador y pondera según relevancia.

```
OVR = (Rol_Principal × 0.5) + (Otros_Roles × 0.5)

Donde:
- Rol_Principal: La calificación más alta del jugador
- Otros_Roles: Promedio de los demás roles
```

**Ejemplo:**
```
Jugador: "El Goleador"
Rol Principal: ATAQUE (92)
Otros roles: (75 + 68 + 58 + 50 + 55) / 5 = 61.2

OVR = (92 × 0.5) + (61.2 × 0.5)
OVR = 46.0 + 30.6
OVR = 76.6
```

**Ventajas:** Balance entre especialización y versatilidad
**Desventajas:** Puede no reflejar jugadores muy versátiles

---

### Método 5: Fórmula Híbrida (ÓPTIMO)

Combina especialización, versatilidad y experiencia.

```
OVR = (Rol_Principal × 0.4) + 
      (Top_3_Promedio × 0.4) + 
      (Todos_Promedio × 0.2)

Donde:
- Rol_Principal: Mejor calificación (40%)
- Top_3_Promedio: Promedio de 3 mejores (40%)
- Todos_Promedio: Promedio de todos (20%)
```

**Ejemplo:**
```
Jugador: "El Fenómeno"

Rol Principal: 92 (ATAQUE)
Top 3: (92 + 82 + 72) / 3 = 82.0
Todos: (92 + 82 + 72 + 65 + 60 + 55) / 6 = 71.0

OVR = (92 × 0.4) + (82.0 × 0.4) + (71.0 × 0.2)
OVR = 36.8 + 32.8 + 14.2
OVR = 83.8
```

**Ventajas:** Balance perfecto entre todos los factores
**Desventajas:** Más complejo de calcular

---

## Comparación de Métodos

### Jugador Especialista: "El Goleador"

```
Calificaciones:
ATAQUE: 92, MEDIOCAMPO: 75, CARRILERO: 68
DEFENSA: 58, ARQUERO: 50, DT: 55

Método 1 (Simple):           66.3
Método 2 (Ponderado):        81.7 ⭐ (refleja especialización)
Método 3 (Top 3):            78.3
Método 4 (Principal):        76.6
Método 5 (Híbrido):          80.5 ⭐⭐ (más balanceado)
```

### Jugador Versátil: "El Todoterreno"

```
Calificaciones:
ATAQUE: 75, MEDIOCAMPO: 78, CARRILERO: 80
DEFENSA: 72, ARQUERO: 65, DT: 70

Método 1 (Simple):           73.3 ⭐ (refleja versatilidad)
Método 2 (Ponderado):        75.8
Método 3 (Top 3):            77.7
Método 4 (Principal):        75.7
Método 5 (Híbrido):          76.4 ⭐⭐ (más balanceado)
```

**Conclusión:** 
- Método 2 (Ponderado) es mejor para especialistas
- Método 5 (Híbrido) es el más equilibrado para todos los perfiles


---

## Implementación en el Sistema Actual

### Opción 1: Calcular en el Servicio (Recomendado)

Agregar método al `RatingService`:

```java
/**
 * Calcula la calificación general (OVR) de un jugador usando el método híbrido.
 * 
 * @param playerProfileId UUID del perfil del jugador
 * @return Calificación general del jugador (0-100)
 */
@Transactional(readOnly = true)
public BigDecimal calculateOverallRating(UUID playerProfileId) {
    logger.debug("Calculando OVR para jugador {}", playerProfileId);
    
    // Obtener todas las calificaciones del jugador
    List<PlayerRating> ratings = playerRatingRepository
            .findByPlayerProfileId(playerProfileId);
    
    if (ratings.isEmpty()) {
        logger.warn("No se encontraron calificaciones para jugador {}", playerProfileId);
        return BigDecimal.ZERO;
    }
    
    // Ordenar por calificación descendente
    ratings.sort((r1, r2) -> r2.getCurrentRating().compareTo(r1.getCurrentRating()));
    
    // Método Híbrido:
    // OVR = (Mejor × 0.4) + (Top3_Promedio × 0.4) + (Todos_Promedio × 0.2)
    
    // 1. Mejor calificación (40%)
    BigDecimal bestRating = ratings.get(0).getCurrentRating();
    BigDecimal bestComponent = bestRating.multiply(BigDecimal.valueOf(0.4));
    
    // 2. Promedio de top 3 (40%)
    BigDecimal top3Sum = BigDecimal.ZERO;
    int top3Count = Math.min(3, ratings.size());
    for (int i = 0; i < top3Count; i++) {
        top3Sum = top3Sum.add(ratings.get(i).getCurrentRating());
    }
    BigDecimal top3Average = top3Sum.divide(BigDecimal.valueOf(top3Count), 2, RoundingMode.HALF_UP);
    BigDecimal top3Component = top3Average.multiply(BigDecimal.valueOf(0.4));
    
    // 3. Promedio de todos (20%)
    BigDecimal allSum = ratings.stream()
            .map(PlayerRating::getCurrentRating)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal allAverage = allSum.divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);
    BigDecimal allComponent = allAverage.multiply(BigDecimal.valueOf(0.2));
    
    // Calcular OVR final
    BigDecimal overallRating = bestComponent.add(top3Component).add(allComponent);
    overallRating = overallRating.setScale(2, RoundingMode.HALF_UP);
    
    logger.debug("OVR calculado para jugador {}: {}", playerProfileId, overallRating);
    
    return overallRating;
}

/**
 * Calcula la calificación general ponderada por prioridades.
 * 
 * @param playerProfileId UUID del perfil del jugador
 * @return Calificación general ponderada
 */
@Transactional(readOnly = true)
public BigDecimal calculateWeightedOverallRating(UUID playerProfileId) {
    logger.debug("Calculando OVR ponderado para jugador {}", playerProfileId);
    
    List<PlayerRating> ratings = playerRatingRepository
            .findByPlayerProfileId(playerProfileId);
    
    if (ratings.isEmpty()) {
        return BigDecimal.ZERO;
    }
    
    BigDecimal weightedSum = BigDecimal.ZERO;
    BigDecimal totalWeight = BigDecimal.ZERO;
    
    for (PlayerRating rating : ratings) {
        // Obtener peso según prioridad
        BigDecimal weight = BigDecimal.valueOf(rating.getPriorityLevel().getMultiplier());
        
        // Si no tiene prioridad asignada, usar peso mínimo
        if (weight.compareTo(BigDecimal.ZERO) == 0) {
            weight = BigDecimal.valueOf(0.1);
        }
        
        weightedSum = weightedSum.add(rating.getCurrentRating().multiply(weight));
        totalWeight = totalWeight.add(weight);
    }
    
    BigDecimal overallRating = weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP);
    
    logger.debug("OVR ponderado calculado para jugador {}: {}", playerProfileId, overallRating);
    
    return overallRating;
}
```


### Opción 2: Agregar Endpoint al Controlador

```java
/**
 * Obtiene la calificación general (OVR) de un jugador.
 */
@GetMapping("/player/{playerProfileId}/overall")
@Operation(summary = "Obtener calificación general del jugador", 
           description = "Calcula y retorna el OVR (Overall Rating) del jugador considerando todos sus roles")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "OVR calculado exitosamente"),
    @ApiResponse(responseCode = "404", description = "Jugador no encontrado")
})
public ResponseEntity<OverallRatingResponse> getOverallRating(
        @Parameter(description = "UUID del perfil del jugador")
        @PathVariable UUID playerProfileId) {
    
    logger.debug("Consultando OVR para jugador {}", playerProfileId);
    
    try {
        // Calcular ambos métodos
        BigDecimal hybridOVR = ratingService.calculateOverallRating(playerProfileId);
        BigDecimal weightedOVR = ratingService.calculateWeightedOverallRating(playerProfileId);
        
        // Obtener todas las calificaciones para contexto
        List<PlayerRating> ratings = ratingService.getPlayerRatings(playerProfileId);
        
        if (ratings.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Construir respuesta
        OverallRatingResponse response = new OverallRatingResponse();
        response.setPlayerProfileId(playerProfileId);
        response.setHybridOVR(hybridOVR);
        response.setWeightedOVR(weightedOVR);
        response.setTotalRatings(ratings.size());
        
        // Agregar clasificación
        response.setClassification(getClassification(hybridOVR));
        
        // Agregar desglose por rol
        Map<RoleType, BigDecimal> roleBreakdown = new HashMap<>();
        for (PlayerRating rating : ratings) {
            roleBreakdown.put(rating.getRoleType(), rating.getCurrentRating());
        }
        response.setRoleBreakdown(roleBreakdown);
        
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        logger.error("Error calculando OVR para jugador {}: {}", playerProfileId, e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

private String getClassification(BigDecimal ovr) {
    if (ovr.compareTo(BigDecimal.valueOf(95)) >= 0) return "LEYENDA";
    if (ovr.compareTo(BigDecimal.valueOf(85)) >= 0) return "ÉLITE";
    if (ovr.compareTo(BigDecimal.valueOf(75)) >= 0) return "EXPERTO";
    if (ovr.compareTo(BigDecimal.valueOf(65)) >= 0) return "AVANZADO";
    if (ovr.compareTo(BigDecimal.valueOf(55)) >= 0) return "INTERMEDIO";
    if (ovr.compareTo(BigDecimal.valueOf(50)) >= 0) return "PRINCIPIANTE";
    return "NOVATO";
}
```

### DTO de Respuesta

```java
public class OverallRatingResponse {
    private UUID playerProfileId;
    private BigDecimal hybridOVR;
    private BigDecimal weightedOVR;
    private String classification;
    private Integer totalRatings;
    private Map<RoleType, BigDecimal> roleBreakdown;
    
    // Getters y Setters
}
```


---

## Ejemplo de Uso del Endpoint

### Request

```http
GET /api/v1/ratings/player/550e8400-e29b-41d4-a716-446655440000/overall
Authorization: Bearer <token>
```

### Response

```json
{
  "playerProfileId": "550e8400-e29b-41d4-a716-446655440000",
  "hybridOVR": 83.80,
  "weightedOVR": 81.70,
  "classification": "EXPERTO",
  "totalRatings": 6,
  "roleBreakdown": {
    "ATAQUE": 92.00,
    "MEDIOCAMPO": 82.00,
    "CARRILERO": 72.00,
    "DEFENSA": 65.00,
    "DT": 60.00,
    "ARQUERO": 55.00
  }
}
```

---

## Visualización del OVR

### Tarjeta de Jugador Estilo FIFA

```
┌─────────────────────────────────────┐
│           EL FENÓMENO               │
│                                     │
│            ⭐ 84 ⭐                  │
│           EXPERTO                   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ ATAQUE      ████████████ 92 │   │
│  │ MEDIOCAMPO  ██████████   82 │   │
│  │ CARRILERO   ████████     72 │   │
│  │ DEFENSA     ██████       65 │   │
│  │ DT          █████        60 │   │
│  │ ARQUERO     ████         55 │   │
│  └─────────────────────────────┘   │
│                                     │
│  Partidos: 113  |  Versatilidad: ⭐⭐⭐│
└─────────────────────────────────────┘
```

### Comparación de Jugadores

```
JUGADOR A: "El Goleador"        JUGADOR B: "El Todoterreno"
OVR: 82 ⭐⭐                      OVR: 76 ⭐⭐

Especialización: ATAQUE         Especialización: EQUILIBRADO
Mejor rol: 92                   Mejor rol: 80
Versatilidad: ⭐⭐               Versatilidad: ⭐⭐⭐⭐

Recomendado para:               Recomendado para:
✓ Posición fija                 ✓ Rotaciones
✓ Finalización                  ✓ Comodín
✓ Goles                         ✓ Adaptabilidad
```


---

## Casos de Uso del OVR

### 1. Ranking de Jugadores

```sql
-- Query para obtener top 10 jugadores por OVR
SELECT 
    pp.atleta_uuid,
    pp.alias,
    -- Calcular OVR híbrido
    (
        -- Mejor calificación (40%)
        (SELECT MAX(current_rating) FROM player_ratings WHERE player_profile_id = pp.atleta_uuid) * 0.4 +
        -- Top 3 promedio (40%)
        (SELECT AVG(current_rating) FROM (
            SELECT current_rating FROM player_ratings 
            WHERE player_profile_id = pp.atleta_uuid 
            ORDER BY current_rating DESC LIMIT 3
        ) top3) * 0.4 +
        -- Todos promedio (20%)
        (SELECT AVG(current_rating) FROM player_ratings WHERE player_profile_id = pp.atleta_uuid) * 0.2
    ) as overall_rating
FROM player_profiles pp
WHERE EXISTS (SELECT 1 FROM player_ratings WHERE player_profile_id = pp.atleta_uuid)
ORDER BY overall_rating DESC
LIMIT 10;
```

### 2. Balanceo de Equipos

```
Equipo A:
- Jugador 1: OVR 85
- Jugador 2: OVR 78
- Jugador 3: OVR 72
- Jugador 4: OVR 68
- Jugador 5: OVR 65
Promedio: 73.6

Equipo B:
- Jugador 6: OVR 82
- Jugador 7: OVR 75
- Jugador 8: OVR 70
- Jugador 9: OVR 69
- Jugador 10: OVR 72
Promedio: 73.6

✓ Equipos balanceados (diferencia < 2 puntos)
```

### 3. Recomendación de Posición

```
Jugador: "El Versátil"
OVR General: 75

Calificaciones por rol:
- MEDIOCAMPO: 82 (mejor rol) ⭐⭐⭐
- CARRILERO: 78
- ATAQUE: 72
- DEFENSA: 68
- DT: 65
- ARQUERO: 60

Recomendación: Jugar como MEDIOCAMPISTA
Razón: 7 puntos por encima del OVR general
Alternativa: CARRILERO (3 puntos por encima)
```

### 4. Sistema de Matchmaking

```
Buscar partido para jugador con OVR 75:

Rango aceptable: 70-80 (±5 puntos)

Jugadores disponibles:
✓ Jugador A: OVR 78 (diferencia: 3)
✓ Jugador B: OVR 72 (diferencia: 3)
✓ Jugador C: OVR 76 (diferencia: 1)
✗ Jugador D: OVR 85 (diferencia: 10) - Fuera de rango
✗ Jugador E: OVR 65 (diferencia: 10) - Fuera de rango
```


---

## Métricas Adicionales Derivadas del OVR

### 1. Índice de Versatilidad

```
Versatilidad = (Número de roles >= OVR - 10) / 6

Ejemplo:
Jugador con OVR 75
Roles >= 65: ATAQUE(82), MEDIOCAMPO(78), CARRILERO(72), DEFENSA(68)
Versatilidad = 4/6 = 0.67 (Alta)

Clasificación:
- 1.00: Máxima (todos los roles competitivos)
- 0.67-0.99: Alta
- 0.34-0.66: Media
- 0.01-0.33: Baja
- 0.00: Especialista puro
```

### 2. Potencial de Crecimiento

```
Potencial = (100 - OVR) × Factor_Edad × Factor_Experiencia

Factor_Edad:
- < 25 años: 1.0 (máximo potencial)
- 25-30 años: 0.7
- > 30 años: 0.4

Factor_Experiencia:
- < 20 partidos: 1.0
- 20-50 partidos: 0.8
- > 50 partidos: 0.5

Ejemplo:
Jugador 22 años, OVR 75, 15 partidos
Potencial = (100 - 75) × 1.0 × 1.0 = 25 puntos
```

### 3. Consistencia

```
Consistencia = 100 - (Desviación_Estándar_Roles × 2)

Ejemplo:
Roles: 92, 82, 72, 65, 60, 55
Promedio: 71
Desviación: 13.4
Consistencia = 100 - (13.4 × 2) = 73.2

Clasificación:
- 90-100: Muy consistente (todoterreno)
- 70-89: Consistente
- 50-69: Variable
- < 50: Muy especializado
```

### 4. Valor de Mercado (Simulado)

```
Valor = OVR² × Versatilidad × Factor_Edad × 100

Ejemplo:
OVR: 85
Versatilidad: 0.67
Factor_Edad: 1.0 (joven)

Valor = 85² × 0.67 × 1.0 × 100
Valor = 7,225 × 0.67 × 100
Valor = 484,075 puntos
```

---

## Resumen: ¿Qué Método Usar?

### Para Ranking General
✅ **Método 5 (Híbrido)** - Balance perfecto

### Para Especialistas
✅ **Método 2 (Ponderado)** - Refleja prioridades

### Para Comparación Rápida
✅ **Método 3 (Top 3)** - Simple y efectivo

### Para Jugadores Versátiles
✅ **Método 1 (Simple)** - Valora todas las habilidades

---

## Implementación Recomendada

```java
// En RatingService.java

/**
 * Calcula el OVR completo con todas las métricas.
 */
@Transactional(readOnly = true)
public PlayerOverallStats calculateCompleteOverall(UUID playerProfileId) {
    List<PlayerRating> ratings = getPlayerRatings(playerProfileId);
    
    if (ratings.isEmpty()) {
        throw new PlayerNotFoundException("No ratings found", playerProfileId.toString());
    }
    
    PlayerOverallStats stats = new PlayerOverallStats();
    stats.setPlayerProfileId(playerProfileId);
    
    // OVR principal (híbrido)
    stats.setOverallRating(calculateHybridOVR(ratings));
    
    // OVR ponderado
    stats.setWeightedRating(calculateWeightedOVR(ratings));
    
    // Versatilidad
    stats.setVersatilityIndex(calculateVersatility(ratings, stats.getOverallRating()));
    
    // Consistencia
    stats.setConsistencyScore(calculateConsistency(ratings));
    
    // Mejor rol
    stats.setBestRole(findBestRole(ratings));
    
    // Clasificación
    stats.setClassification(getClassification(stats.getOverallRating()));
    
    return stats;
}
```

---

## Conclusión

**SÍ, es totalmente posible generar un valor único (OVR) por jugador** con el sistema actual:

✅ Los datos ya existen en `player_ratings`
✅ Se pueden calcular múltiples métodos de OVR
✅ Se puede implementar fácilmente en el servicio
✅ Se puede exponer vía API REST
✅ Permite ranking, comparación y matchmaking
✅ Base para métricas avanzadas (versatilidad, consistencia, valor)

**Recomendación:** Implementar el **Método 5 (Híbrido)** como OVR principal y el **Método 2 (Ponderado)** como alternativa, exponiendo ambos en la API para diferentes casos de uso.

