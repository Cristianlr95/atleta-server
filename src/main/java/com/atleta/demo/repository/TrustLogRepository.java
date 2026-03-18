package com.atleta.demo.repository;

import com.atleta.demo.entity.TrustLog;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio para la entidad TrustLog.
 * Proporciona operaciones CRUD básicas y consultas personalizadas
 * para la gestión del registro de cambios de confianza de jugadores.
 */
@Repository
public interface TrustLogRepository extends JpaRepository<TrustLog, Long> {

    /**
     * Busca todos los logs de confianza de un jugador específico
     * @param player Perfil del jugador
     * @return Lista de logs ordenados por fecha (más reciente primero)
     */
    @Query("SELECT tl FROM TrustLog tl WHERE tl.player = :player ORDER BY tl.createdAt DESC")
    List<TrustLog> findByPlayerOrderByCreatedAtDesc(@Param("player") PlayerProfile player);

    /**
     * Busca logs de confianza relacionados con un partido específico
     * @param match Partido
     * @return Lista de logs relacionados con el partido
     */
    List<TrustLog> findByMatch(Match match);

    /**
     * Busca logs de confianza de un jugador relacionados con partidos
     * @param player Perfil del jugador
     * @return Lista de logs del jugador que están relacionados con partidos
     */
    @Query("SELECT tl FROM TrustLog tl WHERE tl.player = :player AND tl.match IS NOT NULL ORDER BY tl.createdAt DESC")
    List<TrustLog> findByPlayerWithMatchOrderByCreatedAtDesc(@Param("player") PlayerProfile player);

    /**
     * Busca logs de confianza de un jugador NO relacionados con partidos
     * @param player Perfil del jugador
     * @return Lista de logs del jugador que NO están relacionados con partidos
     */
    @Query("SELECT tl FROM TrustLog tl WHERE tl.player = :player AND tl.match IS NULL ORDER BY tl.createdAt DESC")
    List<TrustLog> findByPlayerWithoutMatchOrderByCreatedAtDesc(@Param("player") PlayerProfile player);

    /**
     * Busca logs con cambios positivos de confianza
     * @param player Perfil del jugador
     * @return Lista de logs con cambios positivos
     */
    @Query("SELECT tl FROM TrustLog tl WHERE tl.player = :player AND tl.cambio > 0 ORDER BY tl.createdAt DESC")
    List<TrustLog> findPositiveChangesByPlayer(@Param("player") PlayerProfile player);

    /**
     * Busca logs con cambios negativos de confianza
     * @param player Perfil del jugador
     * @return Lista de logs con cambios negativos
     */
    @Query("SELECT tl FROM TrustLog tl WHERE tl.player = :player AND tl.cambio < 0 ORDER BY tl.createdAt DESC")
    List<TrustLog> findNegativeChangesByPlayer(@Param("player") PlayerProfile player);

    /**
     * Busca logs por motivo específico
     * @param motivo Motivo del cambio
     * @return Lista de logs con el motivo especificado
     */
    @Query("SELECT tl FROM TrustLog tl WHERE LOWER(tl.motivo) LIKE LOWER(CONCAT('%', :motivo, '%')) ORDER BY tl.createdAt DESC")
    List<TrustLog> findByMotivoContainingIgnoreCase(@Param("motivo") String motivo);

    /**
     * Busca logs en un rango de fechas
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de logs en el rango especificado
     */
    @Query("SELECT tl FROM TrustLog tl WHERE tl.createdAt BETWEEN :fechaInicio AND :fechaFin ORDER BY tl.createdAt DESC")
    List<TrustLog> findByCreatedAtBetween(@Param("fechaInicio") LocalDateTime fechaInicio, 
                                         @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * Suma total de cambios de confianza de un jugador
     * @param player Perfil del jugador
     * @return Suma total de cambios (puede ser positiva o negativa)
     */
    @Query("SELECT SUM(tl.cambio) FROM TrustLog tl WHERE tl.player = :player")
    Long getTotalChangesByPlayer(@Param("player") PlayerProfile player);

    /**
     * Cuenta cambios positivos de confianza de un jugador
     * @param player Perfil del jugador
     * @return Número de cambios positivos
     */
    @Query("SELECT COUNT(tl) FROM TrustLog tl WHERE tl.player = :player AND tl.cambio > 0")
    long countPositiveChangesByPlayer(@Param("player") PlayerProfile player);

    /**
     * Cuenta cambios negativos de confianza de un jugador
     * @param player Perfil del jugador
     * @return Número de cambios negativos
     */
    @Query("SELECT COUNT(tl) FROM TrustLog tl WHERE tl.player = :player AND tl.cambio < 0")
    long countNegativeChangesByPlayer(@Param("player") PlayerProfile player);

    /**
     * Busca el último cambio de confianza de un jugador
     * @param player Perfil del jugador
     * @return Lista con el último cambio (máximo 1 elemento)
     */
    @Query("SELECT tl FROM TrustLog tl WHERE tl.player = :player ORDER BY tl.createdAt DESC")
    List<TrustLog> findLatestChangeByPlayer(@Param("player") PlayerProfile player);

    /**
     * Busca logs de confianza por UUID del atleta
     * @param atletaUuid UUID del atleta
     * @return Lista de logs del atleta
     */
    @Query("SELECT tl FROM TrustLog tl WHERE tl.player.atletaUuid = :atletaUuid ORDER BY tl.createdAt DESC")
    List<TrustLog> findByPlayerAtletaUuidOrderByCreatedAtDesc(@Param("atletaUuid") UUID atletaUuid);

    /**
     * Busca logs con cambios mayores o iguales al especificado
     * @param minCambio Cambio mínimo (puede ser negativo)
     * @return Lista de logs que cumplen el criterio
     */
    @Query("SELECT tl FROM TrustLog tl WHERE tl.cambio >= :minCambio ORDER BY tl.cambio DESC")
    List<TrustLog> findByChangeGreaterThanEqual(@Param("minCambio") Integer minCambio);

    /**
     * Busca logs con cambios menores o iguales al especificado
     * @param maxCambio Cambio máximo (puede ser negativo)
     * @return Lista de logs que cumplen el criterio
     */
    @Query("SELECT tl FROM TrustLog tl WHERE tl.cambio <= :maxCambio ORDER BY tl.cambio ASC")
    List<TrustLog> findByChangeLessThanEqual(@Param("maxCambio") Integer maxCambio);

    /**
     * Obtiene estadísticas de cambios de confianza por jugador
     * @param player Perfil del jugador
     * @return Array con [total_cambios, cambios_positivos, cambios_negativos, suma_total]
     */
    @Query("SELECT COUNT(tl), " +
           "SUM(CASE WHEN tl.cambio > 0 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN tl.cambio < 0 THEN 1 ELSE 0 END), " +
           "SUM(tl.cambio) " +
           "FROM TrustLog tl WHERE tl.player = :player")
    Object[] getTrustChangeStatsByPlayer(@Param("player") PlayerProfile player);

    /**
     * Busca jugadores con más cambios de confianza registrados
     * @return Lista de jugadores ordenados por número de cambios (descendente)
     */
    @Query("SELECT tl.player FROM TrustLog tl GROUP BY tl.player ORDER BY COUNT(tl) DESC")
    List<PlayerProfile> findPlayersOrderByTrustLogCount();

    /**
     * Busca logs de confianza relacionados con un partido y jugador específicos
     * @param match Partido
     * @param player Perfil del jugador
     * @return Lista de logs relacionados con el partido y jugador
     */
    List<TrustLog> findByMatchAndPlayer(Match match, PlayerProfile player);

    /**
     * Busca los cambios más significativos (mayor valor absoluto)
     * @return Lista de logs ordenados por valor absoluto del cambio (descendente)
     */
    @Query("SELECT tl FROM TrustLog tl ORDER BY ABS(tl.cambio) DESC")
    List<TrustLog> findMostSignificantChanges();

    /**
     * Busca logs recientes (últimas 24 horas)
     * @return Lista de logs de las últimas 24 horas
     */
    @Query("SELECT tl FROM TrustLog tl WHERE tl.createdAt > :fecha ORDER BY tl.createdAt DESC")
    List<TrustLog> findRecentLogs(@Param("fecha") LocalDateTime fecha);
}