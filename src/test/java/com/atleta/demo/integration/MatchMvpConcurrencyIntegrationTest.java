package com.atleta.demo.integration;

import com.atleta.demo.entity.*;
import com.atleta.demo.enums.*;
import com.atleta.demo.service.MatchMvpService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "features.mvp-xp-bonus-enabled=true")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MatchMvpConcurrencyIntegrationTest {
    @Autowired MatchMvpService service;
    @Autowired EntityManager entities;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    private Fixture fixture;

    @Test
    void simultaneousLastVotesCloseOnceAndReloadsPreserveRewardsAndRatings() throws Exception {
        fixture = new TransactionTemplate(transactionManager).execute(ignored -> createFixture());
        UUID winner = fixture.players().get(0);
        for (int i = 0; i < 8; i++) {
            assertTrue(service.vote(fixture.matchId(), fixture.players().get(i), winner).isOpen());
        }
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var ninth = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return service.vote(fixture.matchId(), fixture.players().get(8), winner);
            });
            var tenth = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return service.vote(fixture.matchId(), fixture.players().get(9), winner);
            });
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            var first = ninth.get(10, TimeUnit.SECONDS);
            var last = tenth.get(10, TimeUnit.SECONDS);
            assertTrue(!first.isOpen() || !last.isOpen(), "The last response must close voting immediately");
        }
        for (UUID viewer : fixture.players()) {
            var state = service.getMvpState(fixture.matchId(), viewer);
            assertFalse(state.isOpen());
            assertEquals(winner, state.getWinnerUserId());
            assertEquals(10, state.getCandidates().size());
            assertEquals(10, state.getTally().get(0).getVotes());
        }
        assertThrows(IllegalArgumentException.class,
                () -> service.vote(fixture.matchId(), winner, fixture.players().get(1)));
        assertEquals(10, jdbc.queryForObject("select count(*) from match_mvp_votes where match_id=?", Integer.class, fixture.matchId()));
        assertEquals(35, jdbc.queryForObject("select xp_ganada from player_history where match_id=?", Integer.class, fixture.matchId()));
        assertEquals(10, jdbc.queryForObject("select mvp_bonus_xp from player_history where match_id=?", Integer.class, fixture.matchId()));
        assertEquals(110, jdbc.queryForObject("select xp from player_positions where player_id=?", Integer.class, winner));
        assertEquals(1, jdbc.queryForObject("select count(*) from rating_history where match_id=?", Integer.class, fixture.matchId()));
        assertEquals(0, BigDecimal.valueOf(70).compareTo(jdbc.queryForObject("select current_rating from player_ratings where player_profile_id=?", BigDecimal.class, winner)));
    }

    private Fixture createFixture() {
        List<PlayerProfile> players = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Athlete athlete = new Athlete(UUID.randomUUID() + "@mvp.test", "test-hash", "MVP " + i);
            entities.persist(athlete);
            PlayerProfile profile = new PlayerProfile(athlete, "MVP " + i);
            entities.persist(profile);
            players.add(profile);
        }
        Team team = new Team("MVP " + UUID.randomUUID(), players.get(0));
        entities.persist(team);
        Position position = new Position("MVP position " + UUID.randomUUID());
        entities.persist(position);
        Match match = new Match();
        match.setCreador(players.get(0));
        match.setModalidad(MatchMode.CINCO_VS_CINCO);
        match.setFechaHoraProgramada(LocalDateTime.now().minusHours(2));
        match.setEstado(MatchStatus.FINALIZADO);
        match.setFinalizedAt(LocalDateTime.now().minusMinutes(5));
        entities.persist(match);
        for (PlayerProfile profile : players) {
            MatchPlayer participant = new MatchPlayer(match, team, profile, position, PlayerRole.JUGADOR);
            participant.setConfirmado(true);
            entities.persist(participant);
        }
        entities.persist(new PlayerHistory(match, players.get(0), team, position, 1, 0, MatchResult.VICTORIA, 25));
        entities.persist(new PlayerPosition(players.get(0), position, 1, 100));
        PlayerRating rating = new PlayerRating(players.get(0), RoleType.MEDIOCAMPO, PriorityLevel.PRINCIPAL, BigDecimal.valueOf(70));
        entities.persist(rating);
        entities.persist(new RatingHistory(rating, match, BigDecimal.valueOf(69), BigDecimal.valueOf(70), BigDecimal.ONE, 1, 0, false, MatchResultType.GANADO));
        entities.flush();
        return new Fixture(match.getId(), team.getId(), position.getId(), players.stream().map(PlayerProfile::getAtletaUuid).toList());
    }

    @AfterEach
    void cleanup() {
        if (fixture == null) return;
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            for (String table : List.of("match_mvp_votes", "rating_history", "player_history", "match_players")) {
                jdbc.update("delete from " + table + " where match_id=?", fixture.matchId());
            }
            jdbc.update("delete from matches where id=?", fixture.matchId());
            jdbc.update("delete from teams where id=?", fixture.teamId());
            for (UUID player : fixture.players()) {
                jdbc.update("delete from player_positions where player_id=?", player);
                jdbc.update("delete from player_ratings where player_profile_id=?", player);
                jdbc.update("delete from player_profiles where atleta_uuid=?", player);
                jdbc.update("delete from athletes where atleta_uuid=?", player);
            }
            jdbc.update("delete from positions where id=?", fixture.positionId());
        });
    }

    private record Fixture(Long matchId, Long teamId, Long positionId, List<UUID> players) { }
}
