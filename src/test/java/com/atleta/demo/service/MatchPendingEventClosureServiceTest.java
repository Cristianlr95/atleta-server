package com.atleta.demo.service;

import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchEvent;
import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.EventType;
import com.atleta.demo.repository.MatchEventRepository;
import com.atleta.demo.repository.MatchTeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchPendingEventClosureServiceTest {

    @Mock
    private MatchEventRepository matchEventRepository;

    @Mock
    private MatchTeamRepository matchTeamRepository;

    private MatchPendingEventClosureService service;

    @BeforeEach
    void setUp() {
        service = new MatchPendingEventClosureService(matchEventRepository, matchTeamRepository);
    }

    @Test
    void closePendingEventsForFinalization_ConfirmsEventsAndAppliesNewGoal() {
        Match match = new Match();
        match.setId(42L);
        Team team = team();
        PlayerProfile player = player();
        MatchEvent pendingGoal = new MatchEvent(match, EventType.GOL, player, team, player);
        MatchTeam matchTeam = new MatchTeam(match, team, true);

        when(matchEventRepository.findPendingEventsByMatch(match)).thenReturn(List.of(pendingGoal));
        when(matchTeamRepository.findByMatchAndTeam(match, team)).thenReturn(Optional.of(matchTeam));

        service.closePendingEventsForFinalization(match);

        assertTrue(pendingGoal.isFullyConfirmed());
        assertEquals(1, matchTeam.getGoles());
        verify(matchTeamRepository).save(matchTeam);
        verify(matchEventRepository).saveAll(List.of(pendingGoal));
    }

    private PlayerProfile player() {
        PlayerProfile player = new PlayerProfile();
        player.setAtletaUuid(UUID.randomUUID());
        return player;
    }

    private Team team() {
        Team team = new Team();
        team.setId(1L);
        return team;
    }
}
