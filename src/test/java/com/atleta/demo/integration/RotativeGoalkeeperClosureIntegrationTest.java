package com.atleta.demo.integration;

import com.atleta.demo.entity.*;
import com.atleta.demo.enums.*;
import com.atleta.demo.service.MatchService;
import com.atleta.demo.service.RatingService;
import com.atleta.demo.repository.RatingHistoryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RotativeGoalkeeperClosureIntegrationTest {
    @Autowired EntityManager entities;
    @Autowired JdbcTemplate jdbc;
    @Autowired MatchService matches;
    @Autowired RatingHistoryRepository ratingHistory;
    @Autowired RatingService ratings;

    @Test
    void closingUpdatesConfirmedPlayersOnceAndRejectsRepeatedClosure() {
        List<PlayerProfile> players = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            Athlete athlete = new Athlete(UUID.randomUUID() + "@rotative.test", "test-hash", "Jugador " + i);
            entities.persist(athlete);
            PlayerProfile profile = new PlayerProfile(athlete, "Jugador " + i);
            entities.persist(profile);
            players.add(profile);
        }
        Team team = new Team("Rotativo " + UUID.randomUUID(), players.get(0));
        entities.persist(team);
        Position position = new Position("Defensa " + UUID.randomUUID());
        entities.persist(position);
        Position goalkeeperPosition = new Position("Arquero " + UUID.randomUUID());
        entities.persist(goalkeeperPosition);
        Match match = new Match(MatchMode.CINCO_VS_CINCO, LocalDateTime.now().minusHours(2), players.get(0));
        match.setMatchType(MatchType.INTERNAL);
        match.setRotativeGoalkeeper(true);
        match.setEstado(MatchStatus.INICIADO);
        match.setStartedAt(LocalDateTime.now().minusHours(2));
        entities.persist(match);
        entities.persist(new MatchTeam(match, team, true));
        for (int i = 0; i < players.size(); i++) {
            MatchPlayer participant = new MatchPlayer(match, team, players.get(i), i == 0 ? goalkeeperPosition : position,
                    i == 0 ? PlayerRole.CAPITAN : PlayerRole.JUGADOR);
            participant.setConfirmado(i < 10);
            participant.setTeamSide(i % 2 == 0 ? MatchTeamSide.LOCAL : MatchTeamSide.VISITA);
            entities.persist(participant);
        }
        MatchEvent goal = new MatchEvent(match, EventType.GOL, players.get(0), team, players.get(0));
        goal.setConfirmedByHome(true);
        goal.setConfirmedByAway(true);
        entities.persist(goal);
        entities.flush();

        matches.changeMatchStatus(match.getId(), MatchStatus.FINALIZADO, players.get(0).getAtletaUuid());
        entities.flush();
        assertEquals(10, jdbc.queryForObject(
                "select count(*) from rating_history where match_id=? and rotative_goalkeeper_mode=true",
                Integer.class, match.getId()));
        assertEquals(5, jdbc.queryForObject(
                "select count(*) from rating_history where match_id=? and rotative_goalkeeper_mode=true and match_result='GANADO'",
                Integer.class, match.getId()));
        assertEquals(5, jdbc.queryForObject(
                "select count(*) from rating_history where match_id=? and rotative_goalkeeper_mode=true and match_result='PERDIDO'",
                Integer.class, match.getId()));
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from player_ratings where player_profile_id=?", Integer.class,
                players.get(10).getAtletaUuid()));
        Object[] overall = row(ratingHistory.getPlayerPerformanceStatistics(players.get(0).getAtletaUuid()));
        Object[] goalkeeper = row(ratingHistory.getPlayerPerformanceStatisticsByRole(players.get(0).getAtletaUuid(), RoleType.ARQUERO));
        Object[] results = row(ratingHistory.getResultBreakdownByPlayerProfileId(players.get(0).getAtletaUuid()));
        assertEquals(1L, ((Number) overall[0]).longValue());
        assertEquals(1L, ((Number) goalkeeper[0]).longValue());
        assertEquals(1L, ((Number) results[0]).longValue());
        assertEquals(1L, ((Number) ratings.getPlayerPerformanceStatistics(players.get(0).getAtletaUuid())[0]).longValue());
        assertEquals(1L, ((Number) ratings.getPlayerPerformanceStatisticsByRole(players.get(0).getAtletaUuid(), RoleType.ARQUERO)[0]).longValue());
        assertThrows(IllegalArgumentException.class,
                () -> matches.changeMatchStatus(match.getId(), MatchStatus.FINALIZADO, players.get(0).getAtletaUuid()));
        assertEquals(10, jdbc.queryForObject(
                "select count(*) from rating_history where match_id=? and rotative_goalkeeper_mode=true",
                Integer.class, match.getId()));
    }

    private Object[] row(Object[] result) {
        return result.length == 1 && result[0] instanceof Object[] nested ? nested : result;
    }
}
