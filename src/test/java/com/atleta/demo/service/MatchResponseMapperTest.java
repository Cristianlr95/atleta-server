package com.atleta.demo.service;

import com.atleta.demo.dto.response.MatchResponse;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchEvent;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Position;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.EventType;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.enums.MatchTeamSide;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.repository.MatchEventRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchTeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchResponseMapperTest {

    @Mock
    private MatchTeamRepository matchTeamRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private MatchEventRepository matchEventRepository;

    @Mock
    private MatchStatusPolicy matchStatusPolicy;

    private MatchResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new MatchResponseMapper(
                matchTeamRepository,
                matchPlayerRepository,
                matchEventRepository,
                matchStatusPolicy
        );
    }

    @Test
    void toMatchResponse_MapsNestedDataAndCreatorFallback() {
        PlayerProfile creator = player("Creador");
        PlayerProfile striker = player("Nueve");
        PlayerProfile assistant = player("Asistente");

        Match match = new Match(MatchMode.CINCO_VS_CINCO, LocalDateTime.now().plusDays(1), creator);
        match.setId(10L);
        match.setEstado(MatchStatus.INICIADO);
        match.setStartedAt(LocalDateTime.now().minusHours(2));

        Team local = team(1L, "Local", creator);
        Team away = team(2L, "Visita", assistant);

        MatchTeam localMatchTeam = new MatchTeam(match, local, true);
        localMatchTeam.setId(100L);
        localMatchTeam.setGoles(2);

        Position position = new Position("Delantero");
        position.setId(7L);

        MatchPlayer matchPlayer = new MatchPlayer(match, local, striker, position, PlayerRole.JUGADOR);
        matchPlayer.setId(200L);
        matchPlayer.setConfirmado(true);
        matchPlayer.setTeamSide(MatchTeamSide.LOCAL);

        MatchEvent goal = new MatchEvent(match, EventType.GOL, striker, local, assistant, creator);
        goal.setId(300L);
        goal.setConfirmedByHome(true);
        goal.setConfirmedByAway(false);

        when(matchTeamRepository.findByMatch(match)).thenReturn(List.of(localMatchTeam));
        when(matchPlayerRepository.findByMatch(match)).thenReturn(List.of(matchPlayer));
        when(matchEventRepository.findByMatchOrderByCreatedAt(match)).thenReturn(List.of(goal));
        when(matchStatusPolicy.isClosePending(eq(match), eq(1L), any(LocalDateTime.class))).thenReturn(true);

        MatchResponse response = mapper.toMatchResponse(match);

        assertEquals(10L, response.getId());
        assertEquals(MatchStatus.INICIADO, response.getEstado());
        assertTrue(response.getClosePending());
        assertEquals("Creador", response.getCreador().getAlias());
        assertEquals(1, response.getMatchTeams().size());
        assertEquals("Local", response.getMatchTeams().get(0).getTeam().getNombre());
        assertEquals(2, response.getPlayers().size());
        assertEquals("Nueve", response.getPlayers().get(0).getPlayer().getAlias());
        assertEquals(MatchTeamSide.LOCAL, response.getPlayers().get(0).getTeamSide());
        assertEquals("Creador", response.getPlayers().get(1).getPlayer().getAlias());
        assertEquals(PlayerRole.CAPITAN, response.getPlayers().get(1).getRol());
        assertEquals(EventType.GOL, response.getEvents().get(0).getEventType());
        assertEquals("Asistente", response.getEvents().get(0).getAssistPlayer().getAlias());
    }

    private PlayerProfile player(String alias) {
        PlayerProfile player = new PlayerProfile();
        player.setAtletaUuid(UUID.randomUUID());
        player.setAlias(alias);
        player.setTrustScore(100);
        return player;
    }

    private Team team(Long id, String name, PlayerProfile creator) {
        Team team = new Team();
        team.setId(id);
        team.setNombre(name);
        team.setCreador(creator);
        return team;
    }
}
