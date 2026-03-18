package com.atleta.demo.service;

import com.atleta.demo.dto.response.MatchMvpResponse;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchMvpVote;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.PlayerHistory;
import com.atleta.demo.entity.PlayerPosition;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Position;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.repository.MatchMvpVoteRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchRepository;
import com.atleta.demo.repository.PlayerHistoryRepository;
import com.atleta.demo.repository.PlayerPositionRepository;
import com.atleta.demo.repository.PlayerProfileRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchMvpServiceTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchPlayerRepository matchPlayerRepository;
    @Mock
    private MatchMvpVoteRepository matchMvpVoteRepository;
    @Mock
    private PlayerProfileRepository playerProfileRepository;
    @Mock
    private PlayerHistoryRepository playerHistoryRepository;
    @Mock
    private PlayerPositionRepository playerPositionRepository;

    private MatchMvpService serviceWithoutBonus;
    private MatchMvpService serviceWithBonus;

    private Match match;
    private PlayerProfile voter;
    private PlayerProfile candidateA;
    private PlayerProfile candidateB;
    private Team team;
    private Position position;

    @BeforeEach
    void setUp() {
        serviceWithoutBonus = new MatchMvpService(
                matchRepository,
                matchPlayerRepository,
                matchMvpVoteRepository,
                playerProfileRepository,
                playerHistoryRepository,
                playerPositionRepository,
                false
        );
        serviceWithBonus = new MatchMvpService(
                matchRepository,
                matchPlayerRepository,
                matchMvpVoteRepository,
                playerProfileRepository,
                playerHistoryRepository,
                playerPositionRepository,
                true
        );

        voter = new PlayerProfile();
        voter.setAtletaUuid(UUID.randomUUID());
        voter.setAlias("voter");

        candidateA = new PlayerProfile();
        candidateA.setAtletaUuid(UUID.randomUUID());
        candidateA.setAlias("A");

        candidateB = new PlayerProfile();
        candidateB.setAtletaUuid(UUID.randomUUID());
        candidateB.setAlias("B");

        team = new Team();
        team.setId(1L);
        team.setNombre("Team");

        position = new Position();
        position.setId(7L);
        position.setNombre("Mediocampo");

        match = new Match();
        match.setId(100L);
        match.setFinalizedAt(LocalDateTime.now().minusMinutes(30));
    }

    @Test
    void vote_shouldUpsertExistingVote() {
        MatchMvpVote existingVote = new MatchMvpVote(match, voter, candidateA);

        when(matchRepository.findById(100L)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));
        when(playerProfileRepository.findById(voter.getAtletaUuid())).thenReturn(Optional.of(voter));
        when(playerProfileRepository.findById(candidateB.getAtletaUuid())).thenReturn(Optional.of(candidateB));
        when(matchMvpVoteRepository.findByMatchAndVoter(match, voter)).thenReturn(Optional.of(existingVote));
        when(matchMvpVoteRepository.findByMatch(match)).thenReturn(List.of(existingVote));
        when(matchPlayerRepository.findConfirmedPlayersByMatch(match)).thenReturn(List.of(
                confirmed(candidateA),
                confirmed(candidateB),
                confirmed(voter)
        ));

        MatchMvpResponse response = serviceWithoutBonus.vote(100L, voter.getAtletaUuid(), candidateB.getAtletaUuid());

        assertNotNull(response);
        assertEquals(candidateB.getAtletaUuid(), existingVote.getVotedUser().getAtletaUuid());
        verify(matchMvpVoteRepository).save(existingVote);
    }

    @Test
    void vote_shouldRejectWhenVotingWindowExpired() {
        match.setFinalizedAt(LocalDateTime.now().minusHours(4));

        when(matchRepository.findById(100L)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));
        when(matchMvpVoteRepository.findByMatch(match)).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> serviceWithoutBonus.vote(100L, voter.getAtletaUuid(), candidateA.getAtletaUuid())
        );

        assertEquals("La votacion MVP ya cerro (ventana de 3 horas vencida)", exception.getMessage());
    }

    @Test
    void getMvpState_shouldLazyCloseAndResolveTieByXp() {
        match.setFinalizedAt(LocalDateTime.now().minusHours(4));

        MatchMvpVote vote1 = new MatchMvpVote(match, voter, candidateA);
        MatchMvpVote vote2 = new MatchMvpVote(match, candidateB, candidateB);

        PlayerHistory historyA = new PlayerHistory(match, candidateA, team, position, 1, 0, null, 30);
        PlayerHistory historyB = new PlayerHistory(match, candidateB, team, position, 2, 0, null, 20);

        when(matchRepository.findById(100L)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));
        when(matchMvpVoteRepository.findByMatch(match)).thenReturn(List.of(vote1, vote2));
        when(matchPlayerRepository.findConfirmedPlayersByMatch(match)).thenReturn(List.of(
                confirmed(candidateA),
                confirmed(candidateB),
                confirmed(voter)
        ));
        when(playerProfileRepository.findById(candidateA.getAtletaUuid())).thenReturn(Optional.of(candidateA));
        when(playerProfileRepository.findById(candidateB.getAtletaUuid())).thenReturn(Optional.of(candidateB));
        when(playerHistoryRepository.findByMatchAndPlayer(eq(match), eq(candidateA))).thenReturn(Optional.of(historyA));
        when(playerHistoryRepository.findByMatchAndPlayer(eq(match), eq(candidateB))).thenReturn(Optional.of(historyB));

        MatchMvpResponse response = serviceWithoutBonus.getMvpState(100L, voter.getAtletaUuid());

        assertNotNull(response);
        assertFalse(response.isOpen());
        assertEquals(candidateA.getAtletaUuid(), response.getWinnerUserId());
        verify(matchRepository, atLeastOnce()).save(match);
    }

    @Test
    void getMvpState_shouldApplyBonusXpWhenFeatureEnabled() {
        match.setFinalizedAt(LocalDateTime.now().minusHours(4));

        MatchMvpVote vote1 = new MatchMvpVote(match, voter, candidateA);
        PlayerHistory winnerHistory = new PlayerHistory(match, candidateA, team, position, 0, 0, null, 25);
        PlayerPosition winnerPosition = new PlayerPosition();
        winnerPosition.setPlayer(candidateA);
        winnerPosition.setPosition(position);
        winnerPosition.setXp(100);

        when(matchRepository.findById(100L)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));
        when(matchMvpVoteRepository.findByMatch(match)).thenReturn(List.of(vote1));
        when(matchPlayerRepository.findConfirmedPlayersByMatch(match)).thenReturn(List.of(
                confirmed(candidateA),
                confirmed(voter)
        ));
        when(playerProfileRepository.findById(candidateA.getAtletaUuid())).thenReturn(Optional.of(candidateA));
        when(playerHistoryRepository.findByMatchAndPlayer(match, candidateA)).thenReturn(Optional.of(winnerHistory));
        when(playerHistoryRepository.applyMvpBonusXpIfNotApplied(100L, candidateA.getAtletaUuid(), 10)).thenReturn(1);
        when(playerPositionRepository.findByPlayerAndPosition(candidateA, position)).thenReturn(Optional.of(winnerPosition));
        when(playerPositionRepository.save(any(PlayerPosition.class))).thenAnswer(inv -> inv.getArgument(0));

        MatchMvpResponse response = serviceWithBonus.getMvpState(100L, voter.getAtletaUuid());

        assertNotNull(response);
        verify(playerHistoryRepository).applyMvpBonusXpIfNotApplied(100L, candidateA.getAtletaUuid(), 10);
        verify(playerPositionRepository).save(any(PlayerPosition.class));
    }

    @Test
    void getMvpState_shouldNotApplyBonusWhenFeatureDisabled() {
        match.setFinalizedAt(LocalDateTime.now().minusHours(4));
        MatchMvpVote vote = new MatchMvpVote(match, voter, candidateA);

        when(matchRepository.findById(100L)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));
        when(matchMvpVoteRepository.findByMatch(match)).thenReturn(List.of(vote));
        when(matchPlayerRepository.findConfirmedPlayersByMatch(match)).thenReturn(List.of(confirmed(candidateA)));
        serviceWithoutBonus.getMvpState(100L, voter.getAtletaUuid());

        verify(playerHistoryRepository, never()).applyMvpBonusXpIfNotApplied(any(), any(), any());
    }

    private MatchPlayer confirmed(PlayerProfile profile) {
        MatchPlayer mp = new MatchPlayer();
        mp.setMatch(match);
        mp.setPlayer(profile);
        mp.setConfirmado(true);
        mp.setRol(PlayerRole.JUGADOR);
        mp.setTeam(team);
        mp.setPosition(position);
        return mp;
    }
}
