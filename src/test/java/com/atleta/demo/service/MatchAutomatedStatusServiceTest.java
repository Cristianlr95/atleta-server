package com.atleta.demo.service;

import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Position;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.enums.MatchValidationStatus;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchAutomatedStatusServiceTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchTeamRepository matchTeamRepository;
    @Mock
    private MatchPlayerRepository matchPlayerRepository;
    @Mock
    private MatchPlayerHistoryService matchPlayerHistoryService;

    private MatchAutomatedStatusService service;

    @BeforeEach
    void setUp() {
        service = new MatchAutomatedStatusService(
                matchRepository,
                matchTeamRepository,
                matchPlayerRepository,
                matchPlayerHistoryService,
                new MatchRosterPolicy()
        );
    }

    @Test
    void refreshAutomatedMatchStates_StartsReadyMatchWithMinimumConfirmedPlayers() {
        Match match = match(MatchStatus.CREADO);
        match.setFechaHoraProgramada(LocalDateTime.now().minusMinutes(10));
        List<MatchPlayer> players = confirmedPlayers(match, 10);

        when(matchRepository.findCreatedMatchesReadyToStart(any(LocalDateTime.class))).thenReturn(List.of(match));
        when(matchRepository.findExpiredCreatedMatches(any(LocalDateTime.class))).thenReturn(List.of());
        when(matchRepository.findExpiredStartedMatches(any(LocalDateTime.class))).thenReturn(List.of());
        when(matchRepository.findByEstado(MatchStatus.FINALIZADO)).thenReturn(List.of());
        when(matchTeamRepository.findByMatch(match)).thenReturn(List.of(new MatchTeam(match, new Team(), true)));
        when(matchPlayerRepository.findByMatch(match)).thenReturn(players);

        service.refreshAutomatedMatchStates();

        assertEquals(MatchStatus.INICIADO, match.getEstado());
        assertEquals(MatchValidationStatus.PENDING, match.getValidationStatus());
        assertNotNull(match.getStartedAt());
        verify(matchRepository).saveAll(List.of(match));
    }

    @Test
    void refreshAutomatedMatchStates_InvalidatesFinalizedMatchWithoutMinimumConfirmedPlayersOrHistory() {
        Match match = match(MatchStatus.FINALIZADO);
        List<MatchPlayer> players = confirmedPlayers(match, 2);

        when(matchRepository.findCreatedMatchesReadyToStart(any(LocalDateTime.class))).thenReturn(List.of());
        when(matchRepository.findExpiredCreatedMatches(any(LocalDateTime.class))).thenReturn(List.of());
        when(matchRepository.findExpiredStartedMatches(any(LocalDateTime.class))).thenReturn(List.of());
        when(matchRepository.findByEstado(MatchStatus.FINALIZADO)).thenReturn(List.of(match));
        when(matchPlayerRepository.findByMatch(match)).thenReturn(players);
        when(matchPlayerHistoryService.hasHistoryRows(match)).thenReturn(false);

        service.refreshAutomatedMatchStates();

        assertEquals(MatchStatus.INVALIDO, match.getEstado());
        assertEquals(MatchValidationStatus.INVALID_CONFIRMATION_THRESHOLD, match.getValidationStatus());
        verify(matchRepository).saveAll(List.of(match));
    }

    @Test
    void refreshAutomatedMatchStates_KeepsFinalizedMatchWithHistoryRows() {
        Match match = match(MatchStatus.FINALIZADO);
        List<MatchPlayer> players = confirmedPlayers(match, 2);

        when(matchRepository.findCreatedMatchesReadyToStart(any(LocalDateTime.class))).thenReturn(List.of());
        when(matchRepository.findExpiredCreatedMatches(any(LocalDateTime.class))).thenReturn(List.of());
        when(matchRepository.findExpiredStartedMatches(any(LocalDateTime.class))).thenReturn(List.of());
        when(matchRepository.findByEstado(MatchStatus.FINALIZADO)).thenReturn(List.of(match));
        when(matchPlayerRepository.findByMatch(match)).thenReturn(players);
        when(matchPlayerHistoryService.hasHistoryRows(match)).thenReturn(true);

        service.refreshAutomatedMatchStates();

        assertEquals(MatchStatus.FINALIZADO, match.getEstado());
        verify(matchRepository, never()).saveAll(any());
    }

    private Match match(MatchStatus status) {
        PlayerProfile creator = new PlayerProfile();
        creator.setAtletaUuid(UUID.randomUUID());

        Match match = new Match();
        match.setId(42L);
        match.setModalidad(MatchMode.CINCO_VS_CINCO);
        match.setEstado(status);
        match.setCreador(creator);
        return match;
    }

    private List<MatchPlayer> confirmedPlayers(Match match, int count) {
        Team team = new Team();
        team.setId(1L);
        Position position = new Position();
        position.setId(1L);
        position.setNombre("Delantero");

        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> {
                    PlayerProfile player = new PlayerProfile();
                    player.setAtletaUuid(UUID.randomUUID());
                    MatchPlayer matchPlayer = new MatchPlayer(match, team, player, position, PlayerRole.JUGADOR);
                    matchPlayer.setConfirmado(true);
                    return matchPlayer;
                })
                .toList();
    }
}
