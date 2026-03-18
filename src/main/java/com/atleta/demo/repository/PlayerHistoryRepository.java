package com.atleta.demo.repository;

import com.atleta.demo.entity.PlayerHistory;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Team;
import com.atleta.demo.entity.Position;
import com.atleta.demo.enums.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la entidad PlayerHistory.
 * Proporciona operaciones CRUD básicas y consultas personalizadas
 * para la gestión del historial inmutable de jugadores.
 * Esta entidad es la FUENTE DE VERDAD para estadísticas históricas.
 */
@Repository
public interface PlayerHistoryRepository extends JpaRepository<PlayerHistory, Long> {

    /**
     * Busca todo el historial de un jugador específico
     * @param player Perfil del jugador
     * @return Lista del historial del jugador ordenado por fecha (más reciente primero)
     */
    @Query("SELECT ph FROM PlayerHistory ph WHERE ph.player = :player ORDER BY ph.createdAt DESC")
    List<PlayerHistory> findByPlayerOrderByCreatedAtDesc(@Param("player") PlayerProfile player);

    /**
     * Busca el historial de un partido específico
     * @param match Partido
     * @return Lista del historial de todos los jugadores en el partido
     */
    List<PlayerHistory> findByMatch(Match match);

    /**
     * Busca el historial específico de un jugador en un partido
     * @param match Partido
     * @param player Perfil del jugador
     * @return Optional con el historial si existe
     */
    Optional<PlayerHistory> findByMatchAndPlayer(Match match, PlayerProfile player);

    /**
     * Busca historial de un jugador en un equipo específico
     * @param player Perfil del jugador
     * @param team Equipo
     * @return Lista del historial del jugador en ese equipo
     */
    @Query("SELECT ph FROM PlayerHistory ph WHERE ph.player = :player AND ph.team = :team ORDER BY ph.createdAt DESC")
    List<PlayerHistory> findByPlayerAndTeamOrderByCreatedAtDesc(@Param("player") PlayerProfile player, @Param("team") Team team);

    boolean existsByTeam(Team team);

    /**
     * Busca historial por resultado específico
     * @param player Perfil del jugador
     * @param resultado Resultado del partido
     * @return Lista del historial con el resultado especificado
     */
    List<PlayerHistory> findByPlayerAndResultado(PlayerProfile player, MatchResult resultado);

    /**
     * Busca historial de un jugador en una posición específica
     * @param player Perfil del jugador
     * @param position Posición jugada
     * @return Lista del historial del jugador en esa posición
     */
    List<PlayerHistory> findByPlayerAndPosition(PlayerProfile player, Position position);

    /**
     * Cuenta victorias de un jugador
     * @param player Perfil del jugador
     * @return Número de victorias del jugador
     */
    @Query("SELECT COUNT(ph) FROM PlayerHistory ph WHERE ph.player = :player AND ph.resultado = 'VICTORIA'")
    long countVictoriesByPlayer(@Param("player") PlayerProfile player);

    /**
     * Cuenta derrotas de un jugador
     * @param player Perfil del jugador
     * @return Número de derrotas del jugador
     */
    @Query("SELECT COUNT(ph) FROM PlayerHistory ph WHERE ph.player = :player AND ph.resultado = 'DERROTA'")
    long countDefeatsByPlayer(@Param("player") PlayerProfile player);

    /**
     * Cuenta empates de un jugador
     * @param player Perfil del jugador
     * @return Número de empates del jugador
     */
    @Query("SELECT COUNT(ph) FROM PlayerHistory ph WHERE ph.player = :player AND ph.resultado = 'EMPATE'")
    long countDrawsByPlayer(@Param("player") PlayerProfile player);

    /**
     * Suma total de goles de un jugador (FUENTE DE VERDAD)
     * @param player Perfil del jugador
     * @return Total de goles del jugador
     */
    @Query("SELECT SUM(ph.goles) FROM PlayerHistory ph WHERE ph.player = :player")
    Long getTotalGoalsByPlayer(@Param("player") PlayerProfile player);

    /**
     * Suma total de asistencias de un jugador (FUENTE DE VERDAD)
     * @param player Perfil del jugador
     * @return Total de asistencias del jugador
     */
    @Query("SELECT SUM(ph.asistencias) FROM PlayerHistory ph WHERE ph.player = :player")
    Long getTotalAssistsByPlayer(@Param("player") PlayerProfile player);

    /**
     * Suma total de XP ganada por un jugador
     * @param player Perfil del jugador
     * @return Total de XP del jugador
     */
    @Query("SELECT SUM(ph.xpGanada) FROM PlayerHistory ph WHERE ph.player = :player")
    Long getTotalXpByPlayer(@Param("player") PlayerProfile player);

    /**
     * Busca jugadores con más goles (basado en historial - FUENTE DE VERDAD)
     * @return Lista de jugadores ordenados por goles totales (descendente)
     */
    @Query("SELECT ph.player FROM PlayerHistory ph GROUP BY ph.player ORDER BY SUM(ph.goles) DESC")
    List<PlayerProfile> findTopScorersByTotalGoals();

    /**
     * Busca jugadores con más asistencias (basado en historial - FUENTE DE VERDAD)
     * @return Lista de jugadores ordenados por asistencias totales (descendente)
     */
    @Query("SELECT ph.player FROM PlayerHistory ph GROUP BY ph.player ORDER BY SUM(ph.asistencias) DESC")
    List<PlayerProfile> findTopAssistersByTotalAssists();

    /**
     * Busca jugadores más activos por número de partidos
     * @return Lista de jugadores ordenados por partidos jugados (descendente)
     */
    @Query("SELECT ph.player FROM PlayerHistory ph GROUP BY ph.player ORDER BY COUNT(ph) DESC")
    List<PlayerProfile> findMostActivePlayersByMatchCount();

    /**
     * Busca historial en un rango de fechas
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista del historial en el rango especificado
     */
    @Query("SELECT ph FROM PlayerHistory ph WHERE ph.createdAt BETWEEN :fechaInicio AND :fechaFin ORDER BY ph.createdAt DESC")
    List<PlayerHistory> findByCreatedAtBetween(@Param("fechaInicio") LocalDateTime fechaInicio, 
                                              @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * Busca historial de un jugador por UUID del atleta
     * @param atletaUuid UUID del atleta
     * @return Lista del historial del atleta
     */
    @Query("SELECT ph FROM PlayerHistory ph WHERE ph.player.atletaUuid = :atletaUuid ORDER BY ph.createdAt DESC")
    List<PlayerHistory> findByPlayerAtletaUuidOrderByCreatedAtDesc(@Param("atletaUuid") UUID atletaUuid);

    /**
     * Obtiene estadísticas completas de un jugador (FUENTE DE VERDAD)
     * @param player Perfil del jugador
     * @return Array con [partidos, victorias, derrotas, empates, goles, asistencias, xp_total]
     */
    @Query("SELECT COUNT(ph), " +
           "SUM(CASE WHEN ph.resultado = 'VICTORIA' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN ph.resultado = 'DERROTA' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN ph.resultado = 'EMPATE' THEN 1 ELSE 0 END), " +
           "SUM(ph.goles), SUM(ph.asistencias), SUM(ph.xpGanada) " +
           "FROM PlayerHistory ph WHERE ph.player = :player")
    Object[] getCompleteStatsByPlayer(@Param("player") PlayerProfile player);

    /**
     * Busca historial con goles marcados (mayor a 0)
     * @param player Perfil del jugador
     * @return Lista del historial donde el jugador marcó goles
     */
    @Query("SELECT ph FROM PlayerHistory ph WHERE ph.player = :player AND ph.goles > 0 ORDER BY ph.goles DESC")
    List<PlayerHistory> findMatchesWithGoalsByPlayer(@Param("player") PlayerProfile player);

    /**
     * Busca historial con asistencias realizadas (mayor a 0)
     * @param player Perfil del jugador
     * @return Lista del historial donde el jugador dio asistencias
     */
    @Query("SELECT ph FROM PlayerHistory ph WHERE ph.player = :player AND ph.asistencias > 0 ORDER BY ph.asistencias DESC")
    List<PlayerHistory> findMatchesWithAssistsByPlayer(@Param("player") PlayerProfile player);

    /**
     * Busca el mejor partido de un jugador (más goles + asistencias)
     * @param player Perfil del jugador
     * @return Optional con el mejor partido del jugador
     */
    @Query("SELECT ph FROM PlayerHistory ph WHERE ph.player = :player ORDER BY (ph.goles + ph.asistencias) DESC")
    List<PlayerHistory> findBestMatchesByPlayer(@Param("player") PlayerProfile player);

    /**
     * Calcula porcentaje de victorias de un jugador
     * @param player Perfil del jugador
     * @return Porcentaje de victorias (0.0 a 1.0)
     */
    @Query("SELECT AVG(CASE WHEN ph.resultado = 'VICTORIA' THEN 1.0 ELSE 0.0 END) FROM PlayerHistory ph WHERE ph.player = :player")
    Double getWinPercentageByPlayer(@Param("player") PlayerProfile player);

    @Modifying
    @Query(value = "UPDATE player_history " +
            "SET mvp_bonus_xp = :bonusXp, " +
            "    xp_ganada = xp_ganada + :bonusXp, " +
            "    updated_at = CURRENT_TIMESTAMP, " +
            "    version = version + 1 " +
            "WHERE match_id = :matchId " +
            "  AND user_id = :userId " +
            "  AND mvp_bonus_xp = 0", nativeQuery = true)
    int applyMvpBonusXpIfNotApplied(
            @Param("matchId") Long matchId,
            @Param("userId") UUID userId,
            @Param("bonusXp") Integer bonusXp
    );
}
