package com.atleta.demo.controller;

import com.atleta.demo.dto.request.ChangePasswordRequest;
import com.atleta.demo.dto.request.CreateAthleteRequest;
import com.atleta.demo.dto.request.GoogleAuthRequest;
import com.atleta.demo.dto.request.LoginRequest;
import com.atleta.demo.dto.request.UpdateAthleteRequest;
import com.atleta.demo.dto.response.AthleteResponse;
import com.atleta.demo.dto.response.AuthResponse;
import com.atleta.demo.entity.Athlete;
import com.atleta.demo.service.AthleteService;
import com.atleta.demo.service.GoogleAuthService;
import com.atleta.demo.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controlador REST para la gestión de atletas.
 * Proporciona endpoints para registro, login y gestión de atletas.
 * 
 * Requisitos implementados:
 * - 1.1: UUID único como identificador principal
 * - 1.2: Email único en todo el sistema
 * - 1.3: Almacenamiento seguro del hash de contraseña
 * - 1.4: Registro automático de fecha de creación
 * - 1.5: Validación de nombre no vacío y formato válido
 */
@RestController
@RequestMapping("/api/v1/athletes")
@Tag(name = "Atletas", description = "Gestión de atletas - identidad global, registro y autenticación")
public class AthleteController {

    private static final Logger logger = LoggerFactory.getLogger(AthleteController.class);

    private final AthleteService athleteService;
    private final GoogleAuthService googleAuthService;
    private final JwtService jwtService;

    public AthleteController(AthleteService athleteService, 
                            GoogleAuthService googleAuthService,
                            JwtService jwtService) {
        this.athleteService = athleteService;
        this.googleAuthService = googleAuthService;
        this.jwtService = jwtService;
    }

    /**
     * Registra un nuevo atleta en el sistema.
     * Requisitos: 1.1, 1.2, 1.3, 1.4, 1.5
     */
    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo atleta", 
               description = "Crea un nuevo atleta con UUID único, email único y contraseña hasheada")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Atleta registrado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "Email ya existe en el sistema")
    })
    public ResponseEntity<AthleteResponse> registerAthlete(
            @Valid @RequestBody CreateAthleteRequest request) {
        
        logger.info("Solicitud de registro para email: {}", request.getEmail());
        
        try {
            AthleteResponse response = athleteService.registerAthlete(request);
            logger.info("Atleta registrado exitosamente con UUID: {}", response.getAtletaUuid());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error en registro: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * Autentica un atleta con email y contraseña.
     * Requisito: 1.3 (verificación de contraseña hasheada)
     */
    @PostMapping("/login")
    @Operation(summary = "Autenticar atleta", 
               description = "Autentica un atleta con email y contraseña")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Autenticación exitosa"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        
        logger.info("Intento de login para email: {}", request.getEmail());
        
        Optional<Athlete> athleteOpt = athleteService.authenticateEntity(
                request.getEmail(), request.getPassword());
        
        if (athleteOpt.isPresent()) {
            Athlete athlete = athleteOpt.get();
            String accessToken = jwtService.generateToken(athlete);

            AuthResponse response = new AuthResponse(
                athlete.getAtletaUuid(),
                athlete.getEmail(),
                athlete.getNombre(),
                athlete.getGenero(),
                athlete.getAuthProvider(),
                accessToken
            );

            logger.info("Login exitoso para email: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } else {
            logger.warn("Login fallido para email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Autentica un atleta con Google OAuth2.
     * Si el usuario no existe, lo crea automáticamente.
     * Si existe con cuenta local, vincula la cuenta con Google.
     */
    @PostMapping("/auth/google")
    @Operation(summary = "Autenticar con Google", 
               description = "Autentica o registra un atleta usando Google OAuth2. " +
                           "Si el usuario no existe, se crea automáticamente. " +
                           "Si existe con cuenta local, se vincula con Google.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Autenticación exitosa"),
        @ApiResponse(responseCode = "400", description = "Token de Google inválido"),
        @ApiResponse(responseCode = "401", description = "Email de Google no verificado")
    })
    public ResponseEntity<AuthResponse> authenticateWithGoogle(
            @Valid @RequestBody GoogleAuthRequest request) {
        
        logger.info("Intento de autenticación con Google");
        
        try {
            // Autenticar o registrar con Google
            Athlete athlete = googleAuthService.authenticateWithGoogle(request.getIdToken());
            
            // Generar token JWT
            String accessToken = jwtService.generateToken(athlete);
            
            // Crear respuesta
            AuthResponse response = new AuthResponse(
                athlete.getAtletaUuid(),
                athlete.getEmail(),
                athlete.getNombre(),
                athlete.getGenero(),
                athlete.getAuthProvider(),
                accessToken
            );
            
            logger.info("Autenticación con Google exitosa para email: {}", athlete.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Error en autenticación con Google: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error inesperado en autenticación con Google", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene un atleta por su UUID.
     * Requisito: 1.1 (búsqueda por UUID único)
     */
    @GetMapping("/{atletaUuid}")
    @Operation(summary = "Obtener atleta por UUID", 
               description = "Busca un atleta específico por su UUID único")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Atleta encontrado"),
        @ApiResponse(responseCode = "404", description = "Atleta no encontrado")
    })
    public ResponseEntity<AthleteResponse> getAthleteByUuid(
            @Parameter(description = "UUID único del atleta")
            @PathVariable UUID atletaUuid) {
        
        logger.debug("Buscando atleta por UUID: {}", atletaUuid);
        
        Optional<AthleteResponse> athleteOpt = athleteService.findByUuid(atletaUuid);
        
        return athleteOpt.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtiene un atleta por su email.
     * Requisito: 1.2 (búsqueda por email único)
     */
    @GetMapping("/by-email/{email}")
    @Operation(summary = "Obtener atleta por email", 
               description = "Busca un atleta específico por su email único")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Atleta encontrado"),
        @ApiResponse(responseCode = "404", description = "Atleta no encontrado")
    })
    public ResponseEntity<AthleteResponse> getAthleteByEmail(
            @Parameter(description = "Email único del atleta")
            @PathVariable String email) {
        
        logger.debug("Buscando atleta por email: {}", email);
        
        Optional<AthleteResponse> athleteOpt = athleteService.findByEmail(email);
        
        return athleteOpt.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Actualiza la información básica de un atleta.
     * Requisito: 1.5 (validación de nombre no vacío)
     */
    @PutMapping("/{atletaUuid}")
    @Operation(summary = "Actualizar información del atleta", 
               description = "Actualiza el nombre del atleta")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Atleta actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Atleta no encontrado")
    })
    public ResponseEntity<AthleteResponse> updateAthlete(
            @Parameter(description = "UUID único del atleta")
            @PathVariable UUID atletaUuid,
            @Valid @RequestBody UpdateAthleteRequest request) {
        
        logger.info("Actualizando atleta: {}", atletaUuid);
        
        try {
            AthleteResponse response = athleteService.updateAthlete(atletaUuid, request.getNombre());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error actualizando atleta: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Cambia la contraseña de un atleta.
     * Requisito: 1.3 (almacenamiento seguro del hash de contraseña)
     */
    @PutMapping("/{atletaUuid}/password")
    @Operation(summary = "Cambiar contraseña del atleta", 
               description = "Cambia la contraseña del atleta verificando la contraseña actual")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contraseña cambiada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o contraseña actual incorrecta"),
        @ApiResponse(responseCode = "404", description = "Atleta no encontrado")
    })
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "UUID único del atleta")
            @PathVariable UUID atletaUuid,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        logger.info("Cambiando contraseña para atleta: {}", atletaUuid);
        
        try {
            athleteService.changePassword(atletaUuid, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Error cambiando contraseña: {}", e.getMessage());
            if (e.getMessage().contains("No se encontró")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        }
    }

    /**
     * Busca atletas por nombre (búsqueda parcial).
     * Requisito: 1.5 (búsqueda por nombre válido)
     */
    @GetMapping("/search")
    @Operation(summary = "Buscar atletas por nombre", 
               description = "Busca atletas que contengan el texto especificado en su nombre")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Búsqueda completada exitosamente")
    })
    public ResponseEntity<List<AthleteResponse>> searchAthletesByName(
            @Parameter(description = "Texto a buscar en el nombre")
            @RequestParam String nombre) {
        
        logger.debug("Buscando atletas por nombre: {}", nombre);
        
        List<AthleteResponse> athletes = athleteService.searchByName(nombre);
        return ResponseEntity.ok(athletes);
    }

    /**
     * Obtiene atletas registrados después de una fecha específica.
     * Requisito: 1.4 (consulta por fecha de creación)
     */
    @GetMapping("/registered-after")
    @Operation(summary = "Obtener atletas registrados después de una fecha", 
               description = "Obtiene todos los atletas registrados después de la fecha especificada")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Consulta completada exitosamente")
    })
    public ResponseEntity<List<AthleteResponse>> getAthletesRegisteredAfter(
            @Parameter(description = "Fecha de referencia (formato: yyyy-MM-ddTHH:mm:ss)")
            @RequestParam LocalDateTime fecha) {
        
        logger.debug("Buscando atletas registrados después de: {}", fecha);
        
        List<AthleteResponse> athletes = athleteService.findRegisteredAfter(fecha);
        return ResponseEntity.ok(athletes);
    }

    /**
     * Verifica si un email ya está registrado.
     * Requisito: 1.2 (verificación de unicidad de email)
     */
    @GetMapping("/email-exists/{email}")
    @Operation(summary = "Verificar si email existe", 
               description = "Verifica si un email ya está registrado en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verificación completada")
    })
    public ResponseEntity<Boolean> emailExists(
            @Parameter(description = "Email a verificar")
            @PathVariable String email) {
        
        logger.debug("Verificando existencia de email: {}", email);
        
        boolean exists = athleteService.emailExists(email);
        return ResponseEntity.ok(exists);
    }

    /**
     * Obtiene estadísticas generales de atletas.
     * Requisito: Información general del sistema
     */
    @GetMapping("/stats")
    @Operation(summary = "Obtener estadísticas de atletas", 
               description = "Obtiene estadísticas generales como el número total de atletas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estadísticas obtenidas exitosamente")
    })
    public ResponseEntity<Long> getTotalAthletes() {
        
        logger.debug("Obteniendo estadísticas de atletas");
        
        long total = athleteService.getTotalAthletes();
        return ResponseEntity.ok(total);
    } 
}
