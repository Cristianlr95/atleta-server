package com.atleta.demo.integration;

import com.atleta.demo.dto.request.RespondRequestDecision;
import com.atleta.demo.entity.Athlete;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchInvite;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.MatchTeam;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Position;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.enums.RequestStatus;
import com.atleta.demo.repository.AthleteRepository;
import com.atleta.demo.repository.MatchInviteRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchRepository;
import com.atleta.demo.repository.PlayerProfileRepository;
import com.atleta.demo.repository.PositionRepository;
import com.atleta.demo.repository.TeamRepository;
import com.atleta.demo.service.SocialService;
import com.atleta.demo.service.MatchRosterPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MatchInviteConcurrencyIntegrationTest {

    private Long fixtureMatchId;
    private List<UUID> fixtureNotificationRecipients = List.of();

    @Autowired private SocialService socialService;
    @Autowired private AthleteRepository athleteRepository;
    @Autowired private PlayerProfileRepository playerProfileRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private MatchPlayerRepository matchPlayerRepository;
    @Autowired private MatchInviteRepository matchInviteRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MatchRosterPolicy matchRosterPolicy;

    @Test
    void simultaneousAcceptancesNeverExceedCapacityAndOverflowGoesToWaitlist() throws Exception {
        removeH2LegacyWaitlistConstraint();
        Fixture fixture = new TransactionTemplate(transactionManager).execute(ignored -> createFixture());
        if (fixture == null) {
            throw new IllegalStateException("Could not create concurrency fixture");
        }
        fixtureMatchId = fixture.matchId();
        fixtureNotificationRecipients = List.of(
                fixture.creatorUuid(), fixture.firstTargetUuid(), fixture.secondTargetUuid());

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> firstResponse = executor.submit(() -> acceptWhenReleased(
                    fixture.firstInviteId(), fixture.firstTargetUuid(), ready, start));
            Future<?> secondResponse = executor.submit(() -> acceptWhenReleased(
                    fixture.secondInviteId(), fixture.secondTargetUuid(), ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS), "Both acceptance transactions should be ready");
            start.countDown();
            firstResponse.get(10, TimeUnit.SECONDS);
            secondResponse.get(10, TimeUnit.SECONDS);
        }

        Match reloadedMatch = matchRepository.findById(fixture.matchId()).orElseThrow();
        assertEquals(10, matchRosterPolicy.confirmedPlayerCount(
                reloadedMatch,
                matchPlayerRepository.findByMatch(reloadedMatch)
        ));
        List<RequestStatus> statuses = matchInviteRepository.findAllById(
                        List.of(fixture.firstInviteId(), fixture.secondInviteId()))
                .stream()
                .map(MatchInvite::getStatus)
                .toList();
        assertEquals(1, statuses.stream().filter(RequestStatus.ACEPTADA::equals).count());
        assertEquals(1, statuses.stream().filter(RequestStatus.LISTA_ESPERA::equals).count());
    }

    @AfterEach
    void removeConcurrencyFixture() {
        if (fixtureMatchId == null) {
            return;
        }
        // Esta suite comparte la misma H2 en memoria. Elimina primero las filas
        // dependientes para no contaminar las pruebas de repositorio posteriores.
        fixtureNotificationRecipients.forEach(recipientUuid ->
                jdbcTemplate.update("DELETE FROM notifications WHERE recipient_user_id = ?", recipientUuid));
        jdbcTemplate.update("DELETE FROM match_invites WHERE match_id = ?", fixtureMatchId);
        jdbcTemplate.update("DELETE FROM match_players WHERE match_id = ?", fixtureMatchId);
        jdbcTemplate.update("DELETE FROM match_teams WHERE match_id = ?", fixtureMatchId);
        jdbcTemplate.update("DELETE FROM matches WHERE id = ?", fixtureMatchId);
        fixtureMatchId = null;
        fixtureNotificationRecipients = List.of();
    }

    /**
     * V008 used an unnamed inline check. PostgreSQL names it
     * match_invites_status_check, while H2 generates CONSTRAINT_x and therefore
     * V025 cannot drop that legacy copy. Remove only that H2-only stale check so
     * this concurrency test exercises the current production constraint.
     */
    private void removeH2LegacyWaitlistConstraint() {
        String product = jdbcTemplate.execute(
                (ConnectionCallback<String>) connection -> connection.getMetaData().getDatabaseProductName());
        if (product == null || !product.equalsIgnoreCase("H2")) {
            return;
        }
        List<String> staleConstraints = jdbcTemplate.queryForList("""
                SELECT tc.constraint_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.check_constraints cc
                  ON cc.constraint_catalog = tc.constraint_catalog
                 AND cc.constraint_schema = tc.constraint_schema
                 AND cc.constraint_name = tc.constraint_name
                WHERE LOWER(tc.table_name) = 'match_invites'
                  AND UPPER(tc.constraint_type) = 'CHECK'
                  AND UPPER(cc.check_clause) LIKE '%STATUS%'
                  AND UPPER(cc.check_clause) NOT LIKE '%LISTA_ESPERA%'
                """, String.class);
        staleConstraints.forEach(name -> jdbcTemplate.execute(
                "ALTER TABLE match_invites DROP CONSTRAINT \"" + name.replace("\"", "\"\"") + "\""));
    }

    private Fixture createFixture() {
        PlayerProfile creator = savePlayer("creator");
        Team team = teamRepository.save(new Team("Concurrency " + UUID.randomUUID(), creator));
        Position position = positionRepository.save(new Position("Concurrency " + UUID.randomUUID()));

        Match match = new Match(MatchMode.CINCO_VS_CINCO, LocalDateTime.now().plusDays(1), creator);
        match.addMatchTeam(new MatchTeam(match, team, true));
        match = matchRepository.saveAndFlush(match);

        // Ocho filas confirmadas + el creador implícito dejan exactamente un cupo.
        for (int index = 0; index < 8; index++) {
            PlayerProfile confirmed = savePlayer("confirmed-" + index);
            MatchPlayer participant = new MatchPlayer(match, team, confirmed, position, PlayerRole.JUGADOR);
            participant.setConfirmado(true);
            matchPlayerRepository.save(participant);
        }
        matchPlayerRepository.flush();

        PlayerProfile firstTarget = savePlayer("first-target");
        PlayerProfile secondTarget = savePlayer("second-target");
        MatchInvite first = matchInviteRepository.save(new MatchInvite(match, team, creator, firstTarget, "join"));
        MatchInvite second = matchInviteRepository.save(new MatchInvite(match, team, creator, secondTarget, "join"));
        matchInviteRepository.flush();
        return new Fixture(
                match.getId(),
                creator.getAtletaUuid(),
                first.getId(),
                firstTarget.getAtletaUuid(),
                second.getId(),
                secondTarget.getAtletaUuid()
        );
    }

    private void acceptWhenReleased(Long inviteId, UUID actorUuid, CountDownLatch ready, CountDownLatch start) {
        try {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Acceptance start barrier timed out");
            }
            RespondRequestDecision decision = new RespondRequestDecision();
            decision.setActorUuid(actorUuid);
            decision.setAccept(true);
            socialService.respondMatchInvite(inviteId, decision);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private PlayerProfile savePlayer(String prefix) {
        String unique = prefix + "-" + UUID.randomUUID();
        Athlete athlete = athleteRepository.save(new Athlete(unique + "@example.test", "hash", unique));
        return playerProfileRepository.save(new PlayerProfile(athlete, prefix));
    }

    private record Fixture(
            Long matchId,
            UUID creatorUuid,
            Long firstInviteId,
            UUID firstTargetUuid,
            Long secondInviteId,
            UUID secondTargetUuid
    ) {
    }
}
