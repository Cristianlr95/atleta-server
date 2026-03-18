# Documento de Diseño: API Atletas y Fútbol

## Visión General

Este diseño establece una API REST completa para la gestión de atletas y fútbol que maneja identidades globales de atletas, perfiles de jugadores, equipos, partidos, estadísticas y todo el ecosistema deportivo. La solución implementa una arquitectura en capas con separación clara de responsabilidades, manejo de relaciones complejas entre entidades, y lógica de negocio específica del dominio deportivo.

## Arquitectura

### Arquitectura en Capas

La aplicación seguirá el patrón de arquitectura en capas estándar:

```
┌─────────────────────────────────────┐
│           Controller Layer          │  ← Manejo de HTTP requests/responses
├─────────────────────────────────────┤
│            Service Layer            │  ← Lógica de negocio
├─────────────────────────────────────┤
│          Repository Layer           │  ← Acceso a datos
├─────────────────────────────────────┤
│            Data Layer               │  ← Entidades JPA
└─────────────────────────────────────┘
```

### Estructura de Paquetes

```
com.atleta.demo/
├── config/                 # Configuraciones
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   ├── OpenApiConfig.java
│   └── DatabaseConfig.java
├── controller/             # Controladores REST
│   ├── AthleteController.java
│   ├── PlayerProfileController.java
│   ├── TeamController.java
│   ├── MatchController.java
│   └── PositionController.java
├── service/               # Servicios de negocio
│   ├── AthleteService.java
│   ├── PlayerProfileService.java
│   ├── TeamService.java
│   ├── MatchService.java
│   └── TrustScoreService.java
├── repository/            # Repositorios de datos
│   ├── AthleteRepository.java
│   ├── PlayerProfileRepository.java
│   ├── TeamRepository.java
│   ├── MatchRepository.java
│   └── TrustLogRepository.java
├── entity/               # Entidades JPA
│   ├── BaseEntity.java
│   ├── Athlete.java
│   ├── PlayerProfile.java
│   ├── Position.java
│   ├── Team.java
│   ├── Match.java
│   ├── MatchEvent.java
│   └── PlayerHistory.java
├── dto/                  # Data Transfer Objects
│   ├── request/
│   │   ├── CreateAthleteRequest.java
│   │   ├── CreateTeamRequest.java
│   │   └── CreateMatchRequest.java
│   └── response/
│       ├── AthleteResponse.java
│       ├── TeamResponse.java
│       └── MatchResponse.java
├── enums/               # Enumeraciones
│   ├── MatchStatus.java
│   ├── MatchMode.java
│   ├── PlayerRole.java
│   └── EventType.java
├── exception/            # Manejo de excepciones
│   ├── GlobalExceptionHandler.java
│   ├── AthleteNotFoundException.java
│   └── InvalidMatchStateException.java
└── validation/           # Validadores personalizados
    └── TrustScoreValidator.java
```

## Componentes e Interfaces

### 1. Capa de Controladores

**BaseController**: Clase abstracta que proporciona funcionalidad común:
- Logging automático de requests
- Validación de parámetros comunes
- Métodos utilitarios para respuestas

**ExampleController**: Controlador de ejemplo que demuestra:
- Operaciones CRUD completas
- Validación de entrada
- Manejo de errores
- Documentación OpenAPI

### 2. Capa de Servicios

**BaseService**: Interfaz que define operaciones comunes:
- Operaciones CRUD genéricas
- Validaciones de negocio
- Logging de operaciones

**ExampleService**: Implementación de ejemplo que demuestra:
- Lógica de negocio
- Validaciones específicas
- Transacciones

### 3. Capa de Repositorio

**Repositorios JPA**: Extienden JpaRepository para operaciones básicas:
- Métodos CRUD automáticos
- Consultas personalizadas con @Query
- Paginación y ordenamiento

### 4. Manejo de Excepciones

**GlobalExceptionHandler**: Maneja todas las excepciones de forma centralizada:
- Excepciones de validación
- Excepciones de negocio
- Excepciones técnicas
- Excepciones de seguridad

**ErrorResponse**: Estructura estándar para respuestas de error:
```java
{
  "timestamp": "2024-01-07T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/examples",
  "traceId": "abc123",
  "details": [
    {
      "field": "name",
      "message": "Name is required"
    }
  ]
}
```

### 5. Configuración de Seguridad

**SecurityConfig**: Configuración de Spring Security:
- Endpoints públicos y protegidos
- Configuración OAuth2
- CORS habilitado
- CSRF configurado apropiadamente

### 6. Documentación API

**OpenApiConfig**: Configuración de Swagger/OpenAPI:
- Información general de la API
- Configuración de seguridad
- Ejemplos de requests/responses

## Modelos de Datos

### BaseEntity
Entidad base que proporciona campos comunes:
```java
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
}
```

### Athlete (Identidad Global)
```java
@Entity
@Table(name = "athletes")
public class Athlete {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID atletaUuid;
    
    @Column(unique = true, nullable = false)
    @Email
    private String email;
    
    @Column(nullable = false)
    private String passwordHash;
    
    @NotBlank
    @Size(max = 100)
    private String nombre;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // Relación uno-a-uno con PlayerProfile
    @OneToOne(mappedBy = "athlete", cascade = CascadeType.ALL)
    private PlayerProfile playerProfile;
}
```

### PlayerProfile (Contexto Fútbol)
```java
@Entity
@Table(name = "player_profile")
public class PlayerProfile {
    @Id
    private UUID atletaUuid;
    
    @OneToOne
    @MapsId
    @JoinColumn(name = "atleta_uuid")
    private Athlete athlete;
    
    @Size(max = 50)
    private String alias;
    
    @Column(nullable = false)
    private Integer trustScore = 100;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // Relaciones
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    private List<PlayerPosition> positions = new ArrayList<>();
    
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    private List<TrustLog> trustLogs = new ArrayList<>();
}
```

### Position
```java
@Entity
@Table(name = "positions")
public class Position extends BaseEntity {
    @NotBlank
    @Size(max = 50)
    private String nombre; // Portero, Defensa, Carrilero, Mediocampista, Delantero, DT
}
```

### PlayerPosition (Prioridades)
```java
@Entity
@Table(name = "player_positions")
public class PlayerPosition extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id")
    private PlayerProfile player;
    
    @ManyToOne
    @JoinColumn(name = "position_id")
    private Position position;
    
    @Min(1) @Max(3)
    private Integer prioridad;
    
    @Min(0)
    private Integer xp = 0;
}
```

### Team
```java
@Entity
@Table(name = "teams")
public class Team extends BaseEntity {
    @NotBlank
    @Size(max = 100)
    private String nombre;
    
    private String logoUrl;
    
    private Integer anioFundacion;
    
    @ManyToOne
    @JoinColumn(name = "creador_user_id")
    private PlayerProfile creador;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // Relaciones
    @OneToOne(mappedBy = "team", cascade = CascadeType.ALL)
    private TeamStats stats;
    
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    private List<TeamMember> members = new ArrayList<>();
}
```

### TeamMember (N—M entre PlayerProfile y Team)
```java
@Entity
@Table(name = "team_members")
public class TeamMember extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private PlayerProfile player;
    
    @Enumerated(EnumType.STRING)
    private PlayerRole rol; // JUGADOR, CAPITAN, DT
    
    @Column(nullable = false)
    private Boolean activo = true;
    
    @CreationTimestamp
    private LocalDateTime joinedAt;
}
```

### MatchTeam (Match 1—2 Teams exactamente)
```java
@Entity
@Table(name = "match_teams")
public class MatchTeam extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;
    
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;
    
    @Column(nullable = false)
    private Boolean esLocal;
    
    @Min(0)
    private Integer goles = 0;
    
    // Constraint: Un match debe tener exactamente 2 equipos
    @PrePersist
    @PreUpdate
    private void validateTwoTeamsPerMatch() {
        // Validación implementada en el servicio
    }
}
```

### MatchPlayer (Match N—N Players)
```java
@Entity
@Table(name = "match_players")
public class MatchPlayer extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;
    
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private PlayerProfile player;
    
    @ManyToOne
    @JoinColumn(name = "position_id")
    private Position position;
    
    @Enumerated(EnumType.STRING)
    private PlayerRole rol; // JUGADOR, CAPITAN, DT
    
    @Column(nullable = false)
    private Boolean confirmado = false;
}
```

### PlayerHistory (Fuente de Verdad - Inmutable)
```java
@Entity
@Table(name = "player_history")
@Immutable // Hibernate annotation para entidad inmutable
public class PlayerHistory extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "match_id", updatable = false)
    private Match match;
    
    @ManyToOne
    @JoinColumn(name = "user_id", updatable = false)
    private PlayerProfile player;
    
    @ManyToOne
    @JoinColumn(name = "team_id", updatable = false)
    private Team team;
    
    @ManyToOne
    @JoinColumn(name = "position_id", updatable = false)
    private Position position;
    
    @Column(updatable = false)
    private Integer goles;
    
    @Column(updatable = false)
    private Integer asistencias;
    
    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private MatchResult resultado; // VICTORIA, DERROTA, EMPATE
    
    @Column(updatable = false)
    private Integer xpGanada;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    // Esta es la FUENTE DE VERDAD para estadísticas
    // Una vez creado, NUNCA se modifica
}
```

### Match (1—2 Teams exactamente)
```java
@Entity
@Table(name = "matches")
public class Match extends BaseEntity {
    @Enumerated(EnumType.STRING)
    private MatchMode modalidad; // 5v5, 6v6, 7v7
    
    private LocalDateTime fechaHoraProgramada;
    
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal latitud;
    
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal longitud;
    
    @DecimalMin("0.0")
    private BigDecimal cuota;
    
    @ManyToOne
    @JoinColumn(name = "creador_user_id")
    private PlayerProfile creador;
    
    @Enumerated(EnumType.STRING)
    private MatchStatus estado = MatchStatus.CREADO;
    
    private LocalDateTime startedAt;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // Relaciones - EXACTAMENTE 2 equipos por partido
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @Size(min = 2, max = 2, message = "Un partido debe tener exactamente 2 equipos")
    private List<MatchTeam> matchTeams = new ArrayList<>();
    
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    private List<MatchPlayer> players = new ArrayList<>();
    
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    private List<MatchEvent> events = new ArrayList<>();
    
    // Método de conveniencia para validar 2 equipos
    public boolean hasExactlyTwoTeams() {
        return matchTeams != null && matchTeams.size() == 2;
    }
}
```
```java
@Entity
@Table(name = "trust_logs")
public class TrustLog extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id")
    private PlayerProfile player;
    
    @ManyToOne
    @JoinColumn(name = "match_id", nullable = true)
    private Match match; // Puede ser null si el cambio no está relacionado con un partido
    
    @Column(nullable = false)
    private Integer cambio; // Puede ser positivo o negativo
    
    @NotBlank
    @Size(max = 255)
    private String motivo;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

### Enumeraciones

```java
public enum MatchMode {
    CINCO_VS_CINCO("5v5"),
    SEIS_VS_SEIS("6v6"),
    SIETE_VS_SIETE("7v7");
}

public enum MatchStatus {
    CREADO, INICIADO, FINALIZADO, INVALIDO
}

public enum PlayerRole {
    JUGADOR, CAPITAN, DT
}

public enum EventType {
    GOL, ASISTENCIA
}

public enum MatchResult {
    VICTORIA, DERROTA, EMPATE
}
```

## Propiedades de Corrección

*Una propiedad es una característica o comportamiento que debe mantenerse verdadero en todas las ejecuciones válidas de un sistema, esencialmente, una declaración formal sobre lo que el sistema debe hacer. Las propiedades sirven como puente entre especificaciones legibles por humanos y garantías de corrección verificables por máquinas.*

Ahora procederé a analizar los criterios de aceptación para determinar qué propiedades son verificables automáticamente.

Después de revisar todas las propiedades identificadas como testeables, realizaré una reflexión para eliminar redundancias:

**Reflexión de Propiedades:**
- Las propiedades 2.1, 2.3, 2.4 y 2.5 pueden combinarse en una propiedad más comprehensiva sobre manejo de errores
- Las propiedades 3.1, 3.2, 3.3 y 3.4 pueden combinarse en una propiedad sobre validación de datos
- Las propiedades 4.2 y 4.3 pueden mantenerse separadas ya que prueban diferentes aspectos de seguridad
- Las propiedades 7.1 y 7.2 pueden combinarse en una propiedad sobre logging

### Propiedades de Corrección

**Propiedad 1: Unicidad de atletas**
*Para cualquier* atleta registrado en el sistema, el email debe ser único y el UUID debe ser generado automáticamente
**Valida: Requisitos 1.1, 1.2**

**Propiedad 2: Integridad de perfiles de jugador**
*Para cualquier* perfil de jugador creado, debe estar asociado a un atleta existente y tener un trust_score inicial de 100
**Valida: Requisitos 2.1, 2.2**

**Propiedad 3: Validación de posiciones y prioridades**
*Para cualquier* jugador que defina posiciones, las prioridades deben ser únicas (1, 2, 3) y cada posición debe existir en el catálogo
**Valida: Requisitos 3.2, 3.4**

**Propiedad 4: Integridad de equipos**
*Para cualquier* equipo creado, debe tener un creador válido, nombre único, y estadísticas inicializadas en cero
**Valida: Requisitos 4.1, 4.3, 4.4**

**Propiedad 5: Consistencia de membresía**
*Para cualquier* membresía de equipo, el jugador debe existir, el equipo debe existir, y el rol debe ser válido
**Valida: Requisitos 5.1, 5.2**

**Propiedad 6: Validación de partidos**
*Para cualquier* partido creado, debe tener modalidad válida, coordenadas dentro de rangos válidos, y estado inicial 'CREADO'
**Valida: Requisitos 6.1, 6.2, 6.3**

**Propiedad 7: Integridad de participación**
*Para cualquier* participación en partido, el jugador debe existir, el equipo debe existir, y la posición debe ser válida
**Valida: Requisitos 7.1, 7.2, 7.4**

**Propiedad 8: Validación de eventos**
*Para cualquier* evento de partido registrado, debe tener tipo válido, jugador existente, y requerir confirmación de ambos equipos
**Valida: Requisitos 8.1, 8.2, 8.4**

**Propiedad 9: Inmutabilidad del historial como fuente de verdad**
*Para cualquier* registro de historial de jugador, una vez creado no debe poder modificarse y debe ser la única fuente de verdad para estadísticas históricas
**Valida: Requisitos 9.1, 9.4**

**Propiedad 10: Trazabilidad de confianza**
*Para cualquier* cambio en el trust_score, debe registrarse en trust_logs con motivo, fecha y referencia al partido si aplica
**Valida: Requisitos 10.1, 10.3, 10.4**

**Propiedad 11: Restricción de equipos por partido**
*Para cualquier* partido válido, debe tener exactamente 2 equipos asociados (uno local y uno visitante)
**Valida: Requisitos 6.1, 6.2**

## Manejo de Errores

### Jerarquía de Excepciones

```java
ApiException (RuntimeException)
├── ValidationException
├── BusinessException
├── SecurityException
└── DataAccessException
```

### Códigos de Error Estándar

- **400 Bad Request**: Errores de validación
- **401 Unauthorized**: Falta de autenticación
- **403 Forbidden**: Falta de permisos
- **404 Not Found**: Recurso no encontrado
- **409 Conflict**: Conflicto de datos
- **500 Internal Server Error**: Errores internos

## Estrategia de Testing

### Enfoque Dual de Testing

La estrategia de testing combina dos enfoques complementarios:

**Tests Unitarios**: Verifican ejemplos específicos, casos límite y condiciones de error
- Tests de controladores con MockMvc
- Tests de servicios con mocks
- Tests de repositorios con @DataJpaTest
- Tests de validación con datos específicos

**Tests Basados en Propiedades**: Verifican propiedades universales a través de muchas entradas generadas
- Validación de datos con entradas aleatorias
- Manejo de errores con excepciones generadas
- Operaciones CRUD con datos aleatorios
- Seguridad con diferentes tipos de usuarios

### Configuración de Property-Based Testing

- **Framework**: Utilizaremos jqwik para Java
- **Iteraciones mínimas**: 100 iteraciones por test de propiedad
- **Etiquetado**: Cada test de propiedad debe referenciar su propiedad del documento de diseño
- **Formato de etiqueta**: **Feature: api-foundation, Property {número}: {texto de la propiedad}**

### Cobertura de Testing

**Tests de Integración**:
- Tests de endpoints completos con @SpringBootTest
- Tests de seguridad con diferentes roles
- Tests de base de datos con transacciones

**Tests de Configuración**:
- Verificación de configuraciones de Spring
- Tests de perfiles de entorno
- Validación de documentación OpenAPI

### Herramientas de Testing

- **JUnit 5**: Framework base de testing
- **jqwik**: Property-based testing para Java
- **MockMvc**: Testing de controladores
- **TestContainers**: Tests de integración con base de datos
- **Spring Security Test**: Testing de seguridad
- **WireMock**: Mocking de servicios externos

## Configuraciones Específicas

### application.yaml por Perfiles

**Desarrollo (application-dev.yaml)**:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:devdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  security:
    oauth2:
      client:
        registration:
          dev-client:
            client-id: dev-client
            client-secret: dev-secret

logging:
  level:
    com.atleta.demo: DEBUG
    org.springframework.security: DEBUG
```

**Producción (application-prod.yaml)**:
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

logging:
  level:
    com.atleta.demo: INFO
    org.springframework.security: WARN
```

### Métricas y Monitoreo

**Actuator Endpoints**:
- `/actuator/health`: Estado de salud de la aplicación
- `/actuator/metrics`: Métricas de rendimiento
- `/actuator/info`: Información de la aplicación
- `/actuator/loggers`: Configuración de logging

**Métricas Personalizadas**:
- Contadores de peticiones por endpoint
- Tiempo de respuesta promedio
- Errores por tipo
- Operaciones de base de datos

## Consideraciones de Seguridad

### Configuración OAuth2

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .build();
    }
}
```

### Validación de Entrada

**Anotaciones de Validación**:
- `@NotNull`, `@NotBlank`, `@NotEmpty`
- `@Size`, `@Min`, `@Max`
- `@Pattern`, `@Email`
- `@Valid` para validación anidada

**Validadores Personalizados**:
- Validación de reglas de negocio específicas
- Validación cross-field
- Validación condicional

## Documentación y Versionado

### OpenAPI Configuration

```java
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Atleta API")
                .version("1.0.0")
                .description("API Foundation para aplicaciones Atleta"))
            .addSecurityItem(new SecurityRequirement().addList("OAuth2"))
            .components(new Components()
                .addSecuritySchemes("OAuth2", new SecurityScheme()
                    .type(SecurityScheme.Type.OAUTH2)
                    .flows(new OAuthFlows()
                        .authorizationCode(new OAuthFlow()
                            .authorizationUrl("/oauth2/authorize")
                            .tokenUrl("/oauth2/token")))));
    }
}
```

### Versionado de API

- **Versionado por URL**: `/api/v1/`, `/api/v2/`
- **Headers de versión**: `Accept: application/vnd.atleta.v1+json`
- **Compatibilidad hacia atrás**: Mantener versiones anteriores por período definido