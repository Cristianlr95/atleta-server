package com.atleta.demo.entity;

import com.atleta.demo.enums.MatchResult;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Entidad inmutable que representa el historial de participación de un jugador en partidos.
 * Esta es la FUENTE DE VERDAD para todas las estadísticas históricas de los jugadores.
 * Una vez creado, NUNCA se modifica - garantiza la integridad histórica de los datos.
 * 
 * Requisitos implementados:
 * - 9.1: Registro inmutable del historial al terminar un partido
 * - 9.2: Estadísticas individuales (goles, asistencias) por partido
 * - 9.3: Cálculo y asignación de XP ganada
 * - 9.4: Registro del resultado del partido para cada jugador
 * - 9.5: Inclusión de la posición jugada
 */
@Entity
@Table(name = "player_history")
@Immutable // Hibernate annotation para entidad inmutable
public class PlayerHistory extends BaseEntity {

    /**
     * Referencia al partido (inmutable)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false, updatable = false)
    @NotNull(message = "El partido es obligatorio")
    private Match match;

    /**
     * Referencia al jugador (inmutable)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    @NotNull(message = "El jugador es obligatorio")
    private PlayerProfile player;

    /**
     * Referencia al equipo en el que jugó (inmutable)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false, updatable = false)
    @NotNull(message = "El equipo es obligatorio")
    private Team team;

    /**
     * Posición en la que jugó el jugador (inmutable)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false, updatable = false)
    @NotNull(message = "La posición es obligatoria")
    private Position position;

    /**
     * Número de goles anotados por el jugador en este partido (inmutable)
     */
    @Column(name = "goles", nullable = false, updatable = false)
    @Min(value = 0, message = "Los goles no pueden ser negativos")
    private Integer goles;

    /**
     * Número de asistencias realizadas por el jugador en este partido (inmutable)
     */
    @Column(name = "asistencias", nullable = false, updatable = false)
    @Min(value = 0, message = "Las asistencias no pueden ser negativas")
    private Integer asistencias;

    /**
     * Resultado del partido desde la perspectiva del jugador (inmutable)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, updatable = false)
    @NotNull(message = "El resultado es obligatorio")
    private MatchResult resultado;

    /**
     * Experiencia (XP) ganada por el jugador en este partido (inmutable)
     */
    @Column(name = "xp_ganada", nullable = false, updatable = false)
    @Min(value = 0, message = "La XP ganada no puede ser negativa")
    private Integer xpGanada;

    /**
     * Bonus XP por MVP del partido.
     * Se mantiene separado para auditoria del bonus.
     */
    @Column(name = "mvp_bonus_xp", nullable = false, updatable = false)
    @Min(value = 0, message = "El bonus MVP no puede ser negativo")
    private Integer mvpBonusXp;

    /**
     * Timestamp de creación del registro histórico (inmutable)
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public PlayerHistory() {
        // Constructor por defecto para JPA
    }

    /**
     * Constructor completo para crear un registro de historial
     * 
     * @param match Partido jugado
     * @param player Jugador participante
     * @param team Equipo en el que jugó
     * @param position Posición jugada
     * @param goles Goles anotados
     * @param asistencias Asistencias realizadas
     * @param resultado Resultado del partido
     * @param xpGanada XP ganada en el partido
     */
    public PlayerHistory(Match match, PlayerProfile player, Team team, Position position,
                        Integer goles, Integer asistencias, MatchResult resultado, Integer xpGanada) {
        this.match = match;
        this.player = player;
        this.team = team;
        this.position = position;
        this.goles = goles != null ? goles : 0;
        this.asistencias = asistencias != null ? asistencias : 0;
        this.resultado = resultado;
        this.xpGanada = xpGanada != null ? xpGanada : 0;
        this.mvpBonusXp = 0;
    }

    // Getters only (no setters para mantener inmutabilidad)
    public Match getMatch() {
        return match;
    }

    public PlayerProfile getPlayer() {
        return player;
    }

    public Team getTeam() {
        return team;
    }

    public Position getPosition() {
        return position;
    }

    public Integer getGoles() {
        return goles;
    }

    public Integer getAsistencias() {
        return asistencias;
    }

    public MatchResult getResultado() {
        return resultado;
    }

    public Integer getXpGanada() {
        return xpGanada;
    }

    public Integer getMvpBonusXp() {
        return mvpBonusXp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Utility methods (solo lectura)
    /**
     * Verifica si el jugador ganó el partido
     */
    public boolean isVictoria() {
        return MatchResult.VICTORIA.equals(resultado);
    }

    /**
     * Verifica si el jugador perdió el partido
     */
    public boolean isDerrota() {
        return MatchResult.DERROTA.equals(resultado);
    }

    /**
     * Verifica si el partido terminó en empate
     */
    public boolean isEmpate() {
        return MatchResult.EMPATE.equals(resultado);
    }

    /**
     * Verifica si el jugador anotó goles en el partido
     */
    public boolean hasGoles() {
        return goles != null && goles > 0;
    }

    /**
     * Verifica si el jugador realizó asistencias en el partido
     */
    public boolean hasAsistencias() {
        return asistencias != null && asistencias > 0;
    }

    /**
     * Calcula el total de contribuciones (goles + asistencias)
     */
    public Integer getTotalContribuciones() {
        return (goles != null ? goles : 0) + (asistencias != null ? asistencias : 0);
    }

    @Override
    public String toString() {
        return "PlayerHistory{" +
                "id=" + getId() +
                ", match=" + (match != null ? match.getId() : null) +
                ", player=" + (player != null ? player.getAtletaUuid() : null) +
                ", team=" + (team != null ? team.getId() : null) +
                ", position=" + (position != null ? position.getId() : null) +
                ", goles=" + goles +
                ", asistencias=" + asistencias +
                ", resultado=" + resultado +
                ", xpGanada=" + xpGanada +
                ", mvpBonusXp=" + mvpBonusXp +
                ", createdAt=" + createdAt +
                '}';
    }
}
