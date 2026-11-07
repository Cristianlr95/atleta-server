package com.atleta.demo.integration;

import com.atleta.demo.entity.*;
import com.atleta.demo.enums.*;
import com.atleta.demo.service.MatchService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RotativeGoalkeeperConcurrencyIntegrationTest {
    @Autowired EntityManager entities;
    @Autowired JdbcTemplate jdbc;
    @Autowired MatchService matches;
    @Autowired PlatformTransactionManager transactions;
    private Fixture fixture;

    @Test
    void simultaneousClosuresCannotDuplicateGoalkeeperRatings() throws Exception {
        fixture = new TransactionTemplate(transactions).execute(ignored -> createFixture());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var attempts = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int i = 0; i < 2; i++) {
                attempts.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    try {
                        matches.changeMatchStatus(fixture.matchId(), MatchStatus.FINALIZADO, fixture.players().get(0));
                        return true;
                    } catch (IllegalArgumentException rejected) {
                        assertTrue(rejected.getMessage().contains("FINALIZADO"));
                        return false;
                    }
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            int successes = 0;
            for (var attempt : attempts) if (attempt.get(60, TimeUnit.SECONDS)) successes++;
            assertEquals(1, successes);
        }
        assertEquals("FINALIZADO", jdbc.queryForObject("select estado from matches where id=?", String.class, fixture.matchId()));
        assertEquals(10, jdbc.queryForObject(
                "select count(*) from rating_history where match_id=? and rotative_goalkeeper_mode=true",
                Integer.class, fixture.matchId()));
        assertEquals(10, jdbc.queryForObject("select count(*) from player_history where match_id=?", Integer.class, fixture.matchId()));
    }

    private Fixture createFixture() {
        List<PlayerProfile> players = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
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
        Match match = new Match(MatchMode.CINCO_VS_CINCO, LocalDateTime.now().minusHours(2), players.get(0));
        match.setMatchType(MatchType.INTERNAL);
        match.setRotativeGoalkeeper(true);
        match.setEstado(MatchStatus.INICIADO);
        match.setStartedAt(LocalDateTime.now().minusHours(2));
        entities.persist(match);
        entities.persist(new MatchTeam(match, team, true));
        for (int i = 0; i < players.size(); i++) {
            MatchPlayer participant = new MatchPlayer(match, team, players.get(i), position,
                    i == 0 ? PlayerRole.CAPITAN : PlayerRole.JUGADOR);
            participant.setConfirmado(true);
            participant.setTeamSide(i < 5 ? MatchTeamSide.LOCAL : MatchTeamSide.VISITA);
            entities.persist(participant);
        }
        entities.flush();
        return new Fixture(match.getId(), team.getId(), position.getId(), players.stream().map(PlayerProfile::getAtletaUuid).toList());
    }

    @AfterEach
    void cleanup() {
        if (fixture == null) return;
        new TransactionTemplate(transactions).executeWithoutResult(ignored -> {
            for (String table : List.of("rating_history", "player_history", "match_players", "match_teams")) {
                jdbc.update("delete from " + table + " where match_id=?", fixture.matchId());
            }
            jdbc.update("delete from matches where id=?", fixture.matchId());
            jdbc.update("delete from teams where id=?", fixture.teamId());
            for (UUID player : fixture.players()) {
                jdbc.update("delete from player_ratings where player_profile_id=?", player);
                jdbc.update("delete from player_profiles where atleta_uuid=?", player);
                jdbc.update("delete from athletes where atleta_uuid=?", player);
            }
            jdbc.update("delete from positions where id=?", fixture.positionId());
        });
    }

    private record Fixture(Long matchId, Long teamId, Long positionId, List<UUID> players) { }
}
