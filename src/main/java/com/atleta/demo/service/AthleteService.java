package com.atleta.demo.service;

import com.atleta.demo.dto.request.CreateAthleteRequest;
import com.atleta.demo.dto.response.AthleteResponse;
import com.atleta.demo.entity.Athlete;
import com.atleta.demo.repository.AthleteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de atletas.
 * Implementa la lógica de negocio para registro, autenticación y gestión de atletas.
 * 
 * Requisitos implementados:
 * - 1.1: UUID único como identificador principal
 * - 1.2: Email único en todo el sistema
 * - 1.3: Almacenamiento seguro del hash de contraseña
 * - 1.4: Registro automático de fecha de creación
 * - 1.5: Validación de nombre no vacío y formato válido
 */
@Service
@Transactional
public class AthleteService {

    private static final Logger logger = LoggerFactory.getLogger(AthleteService.class);

    private final AthleteRepository athleteRepository;
    private final PasswordEncoder passwordEncoder;

    public AthleteService(AthleteRepository athleteRepository, PasswordEncoder passwordEncoder) {
        this.athleteRepository = athleteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un nuevo atleta en el sistema.
     * 
     * @param request Datos del atleta a registrar
     * @return AthleteResponse con la información del atleta creado
     * @throws IllegalArgumentException si el email ya existe
     */
    public AthleteResponse registerAthlete(CreateAthleteRequest request) {
        logger.info("Registrando nuevo atleta con email: {}", request.getEmail());

        // Requisito 1.2: Validar que el email sea único
        if (athleteRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Ya existe un atleta con el email: " + request.getEmail());
        }

        // Requisito 1.3: Hashear la contraseña de forma segura
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Crear nueva entidad Athlete
        // Requisito 1.1: UUID se genera automáticamente
        // Requisito 1.4: Fecha de creación se registra automáticamente
        // Requisito 1.5: Validación de nombre se hace en la entidad
        Athlete athlete = new Athlete(request.getEmail(), hashedPassword, request.getNombre(), request.getGenero());

        // Guardar en base de datos y forzar flush para asegurar que @CreationTimestamp funcione
        Athlete savedAthlete = athleteRepository.saveAndFlush(athlete);

        logger.info("Atleta registrado exitosamente con UUID: {}", savedAthlete.getAtletaUuid());

        return convertToResponse(savedAthlete);
    }

    /**
     * Busca un atleta por su UUID.
     * 
     * @param atletaUuid UUID del atleta
     * @return Optional con el atleta si existe
     */
    @Transactional(readOnly = true)
    public Optional<AthleteResponse> findByUuid(UUID atletaUuid) {
        logger.debug("Buscando atleta por UUID: {}", atletaUuid);
        
        return athleteRepository.findById(atletaUuid)
                .map(this::convertToResponse);
    }

    /**
     * Busca un atleta por su email.
     * 
     * @param email Email del atleta
     * @return Optional con el atleta si existe
     */
    @Transactional(readOnly = true)
    public Optional<AthleteResponse> findByEmail(String email) {
        logger.debug("Buscando atleta por email: {}", email);
        
        return athleteRepository.findByEmail(email)
                .map(this::convertToResponse);
    }

    /**
     * Autentica un atleta con email y contraseña.
     * 
     * @param email Email del atleta
     * @param password Contraseña en texto plano
     * @return Optional con el atleta si las credenciales son válidas
     */
    @Transactional(readOnly = true)
    public Optional<AthleteResponse> authenticate(String email, String password) {
        return authenticateEntity(email, password).map(this::convertToResponse);
    }

    /**
     * Autentica un atleta con email y contrase\u00f1a y retorna la entidad completa.
     * 
     * @param email Email del atleta
     * @param password Contrase\u00f1a en texto plano
     * @return Optional con la entidad Athlete si las credenciales son v\u00e1lidas
     */
    @Transactional(readOnly = true)
    public Optional<Athlete> authenticateEntity(String email, String password) {
        logger.debug("Intentando autenticar atleta con email: {}", email);

        Optional<Athlete> athleteOpt = athleteRepository.findByEmail(email);
        
        if (athleteOpt.isPresent()) {
            Athlete athlete = athleteOpt.get();
            
            // Verificar contrase\u00f1a
            if (passwordEncoder.matches(password, athlete.getPasswordHash())) {
                logger.info("Autenticaci\u00f3n exitosa para atleta: {}", email);
                return Optional.of(athlete);
            } else {
                logger.warn("Contrase\u00f1a incorrecta para atleta: {}", email);
            }
        } else {
            logger.warn("No se encontr\u00f3 atleta con email: {}", email);
        }

        return Optional.empty();
    }

    /**
     * Actualiza la información básica de un atleta.
     * 
     * @param atletaUuid UUID del atleta
     * @param nombre Nuevo nombre del atleta
     * @return AthleteResponse actualizado
     * @throws IllegalArgumentException si el atleta no existe
     */
    public AthleteResponse updateAthlete(UUID atletaUuid, String nombre) {
        logger.info("Actualizando atleta con UUID: {}", atletaUuid);

        Athlete athlete = athleteRepository.findById(atletaUuid)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró atleta con UUID: " + atletaUuid));

        // Requisito 1.5: Validar nombre no vacío
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }

        athlete.setNombre(nombre.trim());
        Athlete updatedAthlete = athleteRepository.save(athlete);

        logger.info("Atleta actualizado exitosamente: {}", atletaUuid);

        return convertToResponse(updatedAthlete);
    }

    /**
     * Cambia la contraseña de un atleta.
     * 
     * @param atletaUuid UUID del atleta
     * @param currentPassword Contraseña actual
     * @param newPassword Nueva contraseña
     * @throws IllegalArgumentException si el atleta no existe o la contraseña actual es incorrecta
     */
    public void changePassword(UUID atletaUuid, String currentPassword, String newPassword) {
        logger.info("Cambiando contraseña para atleta: {}", atletaUuid);

        Athlete athlete = athleteRepository.findById(atletaUuid)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró atleta con UUID: " + atletaUuid));

        // Verificar contraseña actual
        if (!passwordEncoder.matches(currentPassword, athlete.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }

        // Requisito 1.3: Hashear nueva contraseña de forma segura
        String hashedNewPassword = passwordEncoder.encode(newPassword);
        athlete.setPasswordHash(hashedNewPassword);

        athleteRepository.save(athlete);

        logger.info("Contraseña cambiada exitosamente para atleta: {}", atletaUuid);
    }

    /**
     * Busca atletas por nombre (búsqueda parcial).
     * 
     * @param nombre Nombre o parte del nombre a buscar
     * @return Lista de atletas que coinciden
     */
    @Transactional(readOnly = true)
    public List<AthleteResponse> searchByName(String nombre) {
        logger.debug("Buscando atletas por nombre: {}", nombre);

        return athleteRepository.findByNombreContainingIgnoreCase(nombre)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todos los atletas registrados después de una fecha.
     * 
     * @param fecha Fecha de referencia
     * @return Lista de atletas registrados después de la fecha
     */
    @Transactional(readOnly = true)
    public List<AthleteResponse> findRegisteredAfter(LocalDateTime fecha) {
        logger.debug("Buscando atletas registrados después de: {}", fecha);

        return athleteRepository.findByCreatedAtAfter(fecha)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Verifica si un email ya está registrado.
     * 
     * @param email Email a verificar
     * @return true si el email ya existe, false en caso contrario
     */
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return athleteRepository.existsByEmail(email);
    }

    /**
     * Obtiene el número total de atletas registrados.
     * 
     * @return Número total de atletas
     */
    @Transactional(readOnly = true)
    public long getTotalAthletes() {
        return athleteRepository.countAllAthletes();
    }

    /**
     * Convierte una entidad Athlete a AthleteResponse.
     * 
     * @param athlete Entidad a convertir
     * @return DTO de respuesta
     */
    private AthleteResponse convertToResponse(Athlete athlete) {
        AthleteResponse response = new AthleteResponse(
                athlete.getAtletaUuid(),
                athlete.getEmail(),
                athlete.getNombre(),
                athlete.getGenero(),
                athlete.getCreatedAt()
        );

        // Si tiene perfil de jugador, incluirlo en la respuesta
        if (athlete.getPlayerProfile() != null) {
            // Nota: La conversión del PlayerProfile se hará en PlayerProfileService
            // para evitar dependencias circulares
        }

        return response;
    }
}
