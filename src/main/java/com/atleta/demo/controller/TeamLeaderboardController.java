package com.atleta.demo.controller;

import com.atleta.demo.dto.response.TeamLeaderboardEntryResponse;
import com.atleta.demo.dto.response.TeamExternalRecordResponse;
import com.atleta.demo.security.AuthenticatedUserUtils;
import com.atleta.demo.service.TeamLeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamLeaderboardController {
    private final TeamLeaderboardService teamLeaderboardService;

    public TeamLeaderboardController(TeamLeaderboardService teamLeaderboardService) {
        this.teamLeaderboardService = teamLeaderboardService;
    }

    @GetMapping("/{teamId}/leaderboard")
    @Operation(summary = "Obtener ranking OVR del equipo",
            description = "Retorna el ranking completo solo a creadores o miembros activos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking obtenido"),
            @ApiResponse(responseCode = "403", description = "El usuario no pertenece al equipo"),
            @ApiResponse(responseCode = "404", description = "Equipo no encontrado")
    })
    public ResponseEntity<List<TeamLeaderboardEntryResponse>> getLeaderboard(
            @PathVariable Long teamId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            return ResponseEntity.ok(teamLeaderboardService.getLeaderboard(
                    teamId, AuthenticatedUserUtils.currentUserUuid(jwt)));
        } catch (AccessDeniedException exception) {
            return ResponseEntity.status(403).build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{teamId}/external-record")
    @Operation(summary = "Obtener récord competitivo del equipo",
            description = "Cuenta solo partidos finalizados contra otro equipo. Los entrenamientos internos no suman.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Récord obtenido"),
            @ApiResponse(responseCode = "403", description = "El usuario no pertenece al equipo"),
            @ApiResponse(responseCode = "404", description = "Equipo no encontrado")
    })
    public ResponseEntity<TeamExternalRecordResponse> getExternalRecord(
            @PathVariable Long teamId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            return ResponseEntity.ok(teamLeaderboardService.getExternalRecord(
                    teamId, AuthenticatedUserUtils.currentUserUuid(jwt)));
        } catch (AccessDeniedException exception) {
            return ResponseEntity.status(403).build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }
}
