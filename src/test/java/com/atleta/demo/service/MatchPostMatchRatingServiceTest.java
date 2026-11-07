package com.atleta.demo.service;

import com.atleta.demo.dto.request.PlayerPerformanceDto;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchEvent;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Position;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.EventType;
import com.atleta.demo.enums.MatchResultType;
import com.atleta.demo.enums.MatchType;
import com.atleta.demo.enums.MatchTeamSide;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;
import com.atleta.demo.repository.MatchEventRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchTeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchPostMatchRatingServiceTest {

    @Mock
    private MatchTeamRepository matchTeamRepository;
    @Mock
    private MatchPlayerRepository matchPlayerRepository;
    @Mock
    private MatchEventRepository matchEventRepository;
    @Mock
    private RatingService ratingService;

    private MatchPostMatchRatingService service;

    @BeforeEach
    void setUp() {
        service = new MatchPostMatchRatingService(
                matchTeamRepository,
                matchPlayerRepository,
                matchEventRepository,
                ratingService
        );
    }

    @Test
    void updatePlayerRatingsAfterMatch_BuildsPerformanceForConfirmedPlayers() {
        Match match = match(42L);
        Team homeTeam = team(1L);
        Team awayTeam = team(2L);
        MatchTeam home = matchTeam(match, homeTeam, true, 3);
        MatchTeam away = matchTeam(match, awayTeam, false, 1);
        PlayerProfile scorer = player();
        PlayerProfile assister = player();
        PlayerProfile defender = player();
        Position attackerPosition = position("Delantero");
        Position defenderPosition = position("Defensa");
        MatchPlayer scorerParticipation = matchPlayer(match, homeTeam, scorer, attackerPosition, true);
        MatchPlayer assisterParticipation = matchPlayer(match, homeTeam, assister, attackerPosition, true);
        MatchPlayer defenderParticipation = matchPlayer(match, awayTeam, defender, defenderPosition, true);
        MatchEvent goal = goal(match, homeTeam, scorer, assister);

        when(matchTeamRepository.findByMatch(match)).thenReturn(List.of(home, away));
        when(matchPlayerRepository.findByMatch(match)).thenReturn(List.of(
                scorerParticipation,
                assisterParticipation,
                defenderParticipation
        ));
        when(matchEventRepository.findByMatchOrderByCreatedAt(match)).thenReturn(List.of(goal));

        service.updatePlayerRatingsAfterMatch(match);

        ArgumentCaptor<List<PlayerPerformanceDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(ratingService).updatePlayerRatings(eq(42L), captor.capture());
        List<PlayerPerformanceDto> performances = captor.getValue();

        assertEquals(3, performances.size());
        PlayerPerformanceDto scorerPerformance = performances.stream()
                .filter(item -> scorer.getAtletaUuid().equals(item.getPlayerProfileId()))
                .findFirst()
                .orElseThrow();
        assertEquals(RoleType.ATAQUE, scorerPerformance.getRoleType());
        assertEquals(PriorityLevel.PRINCIPAL, scorerPerformance.getPriorityLevel());
        assertEquals(1, scorerPerformance.getGoalsScored());
        assertEquals(0, scorerPerformance.getAssistsMade());
        assertNull(scorerPerformance.getGoalsConceded());
        assertTrue(scorerPerformance.getWasMvp());
        assertEquals(MatchResultType.GANADO, scorerPerformance.getMatchResult());

        PlayerPerformanceDto defenderPerformance = performances.stream()
                .filter(item -> defender.getAtletaUuid().equals(item.getPlayerProfileId()))
                .findFirst()
                .orElseThrow();
        assertEquals(RoleType.DEFENSA, defenderPerformance.getRoleType());
        assertEquals(3, defenderPerformance.getGoalsConceded());
        assertEquals(MatchResultType.PERDIDO, defenderPerformance.getMatchResult());
        verify(ratingService, never()).updateConfirmedRotativeGoalkeeperRatings(any(), any());
    }

    @Test
    void rotativeModeUsesOnlyConfirmedPlayersAndTheirResults() {
        Match match = match(46L);
        match.setMatchType(MatchType.INTERNAL);
        match.setRotativeGoalkeeper(true);
        match.setFinalScoreLocal(2);
        match.setFinalScoreAway(1);
        Team club = team(1L);
        MatchPlayer local = matchPlayer(match, club, player(), position("Defensa"), true);
        local.setTeamSide(MatchTeamSide.LOCAL);
        MatchPlayer visitor = matchPlayer(match, club, player(), position("Delantero"), true);
        visitor.setTeamSide(MatchTeamSide.VISITA);
        MatchPlayer absent = matchPlayer(match, club, player(), position("Defensa"), false);
        when(matchTeamRepository.findByMatch(match)).thenReturn(List.of());
        when(matchPlayerRepository.findByMatch(match)).thenReturn(List.of(local, visitor, absent));
        when(matchEventRepository.findByMatchOrderByCreatedAt(match)).thenReturn(List.of());

        service.updatePlayerRatingsAfterMatch(match);

        ArgumentCaptor<List<PlayerPerformanceDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(ratingService).updateConfirmedRotativeGoalkeeperRatings(eq(match), captor.capture());
        assertEquals(2, captor.getValue().size());
        assertEquals(MatchResultType.GANADO, captor.getValue().get(0).getMatchResult());
        assertEquals(MatchResultType.PERDIDO, captor.getValue().get(1).getMatchResult());
    }

    @Test
    void rotativeInternalMatchRequiresConfirmedPlayersToHaveASide() {
        Match match = match(47L);
        match.setMatchType(MatchType.INTERNAL);
        match.setRotativeGoalkeeper(true);
        MatchPlayer player = matchPlayer(match, team(1L), player(), position("Defensa"), true);
        when(matchTeamRepository.findByMatch(match)).thenReturn(List.of());
        when(matchPlayerRepository.findByMatch(match)).thenReturn(List.of(player));
        when(matchEventRepository.findByMatchOrderByCreatedAt(match)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> service.updatePlayerRatingsAfterMatch(match));
        verify(ratingService, never()).updatePlayerRatings(any(), any());
    }

    @ParameterizedTest
    @CsvSource({"3,1,GANADO,PERDIDO", "1,3,PERDIDO,GANADO", "2,2,EMPATE,EMPATE"})
    void teamMatchUsesResultFromEachPlayersTeam(int localScore, int awayScore,
                                               MatchResultType localResult, MatchResultType awayResult) {
        Match match = match(45L);
        Team home = team(1L);
        Team away = team(2L);
        MatchPlayer local = matchPlayer(match, home, player(), position("Defensa"), true);
        MatchPlayer visitor = matchPlayer(match, away, player(), position("Defensa"), true);
        MatchPlayer unrelated = matchPlayer(match, team(3L), player(), position("Defensa"), true);
        when(matchTeamRepository.findByMatch(match)).thenReturn(List.of(
                matchTeam(match, home, true, localScore), matchTeam(match, away, false, awayScore)));
        when(matchPlayerRepository.findByMatch(match)).thenReturn(List.of(local, visitor, unrelated));
        when(matchEventRepository.findByMatchOrderByCreatedAt(match)).thenReturn(List.of());

        service.updatePlayerRatingsAfterMatch(match);

        ArgumentCaptor<List<PlayerPerformanceDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(ratingService).updatePlayerRatings(eq(45L), captor.capture());
        List<PlayerPerformanceDto> performances = captor.getValue();
        assertEquals(2, performances.size());
        assertEquals(localResult, performances.get(0).getMatchResult());
        assertEquals(awayResult, performances.get(1).getMatchResult());
        assertEquals(awayScore, performances.get(0).getGoalsConceded());
        assertEquals(localScore, performances.get(1).getGoalsConceded());
    }

    @Test
    void updatePlayerRatingsAfterMatch_SkipsWhenTeamsAreIncomplete() {
        Match match = match(42L);
        when(matchTeamRepository.findByMatch(match)).thenReturn(List.of(matchTeam(match, team(1L), true, 0)));

        service.updatePlayerRatingsAfterMatch(match);

        verify(ratingService, never()).updatePlayerRatings(any(), any());
    }

    @ParameterizedTest
    @CsvSource({"3,1,GANADO,PERDIDO", "1,3,PERDIDO,GANADO", "2,2,EMPATE,EMPATE"})
    void internalMatchUsesFinalScoreAndPlayerSide(int localScore, int awayScore,
                                                 MatchResultType localResult, MatchResultType awayResult) {
        Match match = match(43L);
        match.setMatchType(MatchType.INTERNAL);
        match.setFinalScoreLocal(localScore);
        match.setFinalScoreAway(awayScore);
        Team club = team(1L);
        MatchPlayer local = matchPlayer(match, club, player(), position("Defensa"), true);
        local.setTeamSide(MatchTeamSide.LOCAL);
        MatchPlayer away = matchPlayer(match, club, player(), position("Arquero"), true);
        away.setTeamSide(MatchTeamSide.VISITA);
        MatchPlayer absent = matchPlayer(match, club, player(), position("Delantero"), false);
        absent.setTeamSide(MatchTeamSide.LOCAL);
        when(matchTeamRepository.findByMatch(match)).thenReturn(List.of(matchTeam(match, club, true, 0)));
        when(matchPlayerRepository.findByMatch(match)).thenReturn(List.of(local, away, absent));
        when(matchEventRepository.findByMatchOrderByCreatedAt(match)).thenReturn(List.of());

        service.updatePlayerRatingsAfterMatch(match);

        ArgumentCaptor<List<PlayerPerformanceDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(ratingService).updatePlayerRatings(eq(43L), captor.capture());
        List<PlayerPerformanceDto> performances = captor.getValue();
        assertEquals(2, performances.size());
        assertEquals(localResult, performances.get(0).getMatchResult());
        assertEquals(awayScore, performances.get(0).getGoalsConceded());
        assertEquals(awayResult, performances.get(1).getMatchResult());
        assertEquals(localScore, performances.get(1).getGoalsConceded());
    }

    @ParameterizedTest
    @CsvSource({"Lateral Derecho,CARRILERO", "Lateral Izquierdo,CARRILERO", "DT,DT", "Director Técnico,DT"})
    void internalMatchUpdatesTheRoleOfThePlayedPosition(String positionName, RoleType expectedRole) {
        Match match = match(44L);
        match.setMatchType(MatchType.INTERNAL);
        match.setFinalScoreLocal(0);
        match.setFinalScoreAway(0);
        MatchPlayer participant = matchPlayer(match, team(1L), player(), position(positionName), true);
        when(matchTeamRepository.findByMatch(match)).thenReturn(List.of());
        when(matchPlayerRepository.findByMatch(match)).thenReturn(List.of(participant));
        when(matchEventRepository.findByMatchOrderByCreatedAt(match)).thenReturn(List.of());
        service.updatePlayerRatingsAfterMatch(match);
        ArgumentCaptor<List<PlayerPerformanceDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(ratingService).updatePlayerRatings(eq(44L), captor.capture());
        assertEquals(expectedRole, captor.getValue().get(0).getRoleType());
    }

    private Match match(Long id) {
        Match match = new Match();
        match.setId(id);
        return match;
    }

    private Team team(Long id) {
        Team team = new Team();
        team.setId(id);
        return team;
    }

    private MatchTeam matchTeam(Match match, Team team, boolean local, int goals) {
        MatchTeam matchTeam = new MatchTeam(match, team, local);
        matchTeam.setGoles(goals);
        return matchTeam;
    }

    private MatchPlayer matchPlayer(Match match, Team team, PlayerProfile player, Position position, boolean confirmed) {
        MatchPlayer matchPlayer = new MatchPlayer(match, team, player, position, PlayerRole.JUGADOR);
        matchPlayer.setConfirmado(confirmed);
        return matchPlayer;
    }

    private MatchEvent goal(Match match, Team team, PlayerProfile scorer, PlayerProfile assister) {
        MatchEvent event = new MatchEvent(match, EventType.GOL, scorer, team, scorer);
        event.setAssistPlayer(assister);
        event.setConfirmedByHome(true);
        event.setConfirmedByAway(true);
        return event;
    }

    private PlayerProfile player() {
        PlayerProfile player = new PlayerProfile();
        player.setAtletaUuid(UUID.randomUUID());
        return player;
    }

    private Position position(String name) {
        Position position = new Position();
        position.setNombre(name);
        return position;
    }
}
