package com.atleta.demo.repository;

import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad MatchTeam.
 * Proporciona operaciones CRUD básicas y consultas personalizadas
 * para la gestión de equipos participantes en partidos.
 */
@Repository
public interface MatchTeamRepository extends JpaRepository<MatchTeam, Long> {

    /**
     * Busca todos los equipos de un partido específico
     * @param match Partido
     * @return Lista de equipos participantes en el partido
     */
    List<MatchTeam> findByMatch(Match match);

    /**
     * Busca todos los partidos donde ha participado un equipo
     * @param team Equipo
     * @return Lista de participaciones del equipo en partidos
     */
    List<MatchTeam> findByTeam(Team team);

    boolean existsByTeam(Team team);

    /**
     * Busca el equipo local de un partido
     * @param match Partido
     * @return Optional con el equipo local si existe
     */
    @Query("SELECT mt FROM MatchTeam mt WHERE mt.match = :match AND mt.esLocal = true")
    Optional<MatchTeam> findLocalTeamByMatch(@Param("match") Match match);

    /**
     * Busca el equipo visitante de un partido
     * @param match Partido
     * @return Optional con el equipo visitante si existe
     */
    @Query("SELECT mt FROM MatchTeam mt WHERE mt.match = :match AND mt.esLocal = false")
    Optional<MatchTeam> findVisitingTeamByMatch(@Param("match") Match match);

    /**
     * Busca la participación específica de un equipo en un partido
     * @param match Partido
     * @param team Equipo
     * @return Optional con la participación si existe
     */
    Optional<MatchTeam> findByMatchAndTeam(Match match, Team team);

    /**
     * Verifica si un equipo participa en un partido específico
     * @param match Partido
     * @param team Equipo
     * @return true si participa, false en caso contrario
     */
    boolean existsByMatchAndTeam(Match match, Team team);

    /**
     * Cuenta cuántos equipos participan en un partido
     * @param match Partido
     * @return Número de equipos participantes
     */
    long countByMatch(Match match);

    /**
     * Busca partidos donde un equipo jugó como local
     * @param team Equipo
     * @return Lista de participaciones como equipo local
     */
    @Query("SELECT mt FROM MatchTeam mt WHERE mt.team = :team AND mt.esLocal = true")
    List<MatchTeam> findHomeMatchesByTeam(@Param("team") Team team);

    /**
     * Busca partidos donde un equipo jugó como visitante
     * @param team Equipo
     * @return Lista de participaciones como equipo visitante
     */
    @Query("SELECT mt FROM MatchTeam mt WHERE mt.team = :team AND mt.esLocal = false")
    List<MatchTeam> findAwayMatchesByTeam(@Param("team") Team team);

    /**
     * Busca equipos que han marcado al menos un número específico de goles
     * @param minGoles Número mínimo de goles
     * @return Lista de participaciones que cumplen el criterio
     */
    @Query("SELECT mt FROM MatchTeam mt WHERE mt.goles >= :minGoles ORDER BY mt.goles DESC")
    List<MatchTeam> findByGolesGreaterThanEqual(@Param("minGoles") Integer minGoles);

    /**
     * Busca la participación con más goles en un partido específico
     * @param match Partido
     * @return Optional con la participación que más goles marcó
     */
    @Query("SELECT mt FROM MatchTeam mt WHERE mt.match = :match ORDER BY mt.goles DESC")
    List<MatchTeam> findByMatchOrderByGolesDesc(@Param("match") Match match);

    /**
     * Busca partidos con exactamente 2 equipos (válidos)
     * @return Lista de partidos que tienen exactamente 2 equipos
     */
    @Query("SELECT mt.match FROM MatchTeam mt GROUP BY mt.match HAVING COUNT(mt) = 2")
    List<Match> findMatchesWithExactlyTwoTeams();

    /**
     * Busca partidos que NO tienen exactamente 2 equipos (inválidos)
     * @return Lista de partidos que no tienen exactamente 2 equipos
     */
    @Query("SELECT mt.match FROM MatchTeam mt GROUP BY mt.match HAVING COUNT(mt) != 2")
    List<Match> findMatchesWithoutExactlyTwoTeams();

    /**
     * Calcula el total de goles marcados por un equipo
     * @param team Equipo
     * @return Suma total de goles del equipo
     */
    @Query("SELECT SUM(mt.goles) FROM MatchTeam mt WHERE mt.team = :team")
    Long getTotalGoalsByTeam(@Param("team") Team team);

    /**
     * Calcula el promedio de goles por partido de un equipo
     * @param team Equipo
     * @return Promedio de goles por partido
     */
    @Query("SELECT AVG(mt.goles) FROM MatchTeam mt WHERE mt.team = :team")
    Double getAverageGoalsByTeam(@Param("team") Team team);

    /**
     * Busca equipos ordenados por total de goles marcados
     * @return Lista de equipos ordenados por goles totales (descendente)
     */
    @Query("SELECT mt.team FROM MatchTeam mt GROUP BY mt.team ORDER BY SUM(mt.goles) DESC")
    List<Team> findTeamsOrderByTotalGoals();

    /**
     * Busca enfrentamientos entre dos equipos específicos
     * @param team1 Primer equipo
     * @param team2 Segundo equipo
     * @return Lista de partidos donde se enfrentaron estos equipos
     */
    @Query("SELECT mt1.match FROM MatchTeam mt1 JOIN MatchTeam mt2 ON mt1.match = mt2.match " +
           "WHERE mt1.team = :team1 AND mt2.team = :team2 AND mt1.id != mt2.id")
    List<Match> findMatchesBetweenTeams(@Param("team1") Team team1, @Param("team2") Team team2);

    /**
     * Busca partidos donde hubo goleadas (diferencia de goles >= 3)
     * @return Lista de partidos con goleadas
     */
    @Query("SELECT mt1.match FROM MatchTeam mt1 JOIN MatchTeam mt2 ON mt1.match = mt2.match " +
           "WHERE mt1.id != mt2.id AND ABS(mt1.goles - mt2.goles) >= 3")
    List<Match> findMatchesWithLargeGoalDifference();
}
