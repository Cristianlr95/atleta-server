package com.atleta.demo.repository;

import com.atleta.demo.entity.PlayerPosition;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la entidad PlayerPosition.
 * Proporciona operaciones CRUD básicas y consultas personalizadas
 * para la gestión de posiciones y prioridades de jugadores.
 */
@Repository
public interface PlayerPositionRepository extends JpaRepository<PlayerPosition, Long> {

    /**
     * Busca todas las posiciones de un jugador específico
     * @param player Perfil del jugador
     * @return Lista de posiciones del jugador ordenadas por prioridad
     */
    @Query("SELECT pp FROM PlayerPosition pp WHERE pp.player = :player ORDER BY pp.prioridad ASC")
    List<PlayerPosition> findByPlayerOrderByPrioridad(@Param("player") PlayerProfile player);

    /**
     * Busca todas las posiciones de un jugador por UUID
     * @param atletaUuid UUID del atleta
     * @return Lista de posiciones del jugador ordenadas por prioridad
     */
    @Query("SELECT pp FROM PlayerPosition pp WHERE pp.player.atletaUuid = :atletaUuid ORDER BY pp.prioridad ASC")
    List<PlayerPosition> findByPlayerAtletaUuidOrderByPrioridad(@Param("atletaUuid") UUID atletaUuid);

    /**
     * Busca todos los jugadores que tienen una posición específica
     * @param position Posición a buscar
     * @return Lista de asignaciones de jugadores a esa posición
     */
    List<PlayerPosition> findByPosition(Position position);

    /**
     * Busca la posición con prioridad específica de un jugador
     * @param player Perfil del jugador
     * @param prioridad Prioridad (1, 2, 3)
     * @return Optional con la posición si existe
     */
    Optional<PlayerPosition> findByPlayerAndPrioridad(PlayerProfile player, Integer prioridad);

    /**
     * Busca si un jugador tiene asignada una posición específica
     * @param player Perfil del jugador
     * @param position Posición a verificar
     * @return Optional con la asignación si existe
     */
    Optional<PlayerPosition> findByPlayerAndPosition(PlayerProfile player, Position position);

    /**
     * Verifica si un jugador tiene una posición con prioridad específica
     * @param player Perfil del jugador
     * @param prioridad Prioridad a verificar
     * @return true si existe, false en caso contrario
     */
    boolean existsByPlayerAndPrioridad(PlayerProfile player, Integer prioridad);

    /**
     * Busca jugadores por posición ordenados por XP descendente
     * @param position Posición a buscar
     * @return Lista de asignaciones ordenadas por XP
     */
    @Query("SELECT pp FROM PlayerPosition pp WHERE pp.position = :position ORDER BY pp.xp DESC")
    List<PlayerPosition> findByPositionOrderByXpDesc(@Param("position") Position position);

    /**
     * Busca jugadores con XP mayor o igual al especificado en una posición
     * @param position Posición a buscar
     * @param minXp XP mínima
     * @return Lista de asignaciones que cumplen el criterio
     */
    @Query("SELECT pp FROM PlayerPosition pp WHERE pp.position = :position AND pp.xp >= :minXp ORDER BY pp.xp DESC")
    List<PlayerPosition> findByPositionAndXpGreaterThanEqual(@Param("position") Position position, 
                                                            @Param("minXp") Integer minXp);

    /**
     * Busca la posición principal (prioridad 1) de un jugador
     * @param player Perfil del jugador
     * @return Optional con la posición principal si existe
     */
    @Query("SELECT pp FROM PlayerPosition pp WHERE pp.player = :player AND pp.prioridad = 1")
    Optional<PlayerPosition> findPrimaryPositionByPlayer(@Param("player") PlayerProfile player);

    /**
     * Busca jugadores que tienen una posición como principal (prioridad 1)
     * @param position Posición a buscar
     * @return Lista de jugadores que tienen esa posición como principal
     */
    @Query("SELECT pp FROM PlayerPosition pp WHERE pp.position = :position AND pp.prioridad = 1")
    List<PlayerPosition> findPlayersByPrimaryPosition(@Param("position") Position position);

    /**
     * Cuenta cuántos jugadores tienen una posición específica
     * @param position Posición a contar
     * @return Número de jugadores con esa posición
     */
    long countByPosition(Position position);

    /**
     * Obtiene estadísticas de XP para una posición específica
     * @param position Posición a analizar
     * @return Array con [min, max, avg] de XP para esa posición
     */
    @Query("SELECT MIN(pp.xp), MAX(pp.xp), AVG(pp.xp) FROM PlayerPosition pp WHERE pp.position = :position")
    Object[] getXpStatisticsByPosition(@Param("position") Position position);

    /**
     * Busca jugadores con múltiples posiciones (más de una)
     * @return Lista de jugadores que tienen más de una posición
     */
    @Query("SELECT pp.player FROM PlayerPosition pp GROUP BY pp.player HAVING COUNT(pp) > 1")
    List<PlayerProfile> findPlayersWithMultiplePositions();

    /**
     * Busca asignaciones de posición por rango de XP
     * @param minXp XP mínima
     * @param maxXp XP máxima
     * @return Lista de asignaciones en el rango de XP
     */
    @Query("SELECT pp FROM PlayerPosition pp WHERE pp.xp BETWEEN :minXp AND :maxXp ORDER BY pp.xp DESC")
    List<PlayerPosition> findByXpBetween(@Param("minXp") Integer minXp, @Param("maxXp") Integer maxXp);

    /**
     * Elimina todas las posiciones de un jugador específico
     * @param player Perfil del jugador
     */
    void deleteByPlayer(PlayerProfile player);
}