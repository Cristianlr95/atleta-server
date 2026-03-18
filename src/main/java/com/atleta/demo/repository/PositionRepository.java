package com.atleta.demo.repository;

import com.atleta.demo.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Position.
 * Proporciona operaciones CRUD básicas y consultas personalizadas
 * para la gestión del catálogo de posiciones de fútbol.
 */
@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    /**
     * Busca una posición por su nombre
     * @param nombre Nombre de la posición
     * @return Optional con la posición si existe
     */
    Optional<Position> findByNombre(String nombre);

    /**
     * Verifica si existe una posición con el nombre especificado
     * @param nombre Nombre a verificar
     * @return true si existe, false en caso contrario
     */
    boolean existsByNombre(String nombre);

    /**
     * Busca posiciones por nombre (búsqueda parcial, case-insensitive)
     * @param nombre Nombre o parte del nombre a buscar
     * @return Lista de posiciones que coinciden
     */
    @Query("SELECT p FROM Position p WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Position> findByNombreContainingIgnoreCase(@Param("nombre") String nombre);

    /**
     * Obtiene todas las posiciones ordenadas por nombre
     * @return Lista de todas las posiciones ordenadas alfabéticamente
     */
    @Query("SELECT p FROM Position p ORDER BY p.nombre ASC")
    List<Position> findAllOrderByNombre();

    /**
     * Busca posiciones que están siendo utilizadas por jugadores
     * @return Lista de posiciones que tienen al menos un jugador asignado
     */
    @Query("SELECT DISTINCT p FROM Position p JOIN PlayerPosition pp ON pp.position = p")
    List<Position> findPositionsInUse();

    /**
     * Busca posiciones que NO están siendo utilizadas por jugadores
     * @return Lista de posiciones sin jugadores asignados
     */
    @Query("SELECT p FROM Position p WHERE NOT EXISTS (SELECT pp FROM PlayerPosition pp WHERE pp.position = p)")
    List<Position> findPositionsNotInUse();

    /**
     * Cuenta cuántos jugadores tienen asignada una posición específica
     * @param positionId ID de la posición
     * @return Número de jugadores con esa posición
     */
    @Query("SELECT COUNT(pp) FROM PlayerPosition pp WHERE pp.position.id = :positionId")
    long countPlayersByPositionId(@Param("positionId") Long positionId);

    /**
     * Busca posiciones ordenadas por popularidad (número de jugadores que la tienen)
     * @return Lista de posiciones ordenadas por número de jugadores (descendente)
     */
    @Query("SELECT p FROM Position p LEFT JOIN PlayerPosition pp ON pp.position = p " +
           "GROUP BY p ORDER BY COUNT(pp) DESC")
    List<Position> findPositionsOrderByPopularity();
}