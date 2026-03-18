package com.atleta.demo.repository;

import com.atleta.demo.entity.TeamStats;
import com.atleta.demo.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad TeamStats.
 * Proporciona operaciones CRUD básicas y consultas personalizadas
 * para la gestión de estadísticas de equipos.
 */
@Repository
public interface TeamStatsRepository extends JpaRepository<TeamStats, Long> {

    /**
     * Busca las estadísticas de un equipo específico
     * @param team Equipo
     * @return Optional con las estadísticas del equipo si existen
     */
    Optional<TeamStats> findByTeam(Team team);

    /**
     * Busca equipos ordenados por número de partidos jugados (descendente)
     * @return Lista de estadísticas ordenadas por partidos jugados
     */
    @Query("SELECT ts FROM TeamStats ts ORDER BY ts.partidosJugados DESC")
    List<TeamStats> findAllOrderByPartidosJugadosDesc();

    /**
     * Busca equipos ordenados por número de victorias (descendente)
     * @return Lista de estadísticas ordenadas por victorias
     */
    @Query("SELECT ts FROM TeamStats ts ORDER BY ts.partidosGanados DESC")
    List<TeamStats> findAllOrderByVictoriasDesc();

    /**
     * Busca equipos ordenados por número de goles a favor (descendente)
     * @return Lista de estadísticas ordenadas por goles a favor
     */
    @Query("SELECT ts FROM TeamStats ts ORDER BY ts.golesFavor DESC")
    List<TeamStats> findAllOrderByGolesAFavorDesc();

    /**
     * Busca equipos con mejor diferencia de goles
     * @return Lista de estadísticas ordenadas por diferencia de goles (descendente)
     */
    @Query("SELECT ts FROM TeamStats ts ORDER BY (ts.golesFavor - ts.golesContra) DESC")
    List<TeamStats> findAllOrderByGoalDifferenceDesc();

    /**
     * Busca equipos con al menos un número específico de partidos jugados
     * @param minPartidos Número mínimo de partidos
     * @return Lista de estadísticas que cumplen el criterio
     */
    @Query("SELECT ts FROM TeamStats ts WHERE ts.partidosJugados >= :minPartidos ORDER BY ts.partidosJugados DESC")
    List<TeamStats> findByPartidosJugadosGreaterThanEqual(@Param("minPartidos") Integer minPartidos);

    /**
     * Busca equipos con al menos un número específico de victorias
     * @param minVictorias Número mínimo de victorias
     * @return Lista de estadísticas que cumplen el criterio
     */
    @Query("SELECT ts FROM TeamStats ts WHERE ts.partidosGanados >= :minVictorias ORDER BY ts.partidosGanados DESC")
    List<TeamStats> findByVictoriasGreaterThanEqual(@Param("minVictorias") Integer minVictorias);

    /**
     * Busca equipos con porcentaje de victorias mayor al especificado
     * @param minPorcentaje Porcentaje mínimo de victorias (0.0 a 1.0)
     * @return Lista de estadísticas que cumplen el criterio
     */
    @Query("SELECT ts FROM TeamStats ts WHERE ts.partidosJugados > 0 AND " +
           "(CAST(ts.partidosGanados AS DOUBLE) / ts.partidosJugados) >= :minPorcentaje " +
           "ORDER BY (CAST(ts.partidosGanados AS DOUBLE) / ts.partidosJugados) DESC")
    List<TeamStats> findByWinPercentageGreaterThanEqual(@Param("minPorcentaje") Double minPorcentaje);

    /**
     * Busca equipos invictos (sin derrotas)
     * @return Lista de estadísticas de equipos sin derrotas
     */
    @Query("SELECT ts FROM TeamStats ts WHERE ts.partidosPerdidos = 0 AND ts.partidosJugados > 0 ORDER BY ts.partidosJugados DESC")
    List<TeamStats> findUndefeatedTeams();

    /**
     * Busca equipos que no han ganado ningún partido
     * @return Lista de estadísticas de equipos sin victorias
     */
    @Query("SELECT ts FROM TeamStats ts WHERE ts.partidosGanados = 0 AND ts.partidosJugados > 0 ORDER BY ts.partidosJugados DESC")
    List<TeamStats> findTeamsWithoutWins();

    /**
     * Busca equipos con mejor ataque (más goles a favor)
     * @param limit Número máximo de resultados
     * @return Lista de estadísticas de equipos con mejor ataque
     */
    @Query("SELECT ts FROM TeamStats ts WHERE ts.partidosJugados > 0 ORDER BY ts.golesFavor DESC")
    List<TeamStats> findTopScoringTeams();

    /**
     * Busca equipos con mejor defensa (menos goles en contra)
     * @return Lista de estadísticas de equipos con mejor defensa
     */
    @Query("SELECT ts FROM TeamStats ts WHERE ts.partidosJugados > 0 ORDER BY ts.golesContra ASC")
    List<TeamStats> findBestDefensiveTeams();

    /**
     * Calcula estadísticas generales del sistema
     * @return Array con [total_partidos, total_goles, promedio_goles_por_partido]
     */
    @Query("SELECT SUM(ts.partidosJugados), SUM(ts.golesFavor), AVG(ts.golesFavor) FROM TeamStats ts")
    Object[] getGeneralStatistics();

    /**
     * Busca equipos con estadísticas en cero (recién creados)
     * @return Lista de estadísticas de equipos sin actividad
     */
    @Query("SELECT ts FROM TeamStats ts WHERE ts.partidosJugados = 0")
    List<TeamStats> findTeamsWithoutActivity();

    /**
     * Busca equipos más activos (más partidos jugados)
     * @param limit Número máximo de resultados
     * @return Lista de estadísticas de equipos más activos
     */
    @Query("SELECT ts FROM TeamStats ts ORDER BY ts.partidosJugados DESC")
    List<TeamStats> findMostActiveTeams();

    /**
     * Busca equipos por rango de partidos jugados
     * @param minPartidos Número mínimo de partidos
     * @param maxPartidos Número máximo de partidos
     * @return Lista de estadísticas en el rango especificado
     */
    @Query("SELECT ts FROM TeamStats ts WHERE ts.partidosJugados BETWEEN :minPartidos AND :maxPartidos ORDER BY ts.partidosJugados DESC")
    List<TeamStats> findByPartidosJugadosBetween(@Param("minPartidos") Integer minPartidos, 
                                                @Param("maxPartidos") Integer maxPartidos);
}