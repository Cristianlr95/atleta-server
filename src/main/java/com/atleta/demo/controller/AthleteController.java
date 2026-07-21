package com.atleta.demo.controller;

import com.atleta.demo.dto.request.ChangePasswordRequest;
import com.atleta.demo.dto.request.CreateAthleteRequest;
import com.atleta.demo.dto.request.GoogleAuthRequest;
import com.atleta.demo.dto.request.LoginRequest;
import com.atleta.demo.dto.request.PasswordResetConfirmRequest;
import com.atleta.demo.dto.request.PasswordResetRequest;
import com.atleta.demo.dto.request.RefreshTokenRequest;
import com.atleta.demo.dto.request.UpdateAthleteRequest;
import com.atleta.demo.dto.response.AthleteResponse;
import com.atleta.demo.dto.response.AuthResponse;
import com.atleta.demo.entity.Athlete;
import com.atleta.demo.security.AuthenticatedUserUtils;
import com.atleta.demo.service.AthleteService;
import com.atleta.demo.service.GoogleAuthService;
import com.atleta.demo.service.SessionLifecycleService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/athletes")
@Tag(name = "Atletas", description = "Gestion de atletas - identidad global, registro y autenticacion")
public class AthleteController {

    private static final Logger logger = LoggerFactory.getLogger(AthleteController.class);

    private final AthleteService athleteService;
    private final GoogleAuthService googleAuthService;
    private final SessionLifecycleService sessionLifecycleService;

    public AthleteController(
            AthleteService athleteService,
            GoogleAuthService googleAuthService,
            SessionLifecycleService sessionLifecycleService
    ) {
        this.athleteService = athleteService;
        this.googleAuthService = googleAuthService;
        this.sessionLifecycleService = sessionLifecycleService;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo atleta",
            description = "Crea un nuevo atleta con UUID unico, email unico y contrasena hasheada")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Atleta registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
            @ApiResponse(responseCode = "409", description = "Email ya existe en el sistema")
    })
    public ResponseEntity<AthleteResponse> registerAthlete(@Valid @RequestBody CreateAthleteRequest request) {
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

    @PostMapping("/login")
    @Operation(summary = "Autenticar atleta", description = "Autentica un atleta con email y contrasena")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticacion exitosa"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
            @ApiResponse(responseCode = "401", description = "Credenciales invalidas")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Intento de login para email: {}", request.getEmail());

        Optional<Athlete> athleteOpt = athleteService.authenticateEntity(
                request.getEmail(), request.getPassword());

        if (athleteOpt.isPresent()) {
            Athlete athlete = athleteOpt.get();
            AuthResponse response = sessionLifecycleService.createSession(athlete);

            logger.info("Login exitoso para email: {}", request.getEmail());
            return ResponseEntity.ok(response);
        }

        logger.warn("Login fallido para email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/auth/google")
    @Operation(summary = "Autenticar con Google",
            description = "Autentica o registra un atleta usando Google OAuth2")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticacion exitosa"),
            @ApiResponse(responseCode = "400", description = "Token de Google invalido"),
            @ApiResponse(responseCode = "401", description = "Email de Google no verificado")
    })
    public ResponseEntity<AuthResponse> authenticateWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        logger.info("Intento de autenticacion con Google");

        try {
            Athlete athlete = googleAuthService.authenticateWithGoogle(request.getIdToken());
            AuthResponse response = sessionLifecycleService.createSession(athlete);

            logger.info("Autenticacion con Google exitosa para email: {}", athlete.getEmail());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error en autenticacion con Google: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error inesperado en autenticacion con Google", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/auth/refresh")
    @Operation(summary = "Rotar sesion", description = "Revoca el refresh token usado y emite un nuevo par de tokens")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            return ResponseEntity.ok(sessionLifecycleService.rotateRefreshToken(request.getRefreshToken()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/auth/logout")
    @Operation(summary = "Revocar sesion", description = "Revoca remotamente la sesion asociada al refresh token")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        sessionLifecycleService.revoke(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    @Operation(summary = "Solicitar recuperacion", description = "Envia un enlace de un solo uso sin revelar si el email existe")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        try {
            sessionLifecycleService.requestPasswordReset(request.getEmail());
        } catch (RuntimeException exception) {
            logger.error("No se pudo entregar la recuperacion de contrasena", exception);
        }
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/confirm")
    @Operation(summary = "Confirmar recuperacion", description = "Consume el token y revoca todas las sesiones previas")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        try {
            sessionLifecycleService.confirmPasswordReset(request.getToken(), request.getNewPassword());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{atletaUuid}")
    @Operation(summary = "Obtener atleta por UUID", description = "Busca un atleta especifico por su UUID unico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Atleta encontrado"),
            @ApiResponse(responseCode = "404", description = "Atleta no encontrado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<AthleteResponse> getAthleteByUuid(
            @Parameter(description = "UUID unico del atleta")
            @PathVariable UUID atletaUuid,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUserUtils.requireSameUser(jwt, atletaUuid);
        logger.debug("Buscando atleta por UUID: {}", atletaUuid);

        Optional<AthleteResponse> athleteOpt = athleteService.findByUuid(atletaUuid);
        return athleteOpt.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-email/{email}")
    @Operation(summary = "Obtener atleta por email", description = "Busca un atleta especifico por su email unico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Atleta encontrado"),
            @ApiResponse(responseCode = "404", description = "Atleta no encontrado")
    })
    public ResponseEntity<AthleteResponse> getAthleteByEmail(
            @Parameter(description = "Email unico del atleta")
            @PathVariable String email) {
        logger.debug("Buscando atleta por email: {}", email);
        Optional<AthleteResponse> athleteOpt = athleteService.findByEmail(email);
        return athleteOpt.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{atletaUuid}")
    @Operation(summary = "Actualizar informacion del atleta", description = "Actualiza el nombre del atleta")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Atleta actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
            @ApiResponse(responseCode = "404", description = "Atleta no encontrado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<AthleteResponse> updateAthlete(
            @Parameter(description = "UUID unico del atleta")
            @PathVariable UUID atletaUuid,
            @Valid @RequestBody UpdateAthleteRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUserUtils.requireSameUser(jwt, atletaUuid);
        logger.info("Actualizando atleta: {}", atletaUuid);

        try {
            AthleteResponse response = athleteService.updateAthlete(atletaUuid, request.getNombre());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error actualizando atleta: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{atletaUuid}/password")
    @Operation(summary = "Cambiar contrasena del atleta",
            description = "Cambia la contrasena del atleta verificando la contrasena actual")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contrasena cambiada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos o contrasena actual incorrecta"),
            @ApiResponse(responseCode = "404", description = "Atleta no encontrado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "UUID unico del atleta")
            @PathVariable UUID atletaUuid,
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUserUtils.requireSameUser(jwt, atletaUuid);
        logger.info("Cambiando contrasena para atleta: {}", atletaUuid);

        try {
            athleteService.changePassword(atletaUuid, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Error cambiando contrasena: {}", e.getMessage());
            if (e.getMessage().contains("No se encontro")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar atletas por nombre",
            description = "Busca atletas que contengan el texto especificado en su nombre")
    public ResponseEntity<List<AthleteResponse>> searchAthletesByName(@RequestParam String nombre) {
        logger.debug("Buscando atletas por nombre: {}", nombre);
        return ResponseEntity.ok(athleteService.searchByName(nombre));
    }

    @GetMapping("/registered-after")
    @Operation(summary = "Obtener atletas registrados despues de una fecha",
            description = "Obtiene todos los atletas registrados despues de la fecha especificada")
    public ResponseEntity<List<AthleteResponse>> getAthletesRegisteredAfter(@RequestParam LocalDateTime fecha) {
        logger.debug("Buscando atletas registrados despues de: {}", fecha);
        return ResponseEntity.ok(athleteService.findRegisteredAfter(fecha));
    }

    @GetMapping("/email-exists/{email}")
    @Operation(summary = "Verificar si email existe",
            description = "Verifica si un email ya esta registrado en el sistema")
    public ResponseEntity<Boolean> emailExists(@PathVariable String email) {
        logger.debug("Verificando existencia de email: {}", email);
        return ResponseEntity.ok(athleteService.emailExists(email));
    }

    @GetMapping("/stats")
    @Operation(summary = "Obtener estadisticas de atletas",
            description = "Obtiene estadisticas generales como el numero total de atletas")
    public ResponseEntity<Long> getTotalAthletes() {
        logger.debug("Obteniendo estadisticas de atletas");
        return ResponseEntity.ok(athleteService.getTotalAthletes());
    }
}
