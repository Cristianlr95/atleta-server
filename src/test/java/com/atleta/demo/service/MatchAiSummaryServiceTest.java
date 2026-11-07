package com.atleta.demo.service;

import com.atleta.demo.ai.AiProvider;
import com.atleta.demo.dto.response.MatchAiSummaryResponse;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchEvent;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Position;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.EventType;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.repository.MatchEventRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchAiSummaryServiceTest {
    @Mock private MatchRepository matchRepository;
    @Mock private MatchPlayerRepository matchPlayerRepository;
    @Mock private MatchEventRepository matchEventRepository;
    @Mock private AiProvider aiProvider;
    private MatchAiSummaryService service;

    @BeforeEach
    void setUp() {
        service = new MatchAiSummaryService(matchRepository, matchPlayerRepository, matchEventRepository, aiProvider, new ObjectMapper());
    }

    @Test
    void generatesValidatedNarrativeFromConfirmedFacts() {
        Fixture fixture = fixture();
        when(matchRepository.findById(7L)).thenReturn(Optional.of(fixture.match()));
        when(matchPlayerRepository.findByMatchAndPlayerAtletaUuid(fixture.match(), fixture.actorUuid())).thenReturn(Optional.of(fixture.matchPlayer()));
        when(matchPlayerRepository.findByMatch(fixture.match())).thenReturn(List.of(fixture.matchPlayer()));
        when(matchEventRepository.findGoalsByMatch(fixture.match())).thenReturn(List.of(fixture.goal()));
        when(aiProvider.name()).thenReturn("fake");
        when(aiProvider.generateJson(org.mockito.ArgumentMatchers.any())).thenReturn("""
                {"title":"Ana decide el partido","summary":"Ana marcó en el triunfo 2-1.","highlights":["Ana convirtió un gol confirmado."],"mvpComment":"Ana fue clave."}
                """);

        MatchAiSummaryResponse response = service.generate(7L, fixture.actorUuid());

        assertEquals("fake", response.source());
        assertEquals("Ana decide el partido", response.title());
        assertEquals(1, response.highlights().size());
    }

    @Test
    void fallsBackWhenProviderIsUnavailable() {
        Fixture fixture = fixture();
        when(matchRepository.findById(7L)).thenReturn(Optional.of(fixture.match()));
        when(matchPlayerRepository.findByMatchAndPlayerAtletaUuid(fixture.match(), fixture.actorUuid())).thenReturn(Optional.of(fixture.matchPlayer()));
        when(matchPlayerRepository.findByMatch(fixture.match())).thenReturn(List.of(fixture.matchPlayer()));
        when(matchEventRepository.findGoalsByMatch(fixture.match())).thenReturn(List.of(fixture.goal()));
        when(aiProvider.generateJson(org.mockito.ArgumentMatchers.any())).thenThrow(new RuntimeException("offline"));

        MatchAiSummaryResponse response = service.generate(7L, fixture.actorUuid());

        assertEquals("fallback", response.source());
        assertEquals("Resultado final 2–1", response.title());
    }

    @Test
    void rejectsMatchesThatHaveNotFinished() {
        Fixture fixture = fixture();
        fixture.match().setEstado(MatchStatus.INICIADO);
        when(matchRepository.findById(7L)).thenReturn(Optional.of(fixture.match()));

        assertThrows(IllegalArgumentException.class, () -> service.generate(7L, fixture.actorUuid()));
    }

    private Fixture fixture() {
        UUID actorUuid = UUID.randomUUID();
        PlayerProfile player = new PlayerProfile();
        player.setAlias("Ana");
        player.setAtletaUuid(actorUuid);
        Match match = new Match(MatchMode.CINCO_VS_CINCO, LocalDateTime.now().minusHours(2), player);
        match.setId(7L);
        match.setEstado(MatchStatus.FINALIZADO);
        match.setFinalScoreLocal(2);
        match.setFinalScoreAway(1);
        Team team = new Team();
        Position position = new Position("Delantera");
        MatchPlayer matchPlayer = new MatchPlayer(match, team, player, position, PlayerRole.JUGADOR);
        MatchEvent goal = new MatchEvent(match, EventType.GOL, player, team, player);
        goal.setConfirmedByHome(true);
        goal.setConfirmedByAway(true);
        return new Fixture(actorUuid, match, player, matchPlayer, goal);
    }

    private record Fixture(UUID actorUuid, Match match, PlayerProfile player, MatchPlayer matchPlayer, MatchEvent goal) { }
}
