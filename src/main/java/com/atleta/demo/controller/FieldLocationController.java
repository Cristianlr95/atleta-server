package com.atleta.demo.controller;

import com.atleta.demo.dto.request.CreateFieldLocationRequest;
import com.atleta.demo.dto.request.UpdateFieldLocationRequest;
import com.atleta.demo.dto.response.FieldLocationResponse;
import com.atleta.demo.service.FieldLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fields")
@Tag(name = "Canchas", description = "Catalogo de canchas con coordenadas para mapa")
public class FieldLocationController {

    private final FieldLocationService fieldLocationService;

    public FieldLocationController(FieldLocationService fieldLocationService) {
        this.fieldLocationService = fieldLocationService;
    }

    @GetMapping
    @Operation(summary = "Listar canchas", description = "Lista canchas por ciudad y/o estado activo")
    public ResponseEntity<List<FieldLocationResponse>> getFieldLocations(
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) Boolean soloActivas) {

        return ResponseEntity.ok(fieldLocationService.getFieldLocations(ciudad, soloActivas));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear cancha", description = "Crea una nueva cancha con direccion y coordenadas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cancha creada"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "409", description = "Cancha duplicada")
    })
    public ResponseEntity<FieldLocationResponse> createFieldLocation(@Valid @RequestBody CreateFieldLocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fieldLocationService.createFieldLocation(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Editar cancha", description = "Edita una cancha existente por id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cancha editada"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "404", description = "Cancha no encontrada")
    })
    public ResponseEntity<FieldLocationResponse> updateFieldLocation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFieldLocationRequest request) {
        try {
            return ResponseEntity.ok(fieldLocationService.updateFieldLocation(id, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
