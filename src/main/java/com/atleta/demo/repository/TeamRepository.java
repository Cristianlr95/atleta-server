package com.atleta.demo.repository;

import com.atleta.demo.entity.Team;
import com.atleta.demo.entity.PlayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Team.
 * Proporciona operaciones CRUD básicas y consultas personalizadas
 * para la gestión de equipos de fútbol.
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    /**
     * Busca un equipo por su nombre único
     * @param nombre Nombre del equipo
     * @return Optional con el equipo si existe
     */
    Optional<Team> findByNombre(String nombre);

    /**
     * Verifica si existe un equipo con el nombre especificado
     * @param nombre Nombre a verificar
     * @return true si existe, false en caso contrario
     */
    boolean existsByNombre(String nombre);

    /**
     * Busca equipos por nombre (búsqueda parcial, case-insensitive)
     * @param nombre Nombre o parte del nombre a buscar
     * @return Lista de equipos que coinciden
     */
    @Query("SELECT t FROM Team t WHERE LOWER(t.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Team> findByNombreContainingIgnoreCase(@Param("nombre") String nombre);

    /**
     * Busca equipos creados por un jugador específico
     * @param creador Perfil del jugador creador
     * @return Lista de equipos creados por el jugador
     */
    List<Team> findByCreador(PlayerProfile creador);

    List<Team> findByCreadorAndArchivedFalse(PlayerProfile creador);

    /**
     * Busca equipos creados después de una fecha específica
     * @param fecha Fecha de referencia
     * @return Lista de equipos creados después de la fecha
     */
    @Query("SELECT t FROM Team t WHERE t.createdAt > :fecha ORDER BY t.createdAt DESC")
    List<Team> findByCreatedAtAfter(@Param("fecha") LocalDateTime fecha);

    /**
     * Busca equipos por año de fundación
     * @param anio Año de fundación
     * @return Lista de equipos fundados en el año especificado
     */
    List<Team> findByAnioFundacion(Integer anio);

    /**
     * Busca equipos fundados en un rango de años
     * @param anioInicio Año de inicio del rango
     * @param anioFin Año de fin del rango
     * @return Lista de equipos fundados en el rango
     */
    @Query("SELECT t FROM Team t WHERE t.anioFundacion BETWEEN :anioInicio AND :anioFin ORDER BY t.anioFundacion DESC")
    List<Team> findByAnioFundacionBetween(@Param("anioInicio") Integer anioInicio, 
                                         @Param("anioFin") Integer anioFin);

    /**
     * Busca equipos que tienen miembros activos
     * @return Lista de equipos con al menos un miembro activo
     */
    @Query("SELECT DISTINCT t FROM Team t JOIN t.members m WHERE m.activo = true")
    List<Team> findTeamsWithActiveMembers();

    /**
     * Busca equipos que NO tienen miembros activos
     * @return Lista de equipos sin miembros activos
     */
    @Query("SELECT t FROM Team t WHERE NOT EXISTS (SELECT m FROM TeamMember m WHERE m.team = t AND m.activo = true)")
    List<Team> findTeamsWithoutActiveMembers();

    /**
     * Busca equipos donde un jugador específico es miembro activo
     * @param player Perfil del jugador
     * @return Lista de equipos donde el jugador es miembro activo
     */
    @Query("SELECT t FROM Team t JOIN t.members m WHERE m.player = :player AND m.activo = true AND t.archived = false")
    List<Team> findTeamsByActiveMember(@Param("player") PlayerProfile player);

    /**
     * Cuenta el número de miembros activos de un equipo
     * @param teamId ID del equipo
     * @return Número de miembros activos
     */
    @Query("SELECT COUNT(m) FROM TeamMember m WHERE m.team.id = :teamId AND m.activo = true")
    long countActiveMembersByTeamId(@Param("teamId") Long teamId);

    /**
     * Busca equipos ordenados por número de miembros activos (descendente)
     * @return Lista de equipos ordenados por número de miembros
     */
    @Query("SELECT t FROM Team t LEFT JOIN t.members m WHERE m.activo = true OR m IS NULL " +
           "GROUP BY t ORDER BY COUNT(m) DESC")
    List<Team> findTeamsOrderByActiveMembersCount();

    /**
     * Busca equipos que han participado en partidos
     * @return Lista de equipos que han participado en al menos un partido
     */
    @Query("SELECT DISTINCT t FROM Team t JOIN MatchTeam mt ON mt.team = t")
    List<Team> findTeamsWithMatchHistory();

    /**
     * Busca equipos creados en un rango de fechas
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin Fecha de fin del rango
     * @return Lista de equipos creados en el rango
     */
    @Query("SELECT t FROM Team t WHERE t.createdAt BETWEEN :fechaInicio AND :fechaFin ORDER BY t.createdAt DESC")
    List<Team> findByCreatedAtBetween(@Param("fechaInicio") LocalDateTime fechaInicio, 
                                     @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * Busca equipos que tienen logo configurado
     * @return Lista de equipos con logo URL no nulo
     */
    @Query("SELECT t FROM Team t WHERE t.logoUrl IS NOT NULL AND t.logoUrl != ''")
    List<Team> findTeamsWithLogo();
}
