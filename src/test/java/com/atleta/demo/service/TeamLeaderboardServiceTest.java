package com.atleta.demo.service;

import com.atleta.demo.dto.response.TeamLeaderboardEntryResponse;
import com.atleta.demo.dto.response.TeamExternalRecordResponse;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.PlayerRating;
import com.atleta.demo.entity.Team;
import com.atleta.demo.entity.TeamMember;
import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.enums.MatchType;
import com.atleta.demo.repository.MatchTeamRepository;
import com.atleta.demo.repository.PlayerRatingRepository;
import com.atleta.demo.repository.TeamMemberRepository;
import com.atleta.demo.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamLeaderboardServiceTest {
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private PlayerRatingRepository playerRatingRepository;
    @Mock private MatchTeamRepository matchTeamRepository;

    private TeamLeaderboardService service;
    private Team team;
    private PlayerProfile alpha;
    private PlayerProfile beta;
    private PlayerProfile unrated;

    @BeforeEach
    void setUp() {
        service = new TeamLeaderboardService(teamRepository, teamMemberRepository, playerRatingRepository, matchTeamRepository);
        alpha = player("11111111-1111-1111-1111-111111111111", "Alpha");
        beta = player("22222222-2222-2222-2222-222222222222", "Beta");
        unrated = player("33333333-3333-3333-3333-333333333333", "Sin rating");
        team = new Team("Atleta FC", alpha);
        team.setId(77L);
        when(teamRepository.findById(77L)).thenReturn(Optional.of(team));
    }

    @Test
    void ranksTiesStablyAndKeepsUnratedMembersWithoutInventingOvr() {
        List<TeamMember> members = List.of(member(alpha), member(beta), member(unrated));
        when(teamMemberRepository.findActiveByTeam(team)).thenReturn(members);
        when(playerRatingRepository.findByPlayerProfileIds(List.of(
                alpha.getAtletaUuid(), beta.getAtletaUuid(), unrated.getAtletaUuid())))
                .thenReturn(List.of(rating(alpha, 80, 4), rating(beta, 80, 3)));

        List<TeamLeaderboardEntryResponse> result = service.getLeaderboard(77L, alpha.getAtletaUuid());

        assertEquals(List.of("Alpha", "Beta", "Sin rating"), result.stream().map(TeamLeaderboardEntryResponse::alias).toList());
        assertEquals(1, result.get(0).rank());
        assertEquals(1, result.get(1).rank());
        assertEquals(BigDecimal.valueOf(80).setScale(2), result.get(0).score());
        assertNull(result.get(2).score());
        assertEquals(false, result.get(2).rated());
        verify(playerRatingRepository).findByPlayerProfileIds(List.of(
                alpha.getAtletaUuid(), beta.getAtletaUuid(), unrated.getAtletaUuid()));
    }

    @Test
    void rejectsUsersOutsideTheTeamBeforeReadingRatings() {
        when(teamMemberRepository.findActiveByTeam(team)).thenReturn(List.of(member(alpha)));

        assertThrows(AccessDeniedException.class,
                () -> service.getLeaderboard(77L, UUID.fromString("99999999-9999-9999-9999-999999999999")));
    }

    @Test
    void returnsEmptyLeaderboardForAuthorizedCreatorOfEmptyTeam() {
        when(teamMemberRepository.findActiveByTeam(team)).thenReturn(List.of());

        assertEquals(List.of(), service.getLeaderboard(77L, alpha.getAtletaUuid()));
    }

    @Test
    void externalRecordCountsOnlyFinalizedMatchesAgainstAnotherTeam() {
        Team rival = new Team("Rival FC", beta);
        rival.setId(88L);
        Match external = match(MatchType.FRIENDLY);
        Match internal = match(MatchType.INTERNAL);
        Match unfinished = match(MatchType.POINTS);
        unfinished.setEstado(MatchStatus.CREADO);
        MatchTeam externalSide = side(external, team, 3);
        MatchTeam externalRival = side(external, rival, 1);
        MatchTeam internalSide = side(internal, team, 4);
        MatchTeam internalRival = side(internal, rival, 0);
        MatchTeam unfinishedSide = side(unfinished, team, 2);
        MatchTeam unfinishedRival = side(unfinished, rival, 1);
        when(teamMemberRepository.findActiveByTeam(team)).thenReturn(List.of(member(alpha)));
        when(matchTeamRepository.findByTeam(team)).thenReturn(List.of(externalSide, internalSide, unfinishedSide));
        when(matchTeamRepository.findByMatch(external)).thenReturn(List.of(externalSide, externalRival));

        TeamExternalRecordResponse record = service.getExternalRecord(77L, alpha.getAtletaUuid());

        assertEquals(1, record.matchesPlayed());
        assertEquals(1, record.wins());
        assertEquals(0, record.draws());
        assertEquals(0, record.losses());
        assertEquals(3, record.points());
        verify(matchTeamRepository).findByMatch(external);
        verify(matchTeamRepository, org.mockito.Mockito.never()).findByMatch(internal);
        verify(matchTeamRepository, org.mockito.Mockito.never()).findByMatch(unfinished);
    }

    private PlayerProfile player(String uuid, String alias) {
        PlayerProfile player = new PlayerProfile();
        player.setAtletaUuid(UUID.fromString(uuid));
        player.setAlias(alias);
        return player;
    }

    private TeamMember member(PlayerProfile player) {
        TeamMember member = new TeamMember(team, player);
        member.setActivo(true);
        return member;
    }

    private PlayerRating rating(PlayerProfile player, int score, int matches) {
        PlayerRating rating = new PlayerRating(
                player, RoleType.ATAQUE, PriorityLevel.PRINCIPAL, BigDecimal.valueOf(score));
        rating.setMatchesPlayed(matches);
        return rating;
    }

    private Match match(MatchType type) {
        Match match = new Match();
        match.setEstado(MatchStatus.FINALIZADO);
        match.setMatchType(type);
        return match;
    }

    private MatchTeam side(Match match, Team side, int goals) {
        MatchTeam matchTeam = new MatchTeam();
        matchTeam.setMatch(match);
        matchTeam.setTeam(side);
        matchTeam.setGoles(goals);
        return matchTeam;
    }
}
