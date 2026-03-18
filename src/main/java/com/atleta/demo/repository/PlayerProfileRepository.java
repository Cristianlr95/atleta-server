package com.atleta.demo.repository;

import com.atleta.demo.entity.PlayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la entidad PlayerProfile.
 * Proporciona operaciones CRUD básicas y consultas personalizadas
 * para la gestión de perfiles de jugadores de fútbol.
 */
@Repository
public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, UUID> {

    /**
     * Busca un perfil de jugador por su alias
     * @param alias Alias del jugador
     * @return Optional con el perfil si existe
     */
    Optional<PlayerProfile> findByAlias(String alias);

    /**
     * Verifica si existe un perfil con el alias especificado
     * @param alias Alias a verificar
     * @return true si existe, false en caso contrario
     */
    boolean existsByAlias(String alias);

    /**
     * Busca perfiles por rango de trust score
     * @param minScore Puntuación mínima
     * @param maxScore Puntuación máxima
     * @return Lista de perfiles en el rango especificado
     */
    @Query("SELECT p FROM PlayerProfile p WHERE p.trustScore BETWEEN :minScore AND :maxScore ORDER BY p.trustScore DESC")
    List<PlayerProfile> findByTrustScoreBetween(@Param("minScore") Integer minScore, 
                                               @Param("maxScore") Integer maxScore);

    /**
     * Busca perfiles con trust score mayor o igual al especificado
     * @param minScore Puntuación mínima
     * @return Lista de perfiles ordenados por trust score descendente
     */
    @Query("SELECT p FROM PlayerProfile p WHERE p.trustScore >= :minScore ORDER BY p.trustScore DESC")
    List<PlayerProfile> findByTrustScoreGreaterThanEqual(@Param("minScore") Integer minScore);

    /**
     * Busca perfiles por alias (búsqueda parcial, case-insensitive)
     * @param alias Alias o parte del alias a buscar
     * @return Lista de perfiles que coinciden
     */
    @Query("SELECT p FROM PlayerProfile p WHERE LOWER(p.alias) LIKE LOWER(CONCAT('%', :alias, '%'))")
    List<PlayerProfile> findByAliasContainingIgnoreCase(@Param("alias") String alias);

    /**
     * Busca perfiles creados después de una fecha específica
     * @param fecha Fecha de referencia
     * @return Lista de perfiles creados después de la fecha
     */
    @Query("SELECT p FROM PlayerProfile p WHERE p.createdAt > :fecha ORDER BY p.createdAt DESC")
    List<PlayerProfile> findByCreatedAtAfter(@Param("fecha") LocalDateTime fecha);

    /**
     * Busca perfiles que tienen posiciones definidas
     * @return Lista de perfiles con al menos una posición
     */
    @Query("SELECT DISTINCT p FROM PlayerProfile p JOIN p.positions pos")
    List<PlayerProfile> findProfilesWithPositions();

    /**
     * Busca perfiles que NO tienen posiciones definidas
     * @return Lista de perfiles sin posiciones
     */
    @Query("SELECT p FROM PlayerProfile p WHERE p.positions IS EMPTY")
    List<PlayerProfile> findProfilesWithoutPositions();

    /**
     * Busca perfiles que son miembros de al menos un equipo
     * @return Lista de perfiles que pertenecen a equipos
     */
    @Query("SELECT DISTINCT p FROM PlayerProfile p JOIN TeamMember tm ON tm.player = p WHERE tm.activo = true")
    List<PlayerProfile> findProfilesWithActiveTeamMembership();

    /**
     * Busca perfiles por nombre del atleta asociado
     * @param nombre Nombre del atleta
     * @return Lista de perfiles cuyos atletas coinciden con el nombre
     */
    @Query("SELECT p FROM PlayerProfile p WHERE LOWER(p.athlete.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<PlayerProfile> findByAthleteNombreContainingIgnoreCase(@Param("nombre") String nombre);

    @Query("""
        SELECT p FROM PlayerProfile p
        WHERE LOWER(COALESCE(p.alias, '')) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(COALESCE(p.athlete.nombre, '')) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(COALESCE(p.athlete.email, '')) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY p.createdAt DESC
    """)
    List<PlayerProfile> searchForSocialLookup(@Param("query") String query);

    /**
     * Obtiene estadísticas básicas de trust scores
     * @return Array con [min, max, avg] de trust scores
     */
    @Query("SELECT MIN(p.trustScore), MAX(p.trustScore), AVG(p.trustScore) FROM PlayerProfile p")
    Object[] getTrustScoreStatistics();

    /**
     * Cuenta perfiles por rango de trust score
     * @param minScore Puntuación mínima
     * @param maxScore Puntuación máxima
     * @return Número de perfiles en el rango
     */
    @Query("SELECT COUNT(p) FROM PlayerProfile p WHERE p.trustScore BETWEEN :minScore AND :maxScore")
    long countByTrustScoreBetween(@Param("minScore") Integer minScore, @Param("maxScore") Integer maxScore);
}
