package com.atleta.demo.repository;

import com.atleta.demo.entity.MatchEvent;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio para la entidad MatchEvent.
 * Proporciona operaciones CRUD básicas y consultas personalizadas
 * para la gestión de eventos de partidos (goles, asistencias).
 */
@Repository
public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {

    /**
     * Busca todos los eventos de un partido específico
     * @param match Partido
     * @return Lista de eventos del partido ordenados por timestamp
     */
    @Query("SELECT me FROM MatchEvent me WHERE me.match = :match ORDER BY me.createdAt ASC")
    List<MatchEvent> findByMatchOrderByCreatedAt(@Param("match") Match match);

    /**
     * Busca todos los eventos de un jugador específico
     * @param player Perfil del jugador
     * @return Lista de eventos del jugador
     */
    List<MatchEvent> findByPlayer(PlayerProfile player);

    boolean existsByTeam(Team team);

    /**
     * Busca eventos por tipo en un partido específico
     * @param match Partido
     * @param tipoEvento Tipo de evento
     * @return Lista de eventos del tipo especificado en el partido
     */
    List<MatchEvent> findByMatchAndTipoEvento(Match match, EventType tipoEvento);

    /**
     * Busca todos los goles de un partido
     * @param match Partido
     * @return Lista de goles del partido
     */
    @Query("SELECT me FROM MatchEvent me WHERE me.match = :match AND me.tipoEvento = 'GOL' ORDER BY me.createdAt ASC")
    List<MatchEvent> findGoalsByMatch(@Param("match") Match match);

    /**
     * Busca todas las asistencias de un partido
     * @param match Partido
     * @return Lista de asistencias del partido
     */
    @Query("SELECT me FROM MatchEvent me WHERE me.match = :match AND me.tipoEvento = 'ASISTENCIA' ORDER BY me.createdAt ASC")
    List<MatchEvent> findAssistsByMatch(@Param("match") Match match);

    /**
     * Busca goles marcados por un jugador específico
     * @param player Perfil del jugador
     * @return Lista de goles del jugador
     */
    @Query("SELECT me FROM MatchEvent me WHERE me.player = :player AND me.tipoEvento = 'GOL'")
    List<MatchEvent> findGoalsByPlayer(@Param("player") PlayerProfile player);

    /**
     * Busca asistencias realizadas por un jugador específico
     * @param player Perfil del jugador
     * @return Lista de asistencias del jugador
     */
    @Query("SELECT me FROM MatchEvent me WHERE me.player = :player AND me.tipoEvento = 'ASISTENCIA'")
    List<MatchEvent> findAssistsByPlayer(@Param("player") PlayerProfile player);

    /**
     * Busca eventos donde un jugador fue asistente
     * @param assistPlayer Perfil del jugador asistente
     * @return Lista de eventos donde fue asistente
     */
    List<MatchEvent> findByAssistPlayer(PlayerProfile assistPlayer);

    /**
     * Busca eventos confirmados por ambos equipos
     * @param match Partido
     * @return Lista de eventos confirmados en el partido
     */
    @Query("SELECT me FROM MatchEvent me WHERE me.match = :match AND me.confirmedByHome = true AND me.confirmedByAway = true")
    List<MatchEvent> findConfirmedEventsByMatch(@Param("match") Match match);

    /**
     * Busca eventos pendientes de confirmación
     * @param match Partido
     * @return Lista de eventos pendientes de confirmación en el partido
     */
    @Query("SELECT me FROM MatchEvent me WHERE me.match = :match AND (me.confirmedByHome = false OR me.confirmedByAway = false)")
    List<MatchEvent> findPendingEventsByMatch(@Param("match") Match match);

    /**
     * Cuenta goles de un jugador en todos los partidos
     * @param player Perfil del jugador
     * @return Número total de goles del jugador
     */
    @Query("SELECT COUNT(me) FROM MatchEvent me WHERE me.player = :player AND me.tipoEvento = 'GOL'")
    long countGoalsByPlayer(@Param("player") PlayerProfile player);

    /**
     * Cuenta asistencias de un jugador en todos los partidos
     * @param player Perfil del jugador
     * @return Número total de asistencias del jugador
     */
    @Query("SELECT COUNT(me) FROM MatchEvent me WHERE me.player = :player AND me.tipoEvento = 'ASISTENCIA'")
    long countAssistsByPlayer(@Param("player") PlayerProfile player);

    /**
     * Cuenta eventos de un partido por tipo
     * @param match Partido
     * @param tipoEvento Tipo de evento
     * @return Número de eventos del tipo especificado en el partido
     */
    long countByMatchAndTipoEvento(Match match, EventType tipoEvento);

    /**
     * Busca jugadores con más goles (goleadores)
     * @return Lista de jugadores ordenados por número de goles (descendente)
     */
    @Query("SELECT me.player FROM MatchEvent me WHERE me.tipoEvento = 'GOL' GROUP BY me.player ORDER BY COUNT(me) DESC")
    List<PlayerProfile> findTopScorersOrderByGoalCount();

    /**
     * Busca jugadores con más asistencias
     * @return Lista de jugadores ordenados por número de asistencias (descendente)
     */
    @Query("SELECT me.player FROM MatchEvent me WHERE me.tipoEvento = 'ASISTENCIA' GROUP BY me.player ORDER BY COUNT(me) DESC")
    List<PlayerProfile> findTopAssistersOrderByAssistCount();

    /**
     * Busca eventos de un jugador por UUID del atleta
     * @param atletaUuid UUID del atleta
     * @return Lista de eventos del atleta
     */
    @Query("SELECT me FROM MatchEvent me WHERE me.player.atletaUuid = :atletaUuid")
    List<MatchEvent> findByPlayerAtletaUuid(@Param("atletaUuid") UUID atletaUuid);

    /**
     * Busca partidos con más goles
     * @return Lista de partidos ordenados por número de goles (descendente)
     */
    @Query("SELECT me.match FROM MatchEvent me WHERE me.tipoEvento = 'GOL' GROUP BY me.match ORDER BY COUNT(me) DESC")
    List<Match> findMatchesOrderByGoalCount();

    /**
     * Busca estadísticas de eventos por jugador
     * @param player Perfil del jugador
     * @return Array con [total_goles, total_asistencias, total_eventos]
     */
    @Query("SELECT " +
           "SUM(CASE WHEN me.tipoEvento = 'GOL' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN me.tipoEvento = 'ASISTENCIA' THEN 1 ELSE 0 END), " +
           "COUNT(me) " +
           "FROM MatchEvent me WHERE me.player = :player")
    Object[] getEventStatsByPlayer(@Param("player") PlayerProfile player);

    /**
     * Busca eventos que necesitan confirmación del equipo local
     * @param match Partido
     * @return Lista de eventos pendientes de confirmación local
     */
    @Query("SELECT me FROM MatchEvent me WHERE me.match = :match AND me.confirmedByHome = false")
    List<MatchEvent> findEventsPendingLocalConfirmation(@Param("match") Match match);

    /**
     * Busca eventos que necesitan confirmación del equipo visitante
     * @param match Partido
     * @return Lista de eventos pendientes de confirmación visitante
     */
    @Query("SELECT me FROM MatchEvent me WHERE me.match = :match AND me.confirmedByAway = false")
    List<MatchEvent> findEventsPendingVisitorConfirmation(@Param("match") Match match);
}
