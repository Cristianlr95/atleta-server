package com.atleta.demo.service;

import com.atleta.demo.ai.AiGenerationRequest;
import com.atleta.demo.ai.AiProvider;
import com.atleta.demo.dto.response.MatchAiSummaryResponse;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchEvent;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.enums.EventType;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.repository.MatchEventRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/** Generates a post-match narrative from persisted facts; it never affects match scoring or closure. */
@Service
public class MatchAiSummaryService {
    public static final String PROMPT_VERSION = "match-summary-v1";
    private static final String SYSTEM_INSTRUCTION = "Eres el relator de ATLETA. Usa exclusivamente los hechos entregados. "
            + "No inventes goles, jugadores, minutos, resultados ni premios. Responde solo JSON con title, summary, highlights y mvpComment. "
            + "highlights debe tener entre 1 y 3 textos breves en español.";

    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchEventRepository matchEventRepository;
    private final AiProvider aiProvider;
    private final ObjectMapper objectMapper;

    public MatchAiSummaryService(MatchRepository matchRepository, MatchPlayerRepository matchPlayerRepository,
                                 MatchEventRepository matchEventRepository, AiProvider aiProvider, ObjectMapper objectMapper) {
        this.matchRepository = matchRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchEventRepository = matchEventRepository;
        this.aiProvider = aiProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public MatchAiSummaryResponse generate(Long matchId, UUID actorUuid) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));
        if (match.getEstado() != MatchStatus.FINALIZADO) {
            throw new IllegalArgumentException("El resumen IA solo está disponible para partidos finalizados");
        }
        if (!matchPlayerRepository.findByMatchAndPlayerAtletaUuid(match, actorUuid).isPresent()
                && !match.getCreador().getAtletaUuid().equals(actorUuid)) {
            throw new AccessDeniedException("El resumen está disponible solo para participantes del partido");
        }

        Facts facts = collectFacts(match);
        try {
            String payload = aiProvider.generateJson(new AiGenerationRequest(PROMPT_VERSION, SYSTEM_INSTRUCTION, facts.asMap()));
            Narrative narrative = parseAndValidate(payload, facts);
            return new MatchAiSummaryResponse(matchId, PROMPT_VERSION, aiProvider.name(), narrative.title(), narrative.summary(), narrative.highlights(), narrative.mvpComment());
        } catch (RuntimeException exception) {
            return fallback(matchId, facts);
        }
    }

    private Facts collectFacts(Match match) {
        List<MatchPlayer> players = matchPlayerRepository.findByMatch(match);
        List<MatchEvent> goals = matchEventRepository.findGoalsByMatch(match).stream()
                .filter(event -> event.getTipoEvento() == EventType.GOL && event.isFullyConfirmed()).toList();
        Map<String, Long> scorers = goals.stream().collect(Collectors.groupingBy(event -> event.getPlayer().getAlias(), LinkedHashMap::new, Collectors.counting()));
        List<String> aliases = players.stream().map(player -> player.getPlayer().getAlias()).filter(Objects::nonNull).distinct().sorted().toList();
        return new Facts(match.getFinalScoreLocal(), match.getFinalScoreAway(), match.getModalidad().name(), aliases,
                scorers, match.getMvpUser() == null ? null : match.getMvpUser().getAlias());
    }

    private Narrative parseAndValidate(String payload, Facts facts) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String title = text(root, "title");
            String summary = text(root, "summary");
            String mvpComment = text(root, "mvpComment");
            List<String> highlights = new ArrayList<>();
            root.path("highlights").forEach(node -> { if (node.isTextual()) highlights.add(node.asText().trim()); });
            if (title.isBlank() || summary.isBlank() || highlights.isEmpty() || highlights.size() > 3 || title.length() > 100 || summary.length() > 500) {
                throw new IllegalArgumentException("Respuesta IA no cumple el contrato");
            }
            String allText = String.join(" ", title, summary, mvpComment, String.join(" ", highlights));
            if (facts.aliases().stream().noneMatch(alias -> allText.contains(alias)) && !facts.aliases().isEmpty()) {
                throw new IllegalArgumentException("Respuesta IA sin referencias verificables");
            }
            return new Narrative(title, summary, List.copyOf(highlights), mvpComment);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Respuesta IA inválida", exception);
        }
    }

    private String text(JsonNode root, String field) { return root.path(field).isTextual() ? root.path(field).asText().trim() : ""; }

    private MatchAiSummaryResponse fallback(Long matchId, Facts facts) {
        String score = facts.homeScore() + "–" + facts.awayScore();
        String standout = facts.scorers().isEmpty() ? "No se registraron goleadores confirmados." : "Goleadores confirmados: " + facts.scorers().entrySet().stream().map(entry -> entry.getKey() + " (" + entry.getValue() + ")").collect(Collectors.joining(", ")) + ".";
        String mvp = facts.mvpAlias() == null ? "MVP pendiente de definición." : "MVP: " + facts.mvpAlias() + ".";
        return new MatchAiSummaryResponse(matchId, PROMPT_VERSION, "fallback", "Resultado final " + score,
                "Partido " + facts.mode() + " finalizado con marcador " + score + ".", List.of(standout, mvp), mvp);
    }

    private record Narrative(String title, String summary, List<String> highlights, String mvpComment) { }
    private record Facts(Integer homeScore, Integer awayScore, String mode, List<String> aliases, Map<String, Long> scorers, String mvpAlias) {
        Map<String, Object> asMap() { return Map.of("score", Map.of("home", homeScore, "away", awayScore), "mode", mode, "players", aliases, "scorers", scorers, "mvp", Optional.ofNullable(mvpAlias).orElse("pending")); }
    }
}
