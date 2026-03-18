package com.atleta.demo.controller;

import com.atleta.demo.dto.response.PositionResponse;
import com.atleta.demo.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controlador REST para la gestión del catálogo de posiciones.
 * Proporciona endpoints para consultar las posiciones disponibles en el sistema.
 * 
 * Requisitos implementados:
 * - 3.1: Catálogo fijo de posiciones (Portero, Defensa, Carrilero, Mediocampista, Delantero, DT)
 */
@RestController
@RequestMapping("/api/v1/positions")
@Tag(name = "Posiciones", description = "Catálogo de posiciones de fútbol y gestión de prioridades")
public class PositionController {

    private static final Logger logger = LoggerFactory.getLogger(PositionController.class);

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    /**
     * Obtiene todas las posiciones disponibles en el catálogo.
     * Requisito: 3.1 (catálogo fijo de posiciones)
     */
    @GetMapping
    @Operation(summary = "Obtener todas las posiciones", 
               description = "Obtiene el catálogo completo de posiciones de fútbol disponibles")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Catálogo de posiciones obtenido exitosamente")
    })
    public ResponseEntity<List<PositionResponse>> getAllPositions() {
        
        logger.debug("Obteniendo todas las posiciones del catálogo");
        
        List<PositionResponse> positions = positionService.getAllPositions();
        return ResponseEntity.ok(positions);
    }

    /**
     * Obtiene una posición específica por su ID.
     * Requisito: 3.1 (consulta de posiciones del catálogo)
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener posición por ID", 
               description = "Obtiene una posición específica del catálogo por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Posición encontrada"),
        @ApiResponse(responseCode = "404", description = "Posición no encontrada")
    })
    public ResponseEntity<PositionResponse> getPositionById(
            @Parameter(description = "ID de la posición")
            @PathVariable Long id) {
        
        logger.debug("Buscando posición por ID: {}", id);
        
        Optional<PositionResponse> positionOpt = positionService.findById(id);
        
        return positionOpt.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Busca posiciones por nombre (búsqueda parcial).
     * Requisito: 3.1 (búsqueda en catálogo de posiciones)
     */
    @GetMapping("/search")
    @Operation(summary = "Buscar posiciones por nombre", 
               description = "Busca posiciones que contengan el texto especificado en su nombre")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Búsqueda completada exitosamente")
    })
    public ResponseEntity<List<PositionResponse>> searchPositionsByName(
            @Parameter(description = "Texto a buscar en el nombre de la posición")
            @RequestParam String nombre) {
        
        logger.debug("Buscando posiciones por nombre: {}", nombre);
        
        List<PositionResponse> positions = positionService.searchByName(nombre);
        return ResponseEntity.ok(positions);
    }
}