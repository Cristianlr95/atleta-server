package com.atleta.demo.repository;

import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la entidad Match.
 * Proporciona operaciones CRUD básicas y consultas personalizadas
 * para la gestión de partidos de fútbol.
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    Optional<Match> findByCreadorAtletaUuidAndCreationIdempotencyKey(UUID creadorUuid, String creationIdempotencyKey);

    /**
     * Busca partidos por modalidad
     * @param modalidad Modalidad del partido (5v5, 6v6, 7v7)
     * @return Lista de partidos con la modalidad especificada
     */
    List<Match> findByModalidad(MatchMode modalidad);

    /**
     * Busca partidos por estado
     * @param estado Estado del partido
     * @return Lista de partidos con el estado especificado
     */
    List<Match> findByEstado(MatchStatus estado);

    /**
     * Busca partidos creados por un jugador específico
     * @param creador Perfil del jugador creador
     * @return Lista de partidos creados por el jugador
     */
    List<Match> findByCreador(PlayerProfile creador);

    /**
     * Busca partidos programados después de una fecha específica
     * @param fecha Fecha de referencia
     * @return Lista de partidos programados después de la fecha
     */
    @Query("SELECT m FROM Match m WHERE m.fechaHoraProgramada > :fecha ORDER BY m.fechaHoraProgramada ASC")
    List<Match> findByFechaHoraProgramadaAfter(@Param("fecha") LocalDateTime fecha);

    /**
     * Busca partidos programados antes de una fecha específica
     * @param fecha Fecha de referencia
     * @return Lista de partidos programados antes de la fecha
     */
    @Query("SELECT m FROM Match m WHERE m.fechaHoraProgramada < :fecha ORDER BY m.fechaHoraProgramada DESC")
    List<Match> findByFechaHoraProgramadaBefore(@Param("fecha") LocalDateTime fecha);

    /**
     * Busca partidos programados en un rango de fechas
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin Fecha de fin del rango
     * @return Lista de partidos programados en el rango
     */
    @Query("SELECT m FROM Match m WHERE m.fechaHoraProgramada BETWEEN :fechaInicio AND :fechaFin ORDER BY m.fechaHoraProgramada ASC")
    List<Match> findByFechaHoraProgramadaBetween(@Param("fechaInicio") LocalDateTime fechaInicio, 
                                                @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * Busca partidos por modalidad y estado
     * @param modalidad Modalidad del partido
     * @param estado Estado del partido
     * @return Lista de partidos que coinciden con ambos criterios
     */
    List<Match> findByModalidadAndEstado(MatchMode modalidad, MatchStatus estado);

    /**
     * Busca partidos donde participa un equipo específico
     * @param team Equipo participante
     * @return Lista de partidos donde participa el equipo
     */
    @Query("SELECT m FROM Match m JOIN m.matchTeams mt WHERE mt.team = :team")
    List<Match> findByTeam(@Param("team") Team team);

    /**
     * Busca partidos donde participa un jugador específico
     * @param player Perfil del jugador
     * @return Lista de partidos donde participa el jugador
     */
    @Query("SELECT DISTINCT m FROM Match m JOIN m.players mp WHERE mp.player = :player")
    List<Match> findByPlayer(@Param("player") PlayerProfile player);

    /**
     * Busca partidos con cuota mayor o igual a la especificada
     * @param cuotaMinima Cuota mínima
     * @return Lista de partidos con cuota mayor o igual
     */
    @Query("SELECT m FROM Match m WHERE m.cuota >= :cuotaMinima ORDER BY m.cuota ASC")
    List<Match> findByCuotaGreaterThanEqual(@Param("cuotaMinima") BigDecimal cuotaMinima);

    /**
     * Busca partidos en un radio específico de coordenadas
     * @param latitud Latitud central
     * @param longitud Longitud central
     * @param radioKm Radio en kilómetros
     * @return Lista de partidos dentro del radio especificado
     */
    @Query("SELECT m FROM Match m WHERE " +
           "(6371 * acos(cos(radians(:latitud)) * cos(radians(m.latitud)) * " +
           "cos(radians(m.longitud) - radians(:longitud)) + " +
           "sin(radians(:latitud)) * sin(radians(m.latitud)))) <= :radioKm")
    List<Match> findMatchesWithinRadius(@Param("latitud") BigDecimal latitud, 
                                       @Param("longitud") BigDecimal longitud, 
                                       @Param("radioKm") Double radioKm);

    /**
     * Busca partidos que tienen exactamente 2 equipos (válidos)
     * @return Lista de partidos con exactamente 2 equipos
     */
    @Query("SELECT m FROM Match m WHERE SIZE(m.matchTeams) = 2")
    List<Match> findMatchesWithExactlyTwoTeams();

    /**
     * Busca partidos que NO tienen exactamente 2 equipos (inválidos)
     * @return Lista de partidos sin exactamente 2 equipos
     */
    @Query("SELECT m FROM Match m WHERE SIZE(m.matchTeams) != 2")
    List<Match> findMatchesWithoutExactlyTwoTeams();

    /**
     * Busca partidos finalizados con eventos registrados
     * @return Lista de partidos finalizados que tienen eventos
     */
    @Query("SELECT DISTINCT m FROM Match m JOIN m.events e WHERE m.estado = 'FINALIZADO'")
    List<Match> findFinishedMatchesWithEvents();

    /**
     * Cuenta partidos por estado
     * @param estado Estado del partido
     * @return Número de partidos con el estado especificado
     */
    long countByEstado(MatchStatus estado);

    /**
     * Cuenta partidos por modalidad
     * @param modalidad Modalidad del partido
     * @return Número de partidos con la modalidad especificada
     */
    long countByModalidad(MatchMode modalidad);

    /**
     * Busca partidos creados en un rango de fechas
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin Fecha de fin del rango
     * @return Lista de partidos creados en el rango
     */
    @Query("SELECT m FROM Match m WHERE m.createdAt BETWEEN :fechaInicio AND :fechaFin ORDER BY m.createdAt DESC")
    List<Match> findByCreatedAtBetween(@Param("fechaInicio") LocalDateTime fechaInicio, 
                                      @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * Busca próximos partidos (programados para el futuro y no finalizados)
     * @return Lista de próximos partidos ordenados por fecha
     */
    @Query("SELECT m FROM Match m WHERE m.fechaHoraProgramada > CURRENT_TIMESTAMP AND m.estado != 'FINALIZADO' ORDER BY m.fechaHoraProgramada ASC")
    List<Match> findUpcomingMatches();

    /**
     * Busca partidos pasados (programados para el pasado)
     * @return Lista de partidos pasados ordenados por fecha descendente
     */
    @Query("SELECT m FROM Match m WHERE m.fechaHoraProgramada < CURRENT_TIMESTAMP ORDER BY m.fechaHoraProgramada DESC")
    List<Match> findPastMatches();

    /**
     * Partidos creados que ya expiraron su ventana de juego y deben invalidarse automáticamente.
     */
    @Query("SELECT m FROM Match m WHERE m.estado = 'CREADO' AND m.fechaHoraProgramada <= :cutoff")
    List<Match> findExpiredCreatedMatches(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Partidos creados cuya hora ya llego y pueden iniciar automaticamente si cumplen precondiciones.
     */
    @Query("SELECT m FROM Match m WHERE m.estado = 'CREADO' AND m.fechaHoraProgramada <= :now")
    List<Match> findCreatedMatchesReadyToStart(@Param("now") LocalDateTime now);

    /**
     * Partidos iniciados que vencieron la ventana de carga/cierre y deben invalidarse.
     */
    @Query("SELECT m FROM Match m WHERE m.estado = 'INICIADO' AND m.startedAt IS NOT NULL AND m.startedAt <= :cutoff")
    List<Match> findExpiredStartedMatches(@Param("cutoff") LocalDateTime cutoff);
}
