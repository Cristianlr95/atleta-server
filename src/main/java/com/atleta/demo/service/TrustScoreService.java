package com.atleta.demo.service;

import com.atleta.demo.dto.request.UpdateTrustScoreRequest;
import com.atleta.demo.dto.response.TrustLogResponse;
import com.atleta.demo.dto.response.PlayerProfileResponse;
import com.atleta.demo.entity.*;
import com.atleta.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para la lógica de cálculo y actualización de confianza.
 * Implementa la lógica de negocio para los requisitos 10.1, 10.2, 10.3, 10.4, 10.5
 */
@Service
@Transactional
public class TrustScoreService {

    private final TrustLogRepository trustLogRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final MatchRepository matchRepository;

    // Constantes para el sistema de confianza
    private static final int TRUST_SCORE_INICIAL = 100;
    private static final int TRUST_SCORE_MINIMO = 0;
    private static final int TRUST_SCORE_MAXIMO = 200;

    public TrustScoreService(TrustLogRepository trustLogRepository,
                             PlayerProfileRepository playerProfileRepository,
                             MatchRepository matchRepository) {
        this.trustLogRepository = trustLogRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.matchRepository = matchRepository;
    }

    /**
     * Actualiza el trust score de un jugador y registra el cambio.
     * Requisitos: 10.1, 10.2, 10.3, 10.4, 10.5
     */
    public TrustLogResponse updateTrustScore(UpdateTrustScoreRequest request, UUID changedByUuid) {
        // Buscar el jugador (Requisito 10.1)
        PlayerProfile player = playerProfileRepository.findById(request.getPlayerUuid())
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + request.getPlayerUuid()));

        // Buscar quien realiza el cambio
        PlayerProfile changedBy = null;
        if (changedByUuid != null) {
            changedBy = playerProfileRepository.findById(changedByUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario que realiza el cambio no encontrado: " + changedByUuid));
        }

        // Buscar el partido si se especifica (Requisito 10.2)
        Match match = null;
        if (request.getMatchId() != null) {
            match = matchRepository.findById(request.getMatchId())
                    .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + request.getMatchId()));
        }

        // Calcular nuevo trust score
        Integer trustScoreAnterior = player.getTrustScore();
        Integer nuevoTrustScore = calculateNewTrustScore(trustScoreAnterior, request.getCambio());

        // Crear el log de cambio (Requisito 10.3, 10.4)
        TrustLog trustLog = new TrustLog(
                player,
                match,
                request.getCambio(),
                trustScoreAnterior,
                nuevoTrustScore,
                request.getMotivo(),
                changedBy
        );

        trustLog = trustLogRepository.save(trustLog);

        // Actualizar el perfil del jugador (Requisito 10.5)
        player.setTrustScore(nuevoTrustScore);
        playerProfileRepository.save(player);

        return convertToTrustLogResponse(trustLog);
    }

    /**
     * Actualiza automáticamente el trust score basado en comportamiento en partidos.
     */
    public TrustLogResponse updateTrustScoreAutomatic(UUID playerUuid, Long matchId, Integer cambio, String motivo) {
        PlayerProfile player = playerProfileRepository.findById(playerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + playerUuid));

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));

        Integer trustScoreAnterior = player.getTrustScore();
        Integer nuevoTrustScore = calculateNewTrustScore(trustScoreAnterior, cambio);

        // Crear log automático (sin changedBy)
        TrustLog trustLog = new TrustLog(
                player,
                cambio,
                trustScoreAnterior,
                nuevoTrustScore,
                motivo
        );
        trustLog.setMatch(match);

        trustLog = trustLogRepository.save(trustLog);

        // Actualizar perfil
        player.setTrustScore(nuevoTrustScore);
        playerProfileRepository.save(player);

        return convertToTrustLogResponse(trustLog);
    }

    /**
     * Obtiene el historial completo de cambios de confianza de un jugador.
     * Requisito 10.4
     */
    @Transactional(readOnly = true)
    public List<TrustLogResponse> getTrustHistory(UUID playerUuid) {
        PlayerProfile player = playerProfileRepository.findById(playerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + playerUuid));

        return trustLogRepository.findByPlayerOrderByCreatedAtDesc(player).stream()
                .map(this::convertToTrustLogResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los cambios de confianza relacionados con un partido específico.
     */
    @Transactional(readOnly = true)
    public List<TrustLogResponse> getTrustChangesByMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));

        return trustLogRepository.findByMatch(match).stream()
                .map(this::convertToTrustLogResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene solo los cambios positivos de confianza de un jugador.
     */
    @Transactional(readOnly = true)
    public List<TrustLogResponse> getPositiveTrustChanges(UUID playerUuid) {
        PlayerProfile player = playerProfileRepository.findById(playerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + playerUuid));

        return trustLogRepository.findPositiveChangesByPlayer(player).stream()
                .map(this::convertToTrustLogResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene solo los cambios negativos de confianza de un jugador.
     */
    @Transactional(readOnly = true)
    public List<TrustLogResponse> getNegativeTrustChanges(UUID playerUuid) {
        PlayerProfile player = playerProfileRepository.findById(playerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + playerUuid));

        return trustLogRepository.findNegativeChangesByPlayer(player).stream()
                .map(this::convertToTrustLogResponse)
                .collect(Collectors.toList());
    }

    /**
     * Calcula estadísticas de confianza de un jugador.
     */
    @Transactional(readOnly = true)
    public TrustScoreStats getTrustScoreStats(UUID playerUuid) {
        PlayerProfile player = playerProfileRepository.findById(playerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + playerUuid));

        Object[] stats = trustLogRepository.getTrustChangeStatsByPlayer(player);
        
        Long totalCambios = (Long) stats[0];
        Long cambiosPositivos = (Long) stats[1];
        Long cambiosNegativos = (Long) stats[2];
        Long sumaTotal = (Long) stats[3];

        return new TrustScoreStats(
                player.getTrustScore(),
                totalCambios != null ? totalCambios.intValue() : 0,
                cambiosPositivos != null ? cambiosPositivos.intValue() : 0,
                cambiosNegativos != null ? cambiosNegativos.intValue() : 0,
                sumaTotal != null ? sumaTotal.intValue() : 0
        );
    }

    /**
     * Recalcula el trust score de un jugador basado en todo su historial.
     * Útil para correcciones o auditorías.
     */
    public PlayerProfileResponse recalculateTrustScore(UUID playerUuid) {
        PlayerProfile player = playerProfileRepository.findById(playerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + playerUuid));

        // Obtener la suma total de cambios
        Long totalChanges = trustLogRepository.getTotalChangesByPlayer(player);
        int calculatedScore = TRUST_SCORE_INICIAL + (totalChanges != null ? totalChanges.intValue() : 0);

        // Aplicar límites
        calculatedScore = Math.max(TRUST_SCORE_MINIMO, Math.min(TRUST_SCORE_MAXIMO, calculatedScore));

        // Actualizar si hay diferencia
        if (!Integer.valueOf(calculatedScore).equals(player.getTrustScore())) {
            Integer oldScore = player.getTrustScore();
            player.setTrustScore(calculatedScore);
            playerProfileRepository.save(player);

            // Registrar la corrección
            TrustLog correctionLog = new TrustLog(
                    player,
                    calculatedScore - oldScore,
                    oldScore,
                    calculatedScore,
                    "Recálculo automático del trust score"
            );
            trustLogRepository.save(correctionLog);
        }

        return convertToPlayerProfileResponse(player);
    }

    /**
     * Aplica penalizaciones automáticas por comportamientos específicos.
     */
    public TrustLogResponse applyAutomaticPenalty(UUID playerUuid, Long matchId, PenaltyType penaltyType) {
        int cambio = getPenaltyValue(penaltyType);
        String motivo = getPenaltyReason(penaltyType);

        return updateTrustScoreAutomatic(playerUuid, matchId, cambio, motivo);
    }

    /**
     * Aplica bonificaciones automáticas por comportamientos positivos.
     */
    public TrustLogResponse applyAutomaticBonus(UUID playerUuid, Long matchId, BonusType bonusType) {
        int cambio = getBonusValue(bonusType);
        String motivo = getBonusReason(bonusType);

        return updateTrustScoreAutomatic(playerUuid, matchId, cambio, motivo);
    }

    // Métodos privados de utilidad

    private Integer calculateNewTrustScore(Integer currentScore, Integer change) {
        int newScore = currentScore + change;
        return Math.max(TRUST_SCORE_MINIMO, Math.min(TRUST_SCORE_MAXIMO, newScore));
    }

    private int getPenaltyValue(PenaltyType penaltyType) {
        switch (penaltyType) {
            case NO_SHOW: return -10;
            case LATE_CANCELLATION: return -5;
            case UNSPORTSMANLIKE_CONDUCT: return -15;
            case REPEATED_OFFENSE: return -20;
            default: return -5;
        }
    }

    private String getPenaltyReason(PenaltyType penaltyType) {
        switch (penaltyType) {
            case NO_SHOW: return "No se presentó al partido confirmado";
            case LATE_CANCELLATION: return "Cancelación tardía de participación";
            case UNSPORTSMANLIKE_CONDUCT: return "Conducta antideportiva";
            case REPEATED_OFFENSE: return "Reincidencia en faltas";
            default: return "Penalización automática";
        }
    }

    private int getBonusValue(BonusType bonusType) {
        switch (bonusType) {
            case CONSISTENT_PARTICIPATION: return 5;
            case GOOD_SPORTSMANSHIP: return 10;
            case MATCH_COMPLETION: return 3;
            case LEADERSHIP: return 8;
            default: return 3;
        }
    }

    private String getBonusReason(BonusType bonusType) {
        switch (bonusType) {
            case CONSISTENT_PARTICIPATION: return "Participación consistente en partidos";
            case GOOD_SPORTSMANSHIP: return "Buen comportamiento deportivo";
            case MATCH_COMPLETION: return "Completó el partido exitosamente";
            case LEADERSHIP: return "Liderazgo positivo en el equipo";
            default: return "Bonificación automática";
        }
    }

    // Métodos de conversión

    private TrustLogResponse convertToTrustLogResponse(TrustLog trustLog) {
        TrustLogResponse response = new TrustLogResponse();
        response.setId(trustLog.getId());
        response.setCambio(trustLog.getCambio());
        response.setMotivo(trustLog.getMotivo());
        response.setCreatedAt(trustLog.getCreatedAt());

        if (trustLog.getPlayer() != null) {
            response.setPlayer(convertToPlayerProfileResponse(trustLog.getPlayer()));
        }

        // Note: Match conversion simplified to avoid circular dependencies
        // In a real implementation, you might want to create a simplified MatchResponse
        // or handle this differently to avoid loading too much data

        return response;
    }

    private PlayerProfileResponse convertToPlayerProfileResponse(PlayerProfile profile) {
        PlayerProfileResponse response = new PlayerProfileResponse();
        response.setAtletaUuid(profile.getAtletaUuid());
        response.setAlias(profile.getAlias());
        response.setGenero(profile.getAthlete() != null ? profile.getAthlete().getGenero() : null);
        response.setTrustScore(profile.getTrustScore());
        response.setCreatedAt(profile.getCreatedAt());

        return response;
    }

    // Enums para tipos de penalizaciones y bonificaciones

    public enum PenaltyType {
        NO_SHOW,
        LATE_CANCELLATION,
        UNSPORTSMANLIKE_CONDUCT,
        REPEATED_OFFENSE
    }

    public enum BonusType {
        CONSISTENT_PARTICIPATION,
        GOOD_SPORTSMANSHIP,
        MATCH_COMPLETION,
        LEADERSHIP
    }

    // Clase para estadísticas de trust score

    public static class TrustScoreStats {
        private final Integer currentTrustScore;
        private final Integer totalChanges;
        private final Integer positiveChanges;
        private final Integer negativeChanges;
        private final Integer totalChangeValue;

        public TrustScoreStats(Integer currentTrustScore, Integer totalChanges, 
                              Integer positiveChanges, Integer negativeChanges, Integer totalChangeValue) {
            this.currentTrustScore = currentTrustScore;
            this.totalChanges = totalChanges;
            this.positiveChanges = positiveChanges;
            this.negativeChanges = negativeChanges;
            this.totalChangeValue = totalChangeValue;
        }

        // Getters
        public Integer getCurrentTrustScore() { return currentTrustScore; }
        public Integer getTotalChanges() { return totalChanges; }
        public Integer getPositiveChanges() { return positiveChanges; }
        public Integer getNegativeChanges() { return negativeChanges; }
        public Integer getTotalChangeValue() { return totalChangeValue; }
        
        public Double getPositiveChangePercentage() {
            return totalChanges > 0 ? (positiveChanges.doubleValue() / totalChanges.doubleValue()) * 100.0 : 0.0;
        }
        
        public Double getNegativeChangePercentage() {
            return totalChanges > 0 ? (negativeChanges.doubleValue() / totalChanges.doubleValue()) * 100.0 : 0.0;
        }
    }
}
