package com.atleta.demo.controller;

import com.atleta.demo.dto.response.MatchAiSummaryResponse;
import com.atleta.demo.security.AuthenticatedUserUtils;
import com.atleta.demo.service.MatchAiSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Optional post-match experience. A provider outage returns a deterministic summary, never a match failure. */
@RestController
@RequestMapping("/api/v1/matches")
public class MatchAiSummaryController {
    private final MatchAiSummaryService matchAiSummaryService;

    public MatchAiSummaryController(MatchAiSummaryService matchAiSummaryService) {
        this.matchAiSummaryService = matchAiSummaryService;
    }

    @PostMapping("/{matchId}/ai-summary")
    @Operation(summary = "Generar resumen post-partido", description = "Genera un relato desde hechos ya confirmados. Si IA no está configurada, devuelve una versión determinista.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen generado"),
            @ApiResponse(responseCode = "400", description = "El partido aún no está finalizado"),
            @ApiResponse(responseCode = "403", description = "No participa en el partido"),
            @ApiResponse(responseCode = "404", description = "Partido no encontrado")
    })
    public ResponseEntity<MatchAiSummaryResponse> generate(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(matchAiSummaryService.generate(matchId, AuthenticatedUserUtils.currentUserUuid(jwt)));
    }
}
