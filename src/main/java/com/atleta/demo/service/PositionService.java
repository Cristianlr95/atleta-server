package com.atleta.demo.service;

import com.atleta.demo.dto.response.PositionResponse;
import com.atleta.demo.entity.Position;
import com.atleta.demo.repository.PositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión del catálogo de posiciones.
 * Proporciona operaciones de consulta sobre el catálogo fijo de posiciones.
 * 
 * Requisitos implementados:
 * - 3.1: Catálogo fijo de posiciones (Portero, Defensa, Carrilero, Mediocampista, Delantero, DT)
 */
@Service
@Transactional(readOnly = true)
public class PositionService {

    private static final Logger logger = LoggerFactory.getLogger(PositionService.class);

    private final PositionRepository positionRepository;

    public PositionService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    /**
     * Obtiene todas las posiciones del catálogo.
     * Requisito: 3.1 (catálogo fijo de posiciones)
     */
    public List<PositionResponse> getAllPositions() {
        logger.debug("Obteniendo todas las posiciones del catálogo");
        
        List<Position> positions = positionRepository.findAll();
        
        return positions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Busca una posición por su ID.
     * Requisito: 3.1 (consulta de posiciones del catálogo)
     */
    public Optional<PositionResponse> findById(Long id) {
        logger.debug("Buscando posición por ID: {}", id);
        
        return positionRepository.findById(id)
                .map(this::convertToResponse);
    }

    /**
     * Busca posiciones por nombre (búsqueda parcial).
     * Requisito: 3.1 (búsqueda en catálogo de posiciones)
     */
    public List<PositionResponse> searchByName(String nombre) {
        logger.debug("Buscando posiciones por nombre: {}", nombre);
        
        List<Position> positions = positionRepository.findByNombreContainingIgnoreCase(nombre);
        
        return positions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convierte una entidad Position a PositionResponse.
     */
    private PositionResponse convertToResponse(Position position) {
        return new PositionResponse(
                position.getId(),
                position.getNombre()
        );
    }
}