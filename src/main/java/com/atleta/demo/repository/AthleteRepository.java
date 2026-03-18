package com.atleta.demo.repository;

import com.atleta.demo.entity.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la entidad Athlete.
 * Proporciona operaciones CRUD básicas y consultas personalizadas
 * para la gestión de atletas en el sistema.
 */
@Repository
public interface AthleteRepository extends JpaRepository<Athlete, UUID> {

    /**
     * Busca un atleta por su email único
     * @param email Email del atleta
     * @return Optional con el atleta si existe
     */
    Optional<Athlete> findByEmail(String email);

    /**
     * Busca un atleta por su Google ID
     * @param googleId ID único de Google
     * @return Optional con el atleta si existe
     */
    Optional<Athlete> findByGoogleId(String googleId);

    /**
     * Verifica si existe un atleta con el email especificado
     * @param email Email a verificar
     * @return true si existe, false en caso contrario
     */
    boolean existsByEmail(String email);

    /**
     * Busca atletas por nombre (búsqueda parcial, case-insensitive)
     * @param nombre Nombre o parte del nombre a buscar
     * @return Lista de atletas que coinciden
     */
    @Query("SELECT a FROM Athlete a WHERE LOWER(a.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Athlete> findByNombreContainingIgnoreCase(@Param("nombre") String nombre);

    /**
     * Busca atletas creados después de una fecha específica
     * @param fecha Fecha de referencia
     * @return Lista de atletas creados después de la fecha
     */
    @Query("SELECT a FROM Athlete a WHERE a.createdAt > :fecha ORDER BY a.createdAt DESC")
    List<Athlete> findByCreatedAtAfter(@Param("fecha") LocalDateTime fecha);

    /**
     * Busca atletas que tienen perfil de jugador asociado
     * @return Lista de atletas con perfil de jugador
     */
    @Query("SELECT a FROM Athlete a WHERE a.playerProfile IS NOT NULL")
    List<Athlete> findAthletesWithPlayerProfile();

    /**
     * Busca atletas que NO tienen perfil de jugador asociado
     * @return Lista de atletas sin perfil de jugador
     */
    @Query("SELECT a FROM Athlete a WHERE a.playerProfile IS NULL")
    List<Athlete> findAthletesWithoutPlayerProfile();

    /**
     * Cuenta el número total de atletas registrados
     * @return Número total de atletas
     */
    @Query("SELECT COUNT(a) FROM Athlete a")
    long countAllAthletes();

    /**
     * Busca atletas registrados en un rango de fechas
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin Fecha de fin del rango
     * @return Lista de atletas registrados en el rango
     */
    @Query("SELECT a FROM Athlete a WHERE a.createdAt BETWEEN :fechaInicio AND :fechaFin ORDER BY a.createdAt DESC")
    List<Athlete> findByCreatedAtBetween(@Param("fechaInicio") LocalDateTime fechaInicio, 
                                        @Param("fechaFin") LocalDateTime fechaFin);
}