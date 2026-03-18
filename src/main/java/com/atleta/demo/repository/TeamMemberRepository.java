package com.atleta.demo.repository;

import com.atleta.demo.entity.TeamMember;
import com.atleta.demo.entity.Team;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.enums.PlayerRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la entidad TeamMember.
 * Proporciona operaciones CRUD básicas y consultas personalizadas
 * para la gestión de membresías de equipos.
 */
@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    /**
     * Busca todos los miembros activos de un equipo
     * @param team Equipo
     * @return Lista de miembros activos del equipo
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.team = :team AND tm.activo = true ORDER BY tm.joinedAt ASC")
    List<TeamMember> findActiveByTeam(@Param("team") Team team);

    /**
     * Busca todos los miembros (activos e inactivos) de un equipo
     * @param team Equipo
     * @return Lista de todos los miembros del equipo
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.team = :team ORDER BY tm.joinedAt ASC")
    List<TeamMember> findByTeam(Team team);

    /**
     * Busca todos los equipos donde un jugador es miembro activo
     * @param player Perfil del jugador
     * @return Lista de membresías activas del jugador
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.player = :player AND tm.activo = true ORDER BY tm.joinedAt DESC")
    List<TeamMember> findActiveByPlayer(@Param("player") PlayerProfile player);

    /**
     * Busca todos los equipos donde un jugador ha sido miembro
     * @param player Perfil del jugador
     * @return Lista de todas las membresías del jugador
     */
    List<TeamMember> findByPlayer(PlayerProfile player);

    /**
     * Busca la membresía específica de un jugador en un equipo
     * @param team Equipo
     * @param player Perfil del jugador
     * @return Optional con la membresía si existe
     */
    Optional<TeamMember> findByTeamAndPlayer(Team team, PlayerProfile player);

    /**
     * Busca la membresía activa específica de un jugador en un equipo
     * @param team Equipo
     * @param player Perfil del jugador
     * @return Optional con la membresía activa si existe
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.team = :team AND tm.player = :player AND tm.activo = true")
    Optional<TeamMember> findActiveByTeamAndPlayer(@Param("team") Team team, @Param("player") PlayerProfile player);

    /**
     * Busca miembros por rol en un equipo específico
     * @param team Equipo
     * @param rol Rol del jugador
     * @return Lista de miembros con el rol especificado
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.team = :team AND tm.rol = :rol AND tm.activo = true")
    List<TeamMember> findByTeamAndRol(@Param("team") Team team, @Param("rol") PlayerRole rol);

    /**
     * Busca capitanes activos de un equipo
     * @param team Equipo
     * @return Lista de capitanes activos del equipo
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.team = :team AND tm.rol = 'CAPITAN' AND tm.activo = true")
    List<TeamMember> findCaptainsByTeam(@Param("team") Team team);

    /**
     * Busca directores técnicos activos de un equipo
     * @param team Equipo
     * @return Lista de DTs activos del equipo
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.team = :team AND tm.rol = 'DT' AND tm.activo = true")
    List<TeamMember> findCoachesByTeam(@Param("team") Team team);

    /**
     * Verifica si un jugador es miembro activo de un equipo
     * @param team Equipo
     * @param player Perfil del jugador
     * @return true si es miembro activo, false en caso contrario
     */
    @Query("SELECT COUNT(tm) > 0 FROM TeamMember tm WHERE tm.team = :team AND tm.player = :player AND tm.activo = true")
    boolean isActiveMember(@Param("team") Team team, @Param("player") PlayerProfile player);

    /**
     * Cuenta miembros activos de un equipo
     * @param team Equipo
     * @return Número de miembros activos
     */
    @Query("SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team = :team AND tm.activo = true")
    long countActiveByTeam(@Param("team") Team team);

    /**
     * Cuenta equipos activos de un jugador
     * @param player Perfil del jugador
     * @return Número de equipos donde es miembro activo
     */
    @Query("SELECT COUNT(tm) FROM TeamMember tm WHERE tm.player = :player AND tm.activo = true")
    long countActiveByPlayer(@Param("player") PlayerProfile player);

    /**
     * Busca miembros que se unieron después de una fecha específica
     * @param fecha Fecha de referencia
     * @return Lista de miembros que se unieron después de la fecha
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.joinedAt > :fecha ORDER BY tm.joinedAt DESC")
    List<TeamMember> findByJoinedAtAfter(@Param("fecha") LocalDateTime fecha);

    /**
     * Busca miembros que se unieron en un rango de fechas
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin Fecha de fin del rango
     * @return Lista de miembros que se unieron en el rango
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.joinedAt BETWEEN :fechaInicio AND :fechaFin ORDER BY tm.joinedAt DESC")
    List<TeamMember> findByJoinedAtBetween(@Param("fechaInicio") LocalDateTime fechaInicio, 
                                          @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * Busca miembros por UUID del atleta
     * @param atletaUuid UUID del atleta
     * @return Lista de membresías del atleta
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.player.atletaUuid = :atletaUuid")
    List<TeamMember> findByPlayerAtletaUuid(@Param("atletaUuid") UUID atletaUuid);

    /**
     * Busca miembros activos por UUID del atleta
     * @param atletaUuid UUID del atleta
     * @return Lista de membresías activas del atleta
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.player.atletaUuid = :atletaUuid AND tm.activo = true")
    List<TeamMember> findActiveByPlayerAtletaUuid(@Param("atletaUuid") UUID atletaUuid);

    /**
     * Busca equipos más populares por número de miembros activos
     * @return Lista de equipos ordenados por número de miembros activos (descendente)
     */
    @Query("SELECT tm.team FROM TeamMember tm WHERE tm.activo = true GROUP BY tm.team ORDER BY COUNT(tm) DESC")
    List<Team> findTeamsOrderByActiveMembersCount();
}