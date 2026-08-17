package com.atleta.demo.repository;

import com.atleta.demo.entity.PlayerRating;
import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;
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
 * Repositorio para la entidad PlayerRating.
 * Proporciona operaciones CRUD básicas y consultas personalizadas
 * para la gestión de calificaciones de jugadores por rol y prioridad.
 */
@Repository
public interface PlayerRatingRepository extends JpaRepository<PlayerRating, Long> {

    /**
     * Busca todas las calificaciones de un jugador específico
     * @param playerProfileId UUID del perfil del jugador
     * @return Lista de calificaciones del jugador
     */
    @Query("SELECT pr FROM PlayerRating pr WHERE pr.playerProfile.atletaUuid = :playerProfileId ORDER BY pr.roleType, pr.priorityLevel")
    List<PlayerRating> findByPlayerProfileId(@Param("playerProfileId") UUID playerProfileId);

    @Query("SELECT pr FROM PlayerRating pr JOIN FETCH pr.playerProfile " +
           "WHERE pr.playerProfile.atletaUuid IN :playerProfileIds")
    List<PlayerRating> findByPlayerProfileIds(@Param("playerProfileIds") List<UUID> playerProfileIds);

    /**
     * Busca la calificación específica de un jugador para un rol y prioridad dados
     * @param playerProfileId UUID del perfil del jugador
     * @param roleType Tipo de rol
     * @param priorityLevel Nivel de prioridad
     * @return Optional con la calificación si existe
     */
    @Query("SELECT pr FROM PlayerRating pr WHERE pr.playerProfile.atletaUuid = :playerProfileId AND pr.roleType = :roleType AND pr.priorityLevel = :priorityLevel")
    Optional<PlayerRating> findByPlayerProfileIdAndRoleTypeAndPriorityLevel(
            @Param("playerProfileId") UUID playerProfileId,
            @Param("roleType") RoleType roleType,
            @Param("priorityLevel") PriorityLevel priorityLevel);

    /**
     * Busca todas las calificaciones de un jugador para un rol específico
     * @param playerProfileId UUID del perfil del jugador
     * @param roleType Tipo de rol
     * @return Lista de calificaciones del jugador para el rol especificado
     */
    @Query("SELECT pr FROM PlayerRating pr WHERE pr.playerProfile.atletaUuid = :playerProfileId AND pr.roleType = :roleType ORDER BY pr.priorityLevel")
    List<PlayerRating> findByPlayerProfileIdAndRoleType(
            @Param("playerProfileId") UUID playerProfileId,
            @Param("roleType") RoleType roleType);

    /**
     * Busca todas las calificaciones de un jugador para un nivel de prioridad específico
     * @param playerProfileId UUID del perfil del jugador
     * @param priorityLevel Nivel de prioridad
     * @return Lista de calificaciones del jugador para el nivel de prioridad especificado
     */
    @Query("SELECT pr FROM PlayerRating pr WHERE pr.playerProfile.atletaUuid = :playerProfileId AND pr.priorityLevel = :priorityLevel ORDER BY pr.roleType")
    List<PlayerRating> findByPlayerProfileIdAndPriorityLevel(
            @Param("playerProfileId") UUID playerProfileId,
            @Param("priorityLevel") PriorityLevel priorityLevel);

    /**
     * Busca todas las calificaciones para un rol específico
     * @param roleType Tipo de rol
     * @return Lista de calificaciones para el rol especificado
     */
    @Query("SELECT pr FROM PlayerRating pr WHERE pr.roleType = :roleType ORDER BY pr.currentRating DESC")
    List<PlayerRating> findByRoleType(@Param("roleType") RoleType roleType);

    /**
     * Busca todas las calificaciones para un nivel de prioridad específico
     * @param priorityLevel Nivel de prioridad
     * @return Lista de calificaciones para el nivel de prioridad especificado
     */
    @Query("SELECT pr FROM PlayerRating pr WHERE pr.priorityLevel = :priorityLevel ORDER BY pr.currentRating DESC")
    List<PlayerRating> findByPriorityLevel(@Param("priorityLevel") PriorityLevel priorityLevel);

    /**
     * Busca calificaciones en un rango de valores
     * @param minRating Calificación mínima
     * @param maxRating Calificación máxima
     * @return Lista de calificaciones en el rango especificado
     */
    @Query("SELECT pr FROM PlayerRating pr WHERE pr.currentRating BETWEEN :minRating AND :maxRating ORDER BY pr.currentRating DESC")
    List<PlayerRating> findByCurrentRatingBetween(
            @Param("minRating") BigDecimal minRating,
            @Param("maxRating") BigDecimal maxRating);

    /**
     * Busca calificaciones actualizadas después de una fecha específica
     * @param date Fecha de referencia
     * @return Lista de calificaciones actualizadas después de la fecha
     */
    @Query("SELECT pr FROM PlayerRating pr WHERE pr.lastUpdated > :date ORDER BY pr.lastUpdated DESC")
    List<PlayerRating> findByLastUpdatedAfter(@Param("date") LocalDateTime date);

    /**
     * Busca calificaciones actualizadas en un período específico
     * @param startDate Fecha de inicio del período
     * @param endDate Fecha de fin del período
     * @return Lista de calificaciones actualizadas en el período
     */
    @Query("SELECT pr FROM PlayerRating pr WHERE pr.lastUpdated BETWEEN :startDate AND :endDate ORDER BY pr.lastUpdated DESC")
    List<PlayerRating> findByLastUpdatedBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Busca calificaciones con un número mínimo de partidos jugados
     * @param minMatches Número mínimo de partidos
     * @return Lista de calificaciones con al menos el número especificado de partidos
     */
    @Query("SELECT pr FROM PlayerRating pr WHERE pr.matchesPlayed >= :minMatches ORDER BY pr.currentRating DESC")
    List<PlayerRating> findByMatchesPlayedGreaterThanEqual(@Param("minMatches") Integer minMatches);

    /**
     * Busca las mejores calificaciones para un rol específico (top N)
     * @param roleType Tipo de rol
     * @param limit Número máximo de resultados
     * @return Lista de las mejores calificaciones para el rol
     */
    @Query(value = "SELECT pr FROM PlayerRating pr WHERE pr.roleType = :roleType ORDER BY pr.currentRating DESC")
    List<PlayerRating> findTopByRoleTypeOrderByCurrentRatingDesc(
            @Param("roleType") RoleType roleType);

    /**
     * Busca calificaciones que están por debajo del mínimo base para su prioridad
     * @return Lista de calificaciones por debajo del mínimo
     */
    @Query("SELECT pr FROM PlayerRating pr WHERE " +
           "(pr.priorityLevel = 'PRINCIPAL' AND pr.currentRating < 70) OR " +
           "(pr.priorityLevel = 'SECUNDARIA' AND pr.currentRating < 60) OR " +
           "(pr.priorityLevel = 'TERCIARIA' AND pr.currentRating < 50)")
    List<PlayerRating> findRatingsBelowMinimum();

    /**
     * Obtiene estadísticas de calificación para un rol específico
     * @param roleType Tipo de rol
     * @return Array con [min, max, avg, count] de calificaciones para el rol
     */
    @Query("SELECT MIN(pr.currentRating), MAX(pr.currentRating), AVG(pr.currentRating), COUNT(pr) FROM PlayerRating pr WHERE pr.roleType = :roleType")
    Object[] getRatingStatisticsByRole(@Param("roleType") RoleType roleType);

    /**
     * Obtiene estadísticas de calificación para un nivel de prioridad específico
     * @param priorityLevel Nivel de prioridad
     * @return Array con [min, max, avg, count] de calificaciones para el nivel de prioridad
     */
    @Query("SELECT MIN(pr.currentRating), MAX(pr.currentRating), AVG(pr.currentRating), COUNT(pr) FROM PlayerRating pr WHERE pr.priorityLevel = :priorityLevel")
    Object[] getRatingStatisticsByPriority(@Param("priorityLevel") PriorityLevel priorityLevel);

    /**
     * Cuenta el número de calificaciones para un jugador específico
     * @param playerProfileId UUID del perfil del jugador
     * @return Número de calificaciones del jugador
     */
    @Query("SELECT COUNT(pr) FROM PlayerRating pr WHERE pr.playerProfile.atletaUuid = :playerProfileId")
    long countByPlayerProfileId(@Param("playerProfileId") UUID playerProfileId);

    /**
     * Cuenta el número de calificaciones para un rol específico
     * @param roleType Tipo de rol
     * @return Número de calificaciones para el rol
     */
    @Query("SELECT COUNT(pr) FROM PlayerRating pr WHERE pr.roleType = :roleType")
    long countByRoleType(@Param("roleType") RoleType roleType);

    /**
     * Verifica si existe una calificación para un jugador, rol y prioridad específicos
     * @param playerProfileId UUID del perfil del jugador
     * @param roleType Tipo de rol
     * @param priorityLevel Nivel de prioridad
     * @return true si existe, false en caso contrario
     */
    @Query("SELECT COUNT(pr) > 0 FROM PlayerRating pr WHERE pr.playerProfile.atletaUuid = :playerProfileId AND pr.roleType = :roleType AND pr.priorityLevel = :priorityLevel")
    boolean existsByPlayerProfileIdAndRoleTypeAndPriorityLevel(
            @Param("playerProfileId") UUID playerProfileId,
            @Param("roleType") RoleType roleType,
            @Param("priorityLevel") PriorityLevel priorityLevel);

    /**
     * Busca jugadores con calificaciones en múltiples roles (versátiles)
     * @param minRoles Número mínimo de roles diferentes
     * @return Lista de UUIDs de jugadores versátiles
     */
    @Query("SELECT pr.playerProfile.atletaUuid FROM PlayerRating pr GROUP BY pr.playerProfile.atletaUuid HAVING COUNT(DISTINCT pr.roleType) >= :minRoles")
    List<UUID> findVersatilePlayersByMinRoles(@Param("minRoles") Long minRoles);

    /**
     * Busca la calificación más alta de un jugador independientemente del rol
     * @param playerProfileId UUID del perfil del jugador
     * @return Optional con la calificación más alta del jugador
     */
    @Query("SELECT pr FROM PlayerRating pr WHERE pr.playerProfile.atletaUuid = :playerProfileId ORDER BY pr.currentRating DESC")
    Optional<PlayerRating> findHighestRatingByPlayerProfileId(@Param("playerProfileId") UUID playerProfileId);

    /**
     * Busca calificaciones de jugadores activos (con al menos un partido jugado)
     * @return Lista de calificaciones de jugadores activos
     */
    @Query("SELECT pr FROM PlayerRating pr WHERE pr.matchesPlayed > 0 ORDER BY pr.currentRating DESC")
    List<PlayerRating> findActivePlayerRatings();
}
