package com.atleta.demo.repository;

import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.Team;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Position;
import com.atleta.demo.enums.PlayerRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la entidad MatchPlayer.
 * Proporciona operaciones CRUD básicas y consultas personalizadas
 * para la gestión de participación de jugadores en partidos.
 */
@Repository
public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, Long> {

    /**
     * Busca todos los jugadores de un partido específico
     * @param match Partido
     * @return Lista de jugadores participantes en el partido
     */
    List<MatchPlayer> findByMatch(Match match);

    /**
     * Busca todos los partidos donde ha participado un jugador
     * @param player Perfil del jugador
     * @return Lista de participaciones del jugador en partidos
     */
    List<MatchPlayer> findByPlayer(PlayerProfile player);

    /**
     * Busca jugadores de un equipo específico en un partido
     * @param match Partido
     * @param team Equipo
     * @return Lista de jugadores del equipo en el partido
     */
    List<MatchPlayer> findByMatchAndTeam(Match match, Team team);

    boolean existsByTeam(Team team);

    /**
     * Busca la participación específica de un jugador en un partido
     * @param match Partido
     * @param player Perfil del jugador
     * @return Optional con la participación si existe
     */
    Optional<MatchPlayer> findByMatchAndPlayer(Match match, PlayerProfile player);

    /**
     * Busca jugadores confirmados en un partido
     * @param match Partido
     * @return Lista de jugadores confirmados para el partido
     */
    @Query("SELECT mp FROM MatchPlayer mp WHERE mp.match = :match AND mp.confirmado = true")
    List<MatchPlayer> findConfirmedPlayersByMatch(@Param("match") Match match);

    /**
     * Busca jugadores NO confirmados en un partido
     * @param match Partido
     * @return Lista de jugadores no confirmados para el partido
     */
    @Query("SELECT mp FROM MatchPlayer mp WHERE mp.match = :match AND mp.confirmado = false")
    List<MatchPlayer> findUnconfirmedPlayersByMatch(@Param("match") Match match);

    /**
     * Busca jugadores por posición en un partido específico
     * @param match Partido
     * @param position Posición
     * @return Lista de jugadores que juegan en esa posición en el partido
     */
    List<MatchPlayer> findByMatchAndPosition(Match match, Position position);

    /**
     * Busca jugadores por rol en un partido específico
     * @param match Partido
     * @param rol Rol del jugador
     * @return Lista de jugadores con el rol especificado en el partido
     */
    List<MatchPlayer> findByMatchAndRol(Match match, PlayerRole rol);

    /**
     * Busca capitanes de un partido
     * @param match Partido
     * @return Lista de capitanes en el partido
     */
    @Query("SELECT mp FROM MatchPlayer mp WHERE mp.match = :match AND mp.rol = 'CAPITAN'")
    List<MatchPlayer> findCaptainsByMatch(@Param("match") Match match);

    /**
     * Busca directores técnicos de un partido
     * @param match Partido
     * @return Lista de DTs en el partido
     */
    @Query("SELECT mp FROM MatchPlayer mp WHERE mp.match = :match AND mp.rol = 'DT'")
    List<MatchPlayer> findCoachesByMatch(@Param("match") Match match);

    /**
     * Verifica si un jugador está registrado en un partido específico
     * @param match Partido
     * @param player Perfil del jugador
     * @return true si está registrado, false en caso contrario
     */
    boolean existsByMatchAndPlayer(Match match, PlayerProfile player);

    /**
     * Verifica si un jugador está confirmado en un partido específico
     * @param match Partido
     * @param player Perfil del jugador
     * @return true si está confirmado, false en caso contrario
     */
    @Query("SELECT COUNT(mp) > 0 FROM MatchPlayer mp WHERE mp.match = :match AND mp.player = :player AND mp.confirmado = true")
    boolean isPlayerConfirmedForMatch(@Param("match") Match match, @Param("player") PlayerProfile player);

    /**
     * Cuenta jugadores confirmados en un partido
     * @param match Partido
     * @return Número de jugadores confirmados
     */
    @Query("SELECT COUNT(mp) FROM MatchPlayer mp WHERE mp.match = :match AND mp.confirmado = true")
    long countConfirmedPlayersByMatch(@Param("match") Match match);

    /**
     * Cuenta jugadores de un equipo en un partido
     * @param match Partido
     * @param team Equipo
     * @return Número de jugadores del equipo en el partido
     */
    long countByMatchAndTeam(Match match, Team team);

    /**
     * Busca partidos donde un jugador jugó en una posición específica
     * @param player Perfil del jugador
     * @param position Posición
     * @return Lista de participaciones del jugador en esa posición
     */
    List<MatchPlayer> findByPlayerAndPosition(PlayerProfile player, Position position);

    /**
     * Busca jugadores más activos (más partidos jugados)
     * @return Lista de jugadores ordenados por número de partidos (descendente)
     */
    @Query("SELECT mp.player FROM MatchPlayer mp GROUP BY mp.player ORDER BY COUNT(mp) DESC")
    List<PlayerProfile> findMostActivePlayersOrderByMatchCount();

    /**
     * Cuenta partidos jugados por un jugador específico
     * @param player Perfil del jugador
     * @return Número de partidos jugados por el jugador
     */
    long countByPlayer(PlayerProfile player);

    /**
     * Busca jugadores por UUID del atleta en un partido
     * @param match Partido
     * @param atletaUuid UUID del atleta
     * @return Optional con la participación si existe
     */
    @Query("SELECT mp FROM MatchPlayer mp WHERE mp.match = :match AND mp.player.atletaUuid = :atletaUuid")
    Optional<MatchPlayer> findByMatchAndPlayerAtletaUuid(@Param("match") Match match, @Param("atletaUuid") UUID atletaUuid);

    /**
     * Busca partidos donde un jugador fue capitán
     * @param player Perfil del jugador
     * @return Lista de partidos donde fue capitán
     */
    @Query("SELECT mp FROM MatchPlayer mp WHERE mp.player = :player AND mp.rol = 'CAPITAN'")
    List<MatchPlayer> findMatchesWhereCaptain(@Param("player") PlayerProfile player);

    /**
     * Busca jugadores que han jugado en múltiples posiciones
     * @return Lista de jugadores que han jugado en más de una posición
     */
    @Query("SELECT mp.player FROM MatchPlayer mp GROUP BY mp.player HAVING COUNT(DISTINCT mp.position) > 1")
    List<PlayerProfile> findPlayersWithMultiplePositions();

    /**
     * Busca estadísticas de confirmación por jugador
     * @param player Perfil del jugador
     * @return Array con [total_partidos, partidos_confirmados, porcentaje_confirmacion]
     */
    @Query("SELECT COUNT(mp), SUM(CASE WHEN mp.confirmado = true THEN 1 ELSE 0 END), " +
           "AVG(CASE WHEN mp.confirmado = true THEN 1.0 ELSE 0.0 END) " +
           "FROM MatchPlayer mp WHERE mp.player = :player")
    Object[] getConfirmationStatsByPlayer(@Param("player") PlayerProfile player);
}
