package com.atleta.demo.service;

import com.atleta.demo.dto.request.CreatePlayerProfileRequest;
import com.atleta.demo.dto.request.AddPlayerPositionRequest;
import com.atleta.demo.dto.request.UpdateTrustScoreRequest;
import com.atleta.demo.dto.request.UpdatePlayerProfileRequest;
import com.atleta.demo.dto.response.PlayerProfileResponse;
import com.atleta.demo.dto.response.PlayerPositionResponse;
import com.atleta.demo.dto.response.PositionResponse;
import com.atleta.demo.dto.response.TrustLogResponse;
import com.atleta.demo.entity.*;
import com.atleta.demo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de perfiles de jugadores.
 * Implementa la lógica de negocio para gestión de perfiles, posiciones y trust score.
 * 
 * Requisitos implementados:
 * - 2.1: Asociación de perfil a atleta existente
 * - 2.2: Trust score inicial de 100
 * - 2.3: Alias único para contexto de fútbol
 * - 2.4: Relación uno-a-uno entre atleta y perfil
 * - 2.5: Registro de cambios de trust score en trust_logs
 */
@Service
@Transactional
public class PlayerProfileService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerProfileService.class);

    private final PlayerProfileRepository playerProfileRepository;
    private final AthleteRepository athleteRepository;
    private final PlayerPositionRepository playerPositionRepository;
    private final PositionRepository positionRepository;
    private final TrustScoreService trustScoreService;

    public PlayerProfileService(PlayerProfileRepository playerProfileRepository,
                               AthleteRepository athleteRepository,
                               PlayerPositionRepository playerPositionRepository,
                               PositionRepository positionRepository,
                               TrustScoreService trustScoreService) {
        this.playerProfileRepository = playerProfileRepository;
        this.athleteRepository = athleteRepository;
        this.playerPositionRepository = playerPositionRepository;
        this.positionRepository = positionRepository;
        this.trustScoreService = trustScoreService;
    }

    /**
     * Crea un nuevo perfil de jugador asociado a un atleta.
     * 
     * @param request Datos del perfil a crear
     * @return PlayerProfileResponse con la información del perfil creado
     * @throws IllegalArgumentException si el atleta no existe o ya tiene perfil
     */
    public PlayerProfileResponse createPlayerProfile(CreatePlayerProfileRequest request) {
        logger.info("Creando perfil de jugador para atleta: {}", request.getAtletaUuid());

        // Requisito 2.1: Verificar que el atleta existe
        Athlete athlete = athleteRepository.findById(request.getAtletaUuid())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró atleta con UUID: " + request.getAtletaUuid()));

        // La fuente de verdad del genero vive en Athlete. Solo usamos el request
        // como compatibilidad para atletas legacy que aun no tengan ese dato.
        if (athlete.getGenero() == null) {
            if (request.getGenero() == null) {
                throw new IllegalArgumentException("Debe definir genero al crear el atleta antes de configurar el jugador");
            }
            athlete.setGenero(request.getGenero());
            athleteRepository.save(athlete);
        }

        // Requisito 2.4: Verificar que no tenga ya un perfil (relación uno-a-uno)
        if (playerProfileRepository.existsById(request.getAtletaUuid())) {
            throw new IllegalArgumentException("El atleta ya tiene un perfil de jugador");
        }

        // Requisito 2.3: Verificar que el alias sea único si se proporciona
        if (request.getAlias() != null && !request.getAlias().trim().isEmpty()) {
            if (playerProfileRepository.existsByAlias(request.getAlias().trim())) {
                throw new IllegalArgumentException("Ya existe un jugador con el alias: " + request.getAlias());
            }
        }

        // Crear nuevo perfil
        // Requisito 2.2: Trust score inicial de 100 (se asigna automáticamente en la entidad)
        PlayerProfile playerProfile = new PlayerProfile(athlete, request.getAlias());

        // Guardar en base de datos
        PlayerProfile savedProfile = playerProfileRepository.save(playerProfile);

        logger.info("Perfil de jugador creado exitosamente para atleta: {}", request.getAtletaUuid());

        return convertToResponse(savedProfile);
    }

    /**
     * Busca un perfil de jugador por UUID del atleta.
     * 
     * @param atletaUuid UUID del atleta
     * @return Optional con el perfil si existe
     */
    @Transactional(readOnly = true)
    public Optional<PlayerProfileResponse> findByAtletaUuid(UUID atletaUuid) {
        logger.debug("Buscando perfil de jugador por UUID: {}", atletaUuid);
        
        return playerProfileRepository.findById(atletaUuid)
                .map(this::convertToResponse);
    }

    /**
     * Busca un perfil de jugador por alias.
     * 
     * @param alias Alias del jugador
     * @return Optional con el perfil si existe
     */
    @Transactional(readOnly = true)
    public Optional<PlayerProfileResponse> findByAlias(String alias) {
        logger.debug("Buscando perfil de jugador por alias: {}", alias);
        
        return playerProfileRepository.findByAlias(alias)
                .map(this::convertToResponse);
    }

    /**
     * Actualiza el alias de un jugador.
     * 
     * @param atletaUuid UUID del atleta
     * @param newAlias Nuevo alias
     * @return PlayerProfileResponse actualizado
     * @throws IllegalArgumentException si el perfil no existe o el alias ya está en uso
     */
    public PlayerProfileResponse updateAlias(UUID atletaUuid, String newAlias) {
        logger.info("Actualizando alias para jugador: {}", atletaUuid);

        PlayerProfile profile = playerProfileRepository.findById(atletaUuid)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró perfil de jugador con UUID: " + atletaUuid));

        // Requisito 2.3: Verificar que el alias sea único
        if (newAlias != null && !newAlias.trim().isEmpty()) {
            String trimmedAlias = newAlias.trim();
            if (!trimmedAlias.equals(profile.getAlias()) && playerProfileRepository.existsByAlias(trimmedAlias)) {
                throw new IllegalArgumentException("Ya existe un jugador con el alias: " + trimmedAlias);
            }
            profile.setAlias(trimmedAlias);
        } else {
            profile.setAlias(null);
        }

        PlayerProfile updatedProfile = playerProfileRepository.save(profile);

        logger.info("Alias actualizado exitosamente para jugador: {}", atletaUuid);

        return convertToResponse(updatedProfile);
    }

    public PlayerProfileResponse updateProfile(UUID atletaUuid, UpdatePlayerProfileRequest request) {
        PlayerProfile profile = playerProfileRepository.findById(atletaUuid)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro perfil de jugador con UUID: " + atletaUuid));

        String normalizedName = null;
        if (request.getNombre() != null) {
            normalizedName = request.getNombre().trim();
            if (normalizedName.isEmpty()) {
                throw new IllegalArgumentException("El nombre no puede estar vacio");
            }
        }

        String normalizedAlias = null;
        if (request.getAlias() != null) {
            normalizedAlias = request.getAlias().trim();
            if (normalizedAlias.isEmpty()) {
                throw new IllegalArgumentException("El alias no puede estar vacio");
            }
            if (!normalizedAlias.equals(profile.getAlias()) && playerProfileRepository.existsByAlias(normalizedAlias)) {
                throw new IllegalArgumentException("Ya existe un jugador con el alias: " + normalizedAlias);
            }
        }

        List<Long> positionIds = request.getPositionIds();
        Map<Long, Position> positionsById = null;
        Map<Long, Integer> xpByPosition = null;
        if (request.getPositionIds() != null) {
            if (positionIds.size() != 3 || positionIds.stream().distinct().count() != 3) {
                throw new IllegalArgumentException("Debes seleccionar exactamente 3 posiciones distintas");
            }

            Map<Long, Position> loadedPositions = new LinkedHashMap<>();
            positionRepository.findAllById(positionIds)
                    .forEach(position -> loadedPositions.put(position.getId(), position));
            if (loadedPositions.size() != 3) {
                throw new IllegalArgumentException("Una o mas posiciones no existen");
            }
            positionsById = loadedPositions;

            xpByPosition = playerPositionRepository.findByPlayerOrderByPrioridad(profile)
                    .stream()
                    .collect(Collectors.toMap(item -> item.getPosition().getId(), PlayerPosition::getXp));
        }

        if (normalizedName != null) {
            profile.getAthlete().setNombre(normalizedName);
        }
        if (normalizedAlias != null) {
            profile.setAlias(normalizedAlias);
        }

        List<PlayerPosition> updatedPositions = null;
        if (positionIds != null && positionsById != null && xpByPosition != null) {
            playerPositionRepository.deleteByPlayer(profile);
            playerPositionRepository.flush();

            updatedPositions = new java.util.ArrayList<>();
            for (int index = 0; index < positionIds.size(); index++) {
                Long positionId = positionIds.get(index);
                updatedPositions.add(new PlayerPosition(
                        profile,
                        positionsById.get(positionId),
                        index + 1,
                        xpByPosition.getOrDefault(positionId, 0)
                ));
            }
            updatedPositions = playerPositionRepository.saveAll(updatedPositions);
        }

        if (normalizedName != null) {
            athleteRepository.save(profile.getAthlete());
        }
        PlayerProfile updatedProfile = playerProfileRepository.save(profile);
        PlayerProfileResponse response = convertToResponse(updatedProfile);
        if (updatedPositions != null) {
            response.setPositions(updatedPositions.stream().map(this::convertToPlayerPositionResponse).toList());
        }
        return response;
    }

    /**
     * Agrega una posición a un jugador con prioridad específica.
     * 
     * @param request Datos de la posición a agregar
     * @return PlayerPositionResponse con la información de la posición agregada
     * @throws IllegalArgumentException si el jugador no existe, la posición no existe, o la prioridad ya está ocupada
     */
    public PlayerPositionResponse addPlayerPosition(AddPlayerPositionRequest request) {
        logger.info("Agregando posición {} con prioridad {} al jugador: {}", 
                   request.getPositionId(), request.getPrioridad(), request.getPlayerUuid());

        // Verificar que el jugador existe
        PlayerProfile player = playerProfileRepository.findById(request.getPlayerUuid())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró jugador con UUID: " + request.getPlayerUuid()));

        // Verificar que la posición existe
        Position position = positionRepository.findById(request.getPositionId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró posición con ID: " + request.getPositionId()));

        // Requisito 3.4: Verificar que las prioridades sean únicas por jugador
        if (playerPositionRepository.existsByPlayerAndPrioridad(player, request.getPrioridad())) {
            throw new IllegalArgumentException("El jugador ya tiene una posición con prioridad: " + request.getPrioridad());
        }

        // Verificar que el jugador no tenga ya esta posición
        if (playerPositionRepository.findByPlayerAndPosition(player, position).isPresent()) {
            throw new IllegalArgumentException("El jugador ya tiene asignada esta posición");
        }

        // Crear nueva asignación de posición
        PlayerPosition playerPosition = new PlayerPosition(player, position, request.getPrioridad());
        PlayerPosition savedPlayerPosition = playerPositionRepository.save(playerPosition);

        logger.info("Posición agregada exitosamente al jugador: {}", request.getPlayerUuid());

        return convertToPlayerPositionResponse(savedPlayerPosition);
    }

    /**
     * Remueve una posición de un jugador.
     * 
     * @param atletaUuid UUID del atleta
     * @param positionId ID de la posición a remover
     * @throws IllegalArgumentException si el jugador no existe o no tiene la posición
     */
    public void removePlayerPosition(UUID atletaUuid, Long positionId) {
        logger.info("Removiendo posición {} del jugador: {}", positionId, atletaUuid);

        PlayerProfile player = playerProfileRepository.findById(atletaUuid)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró jugador con UUID: " + atletaUuid));

        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró posición con ID: " + positionId));

        PlayerPosition playerPosition = playerPositionRepository.findByPlayerAndPosition(player, position)
                .orElseThrow(() -> new IllegalArgumentException("El jugador no tiene asignada esta posición"));

        playerPositionRepository.delete(playerPosition);

        logger.info("Posición removida exitosamente del jugador: {}", atletaUuid);
    }

    /**
     * Obtiene todas las posiciones de un jugador ordenadas por prioridad.
     * 
     * @param atletaUuid UUID del atleta
     * @return Lista de posiciones del jugador
     */
    @Transactional(readOnly = true)
    public List<PlayerPositionResponse> getPlayerPositions(UUID atletaUuid) {
        logger.debug("Obteniendo posiciones del jugador: {}", atletaUuid);

        return playerPositionRepository.findByPlayerAtletaUuidOrderByPrioridad(atletaUuid)
                .stream()
                .map(this::convertToPlayerPositionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Actualiza el trust score de un jugador.
     * 
     * @param request Datos del cambio de trust score
     * @return PlayerProfileResponse actualizado
     * @throws IllegalArgumentException si el jugador no existe
     */
    public PlayerProfileResponse updateTrustScore(UpdateTrustScoreRequest request) {
        logger.info("Actualizando trust score del jugador: {} con cambio: {}", 
                   request.getPlayerUuid(), request.getCambio());

        trustScoreService.updateTrustScore(request, null);
        PlayerProfile updatedPlayer = playerProfileRepository.findById(request.getPlayerUuid())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró jugador con UUID: " + request.getPlayerUuid()));

        logger.info("Trust score actualizado exitosamente para jugador: {} (nuevo: {})",
                   request.getPlayerUuid(), updatedPlayer.getTrustScore());

        return convertToResponse(updatedPlayer);
    }

    /**
     * Obtiene el historial de cambios de trust score de un jugador.
     * 
     * @param atletaUuid UUID del atleta
     * @return Lista de logs de trust score
     */
    @Transactional(readOnly = true)
    public List<TrustLogResponse> getTrustScoreHistory(UUID atletaUuid) {
        logger.debug("Obteniendo historial de trust score del jugador: {}", atletaUuid);

        return trustScoreService.getTrustHistory(atletaUuid);
    }

    /**
     * Busca jugadores por rango de trust score.
     * 
     * @param minScore Puntuación mínima
     * @param maxScore Puntuación máxima
     * @return Lista de jugadores en el rango
     */
    @Transactional(readOnly = true)
    public List<PlayerProfileResponse> findByTrustScoreRange(Integer minScore, Integer maxScore) {
        logger.debug("Buscando jugadores con trust score entre {} y {}", minScore, maxScore);

        return playerProfileRepository.findByTrustScoreBetween(minScore, maxScore)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Busca jugadores por nombre del atleta.
     * 
     * @param nombre Nombre o parte del nombre
     * @return Lista de jugadores que coinciden
     */
    @Transactional(readOnly = true)
    public List<PlayerProfileResponse> searchByAthleteName(String nombre) {
        logger.debug("Buscando jugadores por nombre de atleta: {}", nombre);

        return playerProfileRepository.findByAthleteNombreContainingIgnoreCase(nombre)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Agrega experiencia (XP) a una posición específica de un jugador.
     * 
     * @param atletaUuid UUID del atleta
     * @param positionId ID de la posición
     * @param xpToAdd XP a agregar
     * @throws IllegalArgumentException si el jugador no tiene la posición
     */
    public void addExperienceToPosition(UUID atletaUuid, Long positionId, Integer xpToAdd) {
        logger.info("Agregando {} XP a la posición {} del jugador: {}", xpToAdd, positionId, atletaUuid);

        PlayerProfile player = playerProfileRepository.findById(atletaUuid)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró jugador con UUID: " + atletaUuid));

        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró posición con ID: " + positionId));

        PlayerPosition playerPosition = playerPositionRepository.findByPlayerAndPosition(player, position)
                .orElseThrow(() -> new IllegalArgumentException("El jugador no tiene asignada esta posición"));

        // Requisito 3.3: Mantener contador de experiencia por posición
        playerPosition.addXp(xpToAdd);
        playerPositionRepository.save(playerPosition);

        logger.info("XP agregada exitosamente a la posición {} del jugador: {}", positionId, atletaUuid);
    }

    /**
     * Convierte una entidad PlayerProfile a PlayerProfileResponse.
     * 
     * @param playerProfile Entidad a convertir
     * @return DTO de respuesta
     */
    private PlayerProfileResponse convertToResponse(PlayerProfile playerProfile) {
        PlayerProfileResponse response = new PlayerProfileResponse(
                playerProfile.getAtletaUuid(),
                playerProfile.getAlias(),
                playerProfile.getAthlete() != null ? playerProfile.getAthlete().getGenero() : null,
                playerProfile.getTrustScore(),
                playerProfile.getCreatedAt()
        );
        response.setNombre(playerProfile.getAthlete() != null ? playerProfile.getAthlete().getNombre() : null);

        // Incluir posiciones si están cargadas
        if (playerProfile.getPositions() != null && !playerProfile.getPositions().isEmpty()) {
            List<PlayerPositionResponse> positions = playerProfile.getPositions()
                    .stream()
                    .map(this::convertToPlayerPositionResponse)
                    .collect(Collectors.toList());
            response.setPositions(positions);
        }

        return response;
    }

    /**
     * Convierte una entidad PlayerPosition a PlayerPositionResponse.
     * 
     * @param playerPosition Entidad a convertir
     * @return DTO de respuesta
     */
    private PlayerPositionResponse convertToPlayerPositionResponse(PlayerPosition playerPosition) {
        PositionResponse positionResponse = new PositionResponse(
                playerPosition.getPosition().getId(),
                playerPosition.getPosition().getNombre()
        );

        return new PlayerPositionResponse(
                playerPosition.getId(),
                positionResponse,
                playerPosition.getPrioridad(),
                playerPosition.getXp()
        );
    }

}
