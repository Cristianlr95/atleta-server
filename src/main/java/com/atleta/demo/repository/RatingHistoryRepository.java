package com.atleta.demo.repository;

import com.atleta.demo.entity.RatingHistory;
import com.atleta.demo.enums.MatchResultType;
import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio para la entidad RatingHistory.
 * Proporciona operaciones CRUD básicas y consultas personalizadas
 * para el análisis del historial de calificaciones de jugadores.
 */
@Repository
public interface RatingHistoryRepository extends JpaRepository<RatingHistory, Long> {

    /**
     * Busca todo el historial de calificaciones de un jugador específico
     * @param playerProfileId UUID del perfil del jugador
     * @return Lista del historial de calificaciones ordenado por fecha descendente
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.playerRating.playerProfile.atletaUuid = :playerProfileId ORDER BY rh.createdAt DESC")
    List<RatingHistory> findByPlayerProfileId(@Param("playerProfileId") UUID playerProfileId);

    /**
     * Busca el historial de calificaciones de un jugador para un rol específico
     * @param playerProfileId UUID del perfil del jugador
     * @param roleType Tipo de rol
     * @return Lista del historial de calificaciones para el rol ordenado por fecha descendente
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.playerRating.playerProfile.atletaUuid = :playerProfileId AND rh.playerRating.roleType = :roleType ORDER BY rh.createdAt DESC")
    List<RatingHistory> findByPlayerProfileIdAndRoleType(
            @Param("playerProfileId") UUID playerProfileId,
            @Param("roleType") RoleType roleType);

    /**
     * Busca el historial de calificaciones de un jugador para un nivel de prioridad específico
     * @param playerProfileId UUID del perfil del jugador
     * @param priorityLevel Nivel de prioridad
     * @return Lista del historial de calificaciones para el nivel de prioridad ordenado por fecha descendente
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.playerRating.playerProfile.atletaUuid = :playerProfileId AND rh.playerRating.priorityLevel = :priorityLevel ORDER BY rh.createdAt DESC")
    List<RatingHistory> findByPlayerProfileIdAndPriorityLevel(
            @Param("playerProfileId") UUID playerProfileId,
            @Param("priorityLevel") PriorityLevel priorityLevel);

    /**
     * Busca el historial de calificaciones de un jugador para un rol y prioridad específicos
     * @param playerProfileId UUID del perfil del jugador
     * @param roleType Tipo de rol
     * @param priorityLevel Nivel de prioridad
     * @return Lista del historial de calificaciones ordenado por fecha descendente
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.playerRating.playerProfile.atletaUuid = :playerProfileId AND rh.playerRating.roleType = :roleType AND rh.playerRating.priorityLevel = :priorityLevel ORDER BY rh.createdAt DESC")
    List<RatingHistory> findByPlayerProfileIdAndRoleTypeAndPriorityLevel(
            @Param("playerProfileId") UUID playerProfileId,
            @Param("roleType") RoleType roleType,
            @Param("priorityLevel") PriorityLevel priorityLevel);

    /**
     * Busca el historial de calificaciones para un partido específico
     * @param matchId ID del partido
     * @return Lista del historial de calificaciones para el partido
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.match.id = :matchId ORDER BY rh.playerRating.playerProfile.atletaUuid")
    List<RatingHistory> findByMatchId(@Param("matchId") Long matchId);

    /**
     * Busca el historial de calificaciones en un período específico
     * @param startDate Fecha de inicio del período
     * @param endDate Fecha de fin del período
     * @return Lista del historial de calificaciones en el período ordenado por fecha descendente
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.createdAt BETWEEN :startDate AND :endDate ORDER BY rh.createdAt DESC")
    List<RatingHistory> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Busca el historial de calificaciones de un jugador en un período específico
     * @param playerProfileId UUID del perfil del jugador
     * @param startDate Fecha de inicio del período
     * @param endDate Fecha de fin del período
     * @return Lista del historial de calificaciones del jugador en el período
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.playerRating.playerProfile.atletaUuid = :playerProfileId AND rh.createdAt BETWEEN :startDate AND :endDate ORDER BY rh.createdAt DESC")
    List<RatingHistory> findByPlayerProfileIdAndCreatedAtBetween(
            @Param("playerProfileId") UUID playerProfileId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Busca el historial de calificaciones por resultado de partido
     * @param matchResult Resultado del partido
     * @return Lista del historial de calificaciones para el resultado especificado
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.matchResult = :matchResult ORDER BY rh.createdAt DESC")
    List<RatingHistory> findByMatchResult(@Param("matchResult") MatchResultType matchResult);

    /**
     * Busca el historial de calificaciones donde el jugador fue MVP
     * @return Lista del historial de calificaciones de jugadores MVP
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.wasMvp = true ORDER BY rh.createdAt DESC")
    List<RatingHistory> findMvpHistory();

    /**
     * Busca el historial de calificaciones de un jugador donde fue MVP
     * @param playerProfileId UUID del perfil del jugador
     * @return Lista del historial de calificaciones MVP del jugador
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.playerRating.playerProfile.atletaUuid = :playerProfileId AND rh.wasMvp = true ORDER BY rh.createdAt DESC")
    List<RatingHistory> findMvpHistoryByPlayerProfileId(@Param("playerProfileId") UUID playerProfileId);

    /**
     * Busca el historial de calificaciones en modo arquero rotativo
     * @return Lista del historial de calificaciones en modo arquero rotativo
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.rotativeGoalkeeperMode = true ORDER BY rh.createdAt DESC")
    List<RatingHistory> findRotativeGoalkeeperHistory();

    /**
     * Busca el historial de calificaciones con delta positivo (mejoras)
     * @return Lista del historial de calificaciones con mejoras
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.ratingDelta > 0 ORDER BY rh.ratingDelta DESC")
    List<RatingHistory> findPositiveRatingChanges();

    /**
     * Busca el historial de calificaciones con delta negativo (empeoramientos)
     * @return Lista del historial de calificaciones con empeoramientos
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.ratingDelta < 0 ORDER BY rh.ratingDelta ASC")
    List<RatingHistory> findNegativeRatingChanges();

    /**
     * Busca el historial de calificaciones de un jugador con delta positivo
     * @param playerProfileId UUID del perfil del jugador
     * @return Lista del historial de mejoras del jugador
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.playerRating.playerProfile.atletaUuid = :playerProfileId AND rh.ratingDelta > 0 ORDER BY rh.ratingDelta DESC")
    List<RatingHistory> findPositiveRatingChangesByPlayerProfileId(@Param("playerProfileId") UUID playerProfileId);

    /**
     * Busca el historial de calificaciones de un jugador con delta negativo
     * @param playerProfileId UUID del perfil del jugador
     * @return Lista del historial de empeoramientos del jugador
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.playerRating.playerProfile.atletaUuid = :playerProfileId AND rh.ratingDelta < 0 ORDER BY rh.ratingDelta ASC")
    List<RatingHistory> findNegativeRatingChangesByPlayerProfileId(@Param("playerProfileId") UUID playerProfileId);

    /**
     * Busca el historial de calificaciones con goles anotados
     * @param minGoals Número mínimo de goles
     * @return Lista del historial de calificaciones con al menos el número especificado de goles
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.goalsScored >= :minGoals ORDER BY rh.goalsScored DESC")
    List<RatingHistory> findByGoalsScoredGreaterThanEqual(@Param("minGoals") Integer minGoals);

    /**
     * Busca el historial de calificaciones con asistencias realizadas
     * @param minAssists Número mínimo de asistencias
     * @return Lista del historial de calificaciones con al menos el número especificado de asistencias
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.assistsMade >= :minAssists ORDER BY rh.assistsMade DESC")
    List<RatingHistory> findByAssistsMadeGreaterThanEqual(@Param("minAssists") Integer minAssists);

    /**
     * Busca el historial de calificaciones defensivas (con goles recibidos registrados)
     * @return Lista del historial de calificaciones defensivas
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.goalsConceded IS NOT NULL ORDER BY rh.goalsConceded ASC")
    List<RatingHistory> findDefensiveHistory();

    /**
     * Busca el historial de calificaciones defensivas de un jugador
     * @param playerProfileId UUID del perfil del jugador
     * @return Lista del historial de calificaciones defensivas del jugador
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.playerRating.playerProfile.atletaUuid = :playerProfileId AND rh.goalsConceded IS NOT NULL ORDER BY rh.goalsConceded ASC")
    List<RatingHistory> findDefensiveHistoryByPlayerProfileId(@Param("playerProfileId") UUID playerProfileId);

    /**
     * Obtiene estadísticas de rendimiento de un jugador
     * @param playerProfileId UUID del perfil del jugador
     * @return Array con [total_partidos, total_goles, total_asistencias, veces_mvp, promedio_delta]
     */
    @Query("SELECT COUNT(rh), SUM(rh.goalsScored), SUM(rh.assistsMade), SUM(CASE WHEN rh.wasMvp = true THEN 1 ELSE 0 END), AVG(rh.ratingDelta) FROM RatingHistory rh WHERE rh.playerRating.playerProfile.atletaUuid = :playerProfileId")
    Object[] getPlayerPerformanceStatistics(@Param("playerProfileId") UUID playerProfileId);

    @Query("SELECT " +
           "SUM(CASE WHEN rh.matchResult = 'GANADO' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN rh.matchResult = 'PERDIDO' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN rh.matchResult = 'EMPATE' THEN 1 ELSE 0 END) " +
           "FROM RatingHistory rh WHERE rh.playerRating.playerProfile.atletaUuid = :playerProfileId")
    Object[] getResultBreakdownByPlayerProfileId(@Param("playerProfileId") UUID playerProfileId);

    /**
     * Obtiene estadísticas de rendimiento de un jugador para un rol específico
     * @param playerProfileId UUID del perfil del jugador
     * @param roleType Tipo de rol
     * @return Array con [total_partidos, total_goles, total_asistencias, veces_mvp, promedio_delta]
     */
    @Query("SELECT COUNT(rh), SUM(rh.goalsScored), SUM(rh.assistsMade), SUM(CASE WHEN rh.wasMvp = true THEN 1 ELSE 0 END), AVG(rh.ratingDelta) FROM RatingHistory rh WHERE rh.playerRating.playerProfile.atletaUuid = :playerProfileId AND rh.playerRating.roleType = :roleType")
    Object[] getPlayerPerformanceStatisticsByRole(
            @Param("playerProfileId") UUID playerProfileId,
            @Param("roleType") RoleType roleType);

    /**
     * Obtiene el historial más reciente de un jugador (últimos N registros)
     * @param playerProfileId UUID del perfil del jugador
     * @param limit Número máximo de registros
     * @return Lista del historial más reciente del jugador
     */
    @Query(value = "SELECT rh FROM RatingHistory rh WHERE rh.playerRating.playerProfile.atletaUuid = :playerProfileId ORDER BY rh.createdAt DESC")
    List<RatingHistory> findRecentHistoryByPlayerProfileId(
            @Param("playerProfileId") UUID playerProfileId);

    /**
     * Cuenta el número de registros de historial para un jugador
     * @param playerProfileId UUID del perfil del jugador
     * @return Número de registros de historial del jugador
     */
    @Query("SELECT COUNT(rh) FROM RatingHistory rh WHERE rh.playerRating.playerProfile.atletaUuid = :playerProfileId")
    long countByPlayerProfileId(@Param("playerProfileId") UUID playerProfileId);

    /**
     * Cuenta el número de registros de historial para un partido
     * @param matchId ID del partido
     * @return Número de registros de historial para el partido
     */
    @Query("SELECT COUNT(rh) FROM RatingHistory rh WHERE rh.match.id = :matchId")
    long countByMatchId(@Param("matchId") Long matchId);

    /**
     * Busca el mayor delta de calificación registrado
     * @return Lista de registros con el mayor delta de calificación
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.ratingDelta = (SELECT MAX(rh2.ratingDelta) FROM RatingHistory rh2)")
    List<RatingHistory> findHighestRatingDelta();

    /**
     * Busca el menor delta de calificación registrado
     * @return Lista de registros con el menor delta de calificación
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.ratingDelta = (SELECT MIN(rh2.ratingDelta) FROM RatingHistory rh2)")
    List<RatingHistory> findLowestRatingDelta();

    /**
     * Busca registros de historial con bonos defensivos aplicados
     * @return Lista de registros con bonos defensivos
     */
    @Query("SELECT rh FROM RatingHistory rh WHERE rh.defensiveBonus IS NOT NULL AND rh.defensiveBonus > 0 ORDER BY rh.defensiveBonus DESC")
    List<RatingHistory> findHistoryWithDefensiveBonus();

    /**
     * Busca la evolución de calificación de un jugador para un rol específico (últimos N registros)
     * @param playerProfileId UUID del perfil del jugador
     * @param roleType Tipo de rol
     * @param limit Número máximo de registros
     * @return Lista de la evolución de calificación ordenada cronológicamente
     */
    @Query(value = "SELECT rh FROM RatingHistory rh WHERE rh.playerRating.playerProfile.atletaUuid = :playerProfileId AND rh.playerRating.roleType = :roleType ORDER BY rh.createdAt ASC")
    List<RatingHistory> findRatingEvolutionByPlayerProfileIdAndRoleType(
            @Param("playerProfileId") UUID playerProfileId,
            @Param("roleType") RoleType roleType);
}
