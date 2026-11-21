package com.atleta.demo.controller;

import com.atleta.demo.dto.request.CreateTeamInviteRequest;
import com.atleta.demo.dto.response.TeamActiveMemberResponse;
import com.atleta.demo.service.AthleteService;
import com.atleta.demo.service.SocialService;
import com.atleta.demo.service.TeamService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CaptainTransferIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TeamService teamService;
    @Autowired private AthleteService athleteService;
    @Autowired private SocialService socialService;
    @Autowired private TransactionTemplate transactions;

    private Long teamId;
    private UUID captain;
    private UUID first;
    private UUID second;
    private UUID inactive;
    private UUID outsider;
    private Long pastMatchId;

    @BeforeEach
    void setUp() {
        captain = player();
        first = player();
        second = player();
        inactive = player();
        outsider = player();
        String name = "CAP05-" + UUID.randomUUID();
        jdbc.update("INSERT INTO teams(nombre, creador_user_id) VALUES (?, ?)", name, captain);
        teamId = jdbc.queryForObject("SELECT id FROM teams WHERE nombre = ?", Long.class, name);
        addMember(captain, "CAPITAN", true);
        addMember(first, "JUGADOR", true);
        addMember(second, "JUGADOR", true);
        addMember(inactive, "JUGADOR", false);
    }

    @AfterEach
    void removeFixture() {
        if (pastMatchId != null) {
            jdbc.update("DELETE FROM player_history WHERE match_id = ?", pastMatchId);
            jdbc.update("DELETE FROM match_players WHERE match_id = ?", pastMatchId);
            jdbc.update("DELETE FROM matches WHERE id = ?", pastMatchId);
            jdbc.update("DELETE FROM player_ratings WHERE player_profile_id = ?", first);
            jdbc.update("DELETE FROM team_stats WHERE team_id = ?", teamId);
        }
        jdbc.update("DELETE FROM notifications WHERE recipient_user_id = ? AND context_type = 'TEAM_INVITE'", outsider);
        jdbc.update("DELETE FROM team_invites WHERE team_id = ?", teamId);
        jdbc.update("DELETE FROM team_captain_transfers WHERE team_id = ?", teamId);
        jdbc.update("DELETE FROM team_members WHERE team_id = ?", teamId);
        jdbc.update("DELETE FROM teams WHERE id = ?", teamId);
        for (UUID player : List.of(captain, first, second, inactive, outsider)) {
            jdbc.update("DELETE FROM player_profiles WHERE atleta_uuid = ?", player);
            jdbc.update("DELETE FROM athletes WHERE atleta_uuid = ?", player);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deactivatesMembershipAndPreservesSportsHistory(boolean leaving) throws Exception {
        Long positionId = jdbc.queryForObject("SELECT MIN(id) FROM positions", Long.class);
        jdbc.update("""
                INSERT INTO matches(modalidad, fecha_hora_programada, creador_user_id, estado, categoria_genero, match_type)
                VALUES ('CINCO_VS_CINCO', CURRENT_TIMESTAMP, ?, 'FINALIZADO', 'MIXTO', 'FRIENDLY')
                """, captain);
        pastMatchId = jdbc.queryForObject("SELECT id FROM matches WHERE creador_user_id = ?", Long.class, captain);
        jdbc.update("""
                INSERT INTO match_players(match_id, team_id, user_id, position_id, rol, confirmado)
                VALUES (?, ?, ?, ?, 'JUGADOR', TRUE)
                """, pastMatchId, teamId, first, positionId);
        jdbc.update("""
                INSERT INTO player_history(match_id, team_id, user_id, position_id, goles, asistencias, resultado, xp_ganada)
                VALUES (?, ?, ?, ?, 2, 1, 'VICTORIA', 25)
                """, pastMatchId, teamId, first, positionId);
        jdbc.update("""
                INSERT INTO player_ratings(player_profile_id, role_type, priority_level, current_rating, matches_played)
                VALUES (?, 'ATAQUE', 'PRINCIPAL', 76.50, 6)
                """, first);
        jdbc.update("INSERT INTO team_stats(team_id, partidos_jugados, goles_favor) VALUES (?, 1, 2)", teamId);

        var participation = jdbc.queryForMap("SELECT * FROM match_players WHERE match_id = ?", pastMatchId);
        var history = jdbc.queryForMap("SELECT * FROM player_history WHERE match_id = ?", pastMatchId);
        var rating = jdbc.queryForMap("SELECT * FROM player_ratings WHERE player_profile_id = ?", first);
        var stats = jdbc.queryForMap("SELECT * FROM team_stats WHERE team_id = ?", teamId);
        var membership = jdbc.queryForMap("SELECT id, joined_at FROM team_members WHERE team_id = ? AND user_id = ?", teamId, first);

        (leaving ? leave(first) : remove(captain, first)).andExpect(status().isNoContent());
        (leaving ? leave(first) : remove(captain, first)).andExpect(status().isNotFound());

        assertThat(jdbc.queryForMap("SELECT id, joined_at FROM team_members WHERE team_id = ? AND user_id = ? AND NOT activo",
                teamId, first)).isEqualTo(membership);
        assertThat(jdbc.queryForMap("SELECT * FROM match_players WHERE match_id = ?", pastMatchId)).isEqualTo(participation);
        assertThat(jdbc.queryForMap("SELECT * FROM player_history WHERE match_id = ?", pastMatchId)).isEqualTo(history);
        assertThat(jdbc.queryForMap("SELECT * FROM player_ratings WHERE player_profile_id = ?", first)).isEqualTo(rating);
        assertThat(jdbc.queryForMap("SELECT * FROM team_stats WHERE team_id = ?", teamId)).isEqualTo(stats);
        assertCaptain(captain);
    }

    @Test
    void successorCanRemoveAPlayerAndTheFormerCaptainLosesPermission() throws Exception {
        transfer(captain, first).andExpect(status().isOk());
        remove(captain, second).andExpect(status().isForbidden());
        remove(first, second).andExpect(status().isNoContent());
        assertCaptain(first);
    }

    @Test
    void rejectsANormalMemberEvenIfTheRequestNamesTheCaptainAsActor() throws Exception {
        mvc.perform(delete("/api/v1/teams/{teamId}/members/{playerUuid}", teamId, second)
                        .param("actorUuid", captain.toString())
                        .with(jwt().jwt(token -> token.subject(first.toString()))))
                .andExpect(status().isForbidden());
        assertThat(activeRole(second)).isEqualTo("JUGADOR");
    }

    @Test
    void rejectsRemovingTheCaptainByAnotherMember() throws Exception {
        remove(first, captain).andExpect(status().isForbidden());
        assertCaptain(captain);
    }

    @Test
    void rejectsSelfRemovalAndRemovingACoach() throws Exception {
        remove(captain, captain).andExpect(status().isBadRequest());
        jdbc.update("UPDATE team_members SET rol = 'DT' WHERE team_id = ? AND user_id = ?", teamId, first);
        remove(captain, first).andExpect(status().isBadRequest());
        assertCaptain(captain);
        assertThat(activeRole(first)).isEqualTo("DT");
    }

    @Test
    void rejectsInactiveAndMissingMemberships() throws Exception {
        for (UUID target : List.of(inactive, outsider, UUID.randomUUID())) {
            remove(captain, target).andExpect(status().isNotFound());
        }
    }

    @Test
    void rejectsAmbiguousActiveMemberships() throws Exception {
        addMember(first, "JUGADOR", true);
        remove(captain, first).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM team_members WHERE team_id = ? AND user_id = ? AND activo",
                Long.class, teamId, first)).isEqualTo(2L);
    }

    @Test
    void rejectsRemovalFromAnArchivedTeam() throws Exception {
        jdbc.update("UPDATE teams SET archived = TRUE WHERE id = ?", teamId);
        remove(captain, first).andExpect(status().isNotFound());
        assertThat(activeRole(first)).isEqualTo("JUGADOR");
    }

    @Test
    void recordsOnlySuccessfulTransfersWithTheJwtActorAndTimestamp() throws Exception {
        transfer(captain, first).andExpect(status().isOk());
        transfer(captain, second).andExpect(status().isForbidden());
        transfer(first, inactive).andExpect(status().isBadRequest());
        transfer(first, second).andExpect(status().isOk());

        var transfers = jdbc.queryForList("""
                SELECT outgoing_captain_uuid, incoming_captain_uuid, actor_uuid, transferred_at
                FROM team_captain_transfers WHERE team_id = ? ORDER BY id
                """, teamId);
        assertThat(transfers).hasSize(2);
        assertThat(transfers.get(0)).containsEntry("outgoing_captain_uuid", captain)
                .containsEntry("incoming_captain_uuid", first)
                .containsEntry("actor_uuid", captain);
        assertThat(transfers.get(1)).containsEntry("outgoing_captain_uuid", first)
                .containsEntry("incoming_captain_uuid", second)
                .containsEntry("actor_uuid", first);
        assertThat(transfers).allSatisfy(transfer -> assertThat(transfer.get("transferred_at")).isNotNull());
    }

    @Test
    void rollsBackTheTransferWhenTheAuditWriteFails() throws Exception {
        jdbc.execute("""
                ALTER TABLE team_captain_transfers ADD CONSTRAINT cap05_reject_audit
                CHECK (team_id <> %d)
                """.formatted(teamId));
        try {
            transfer(captain, first).andExpect(status().isConflict());
            assertCaptain(captain);
            assertThat(activeRole(first)).isEqualTo("JUGADOR");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM team_captain_transfers WHERE team_id = ?",
                    Long.class, teamId)).isZero();
        } finally {
            jdbc.execute("ALTER TABLE team_captain_transfers DROP CONSTRAINT cap05_reject_audit");
        }
    }

    @Test
    void leavesOnlyTheMembershipIdentifiedByTheJwt() throws Exception {
        var membership = jdbc.queryForMap("SELECT id, joined_at FROM team_members WHERE team_id = ? AND user_id = ?", teamId, first);
        mvc.perform(delete("/api/v1/teams/{teamId}/members/me", teamId)
                        .param("actorUuid", second.toString())
                        .with(jwt().jwt(token -> token.subject(first.toString()))))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForMap("SELECT id, joined_at FROM team_members WHERE team_id = ? AND user_id = ? AND NOT activo",
                teamId, first)).isEqualTo(membership);
        assertThat(activeRole(second)).isEqualTo("JUGADOR");
        assertCaptain(captain);
        leave(first).andExpect(status().isNotFound());
    }

    @Test
    void captainMustTransferBeforeLeavingEvenIfTheCaptainFoundedTheTeam() throws Exception {
        leave(captain).andExpect(status().isConflict());
        transfer(captain, first).andExpect(status().isOk());
        leave(captain).andExpect(status().isNoContent());
        leave(first).andExpect(status().isConflict());
        assertCaptain(first);
    }

    @Test
    void rejectsLeavingAnArchivedTeamOrWithoutActiveMembership() throws Exception {
        leave(outsider).andExpect(status().isNotFound());
        leave(inactive).andExpect(status().isNotFound());
        jdbc.update("UPDATE teams SET archived = TRUE WHERE id = ?", teamId);
        leave(first).andExpect(status().isNotFound());
        assertThat(activeRole(first)).isEqualTo("JUGADOR");
    }

    @Test
    void leavingRequiresAuthenticationAndPermitsAnActiveCoach() throws Exception {
        mvc.perform(delete("/api/v1/teams/{teamId}/members/me", teamId)).andExpect(status().isUnauthorized());
        jdbc.update("UPDATE team_members SET rol = 'DT' WHERE team_id = ? AND user_id = ?", teamId, first);
        leave(first).andExpect(status().isNoContent());
        assertCaptain(captain);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void membershipChangeWaitsForTransferAndRechecksTheRoster(boolean leaving) throws Exception {
        CountDownLatch transferred = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        CountDownLatch removalStarted = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var transfer = pool.submit(() -> transactions.executeWithoutResult(status -> {
                teamService.transferCaptain(teamId, captain, first);
                transferred.countDown();
                try {
                    assertThat(allowCommit.await(10, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }));
            try {
                assertThat(transferred.await(10, TimeUnit.SECONDS)).isTrue();
                var removal = pool.submit(() -> {
                    removalStarted.countDown();
                    return (leaving ? leave(first) : remove(captain, first)).andReturn().getResponse().getStatus();
                });
                assertThat(removalStarted.await(10, TimeUnit.SECONDS)).isTrue();
                assertThrows(TimeoutException.class, () -> removal.get(250, TimeUnit.MILLISECONDS));
                allowCommit.countDown();
                transfer.get(10, TimeUnit.SECONDS);
                assertThat(removal.get(10, TimeUnit.SECONDS)).isEqualTo(leaving ? 409 : 403);
            } finally {
                allowCommit.countDown();
            }
        }
        assertCaptain(first);
    }

    private ResultActions leave(UUID actor) throws Exception {
        return mvc.perform(delete("/api/v1/teams/{teamId}/members/me", teamId)
                .with(jwt().jwt(token -> token.subject(actor.toString()))));
    }

    private ResultActions remove(UUID actor, UUID target) throws Exception {
        return mvc.perform(delete("/api/v1/teams/{teamId}/members/{playerUuid}", teamId, target)
                .with(jwt().jwt(token -> token.subject(actor.toString()))));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void teamWritesWaitForPendingArchiveAndSeeTheArchivedState(boolean inviting) throws Exception {
        assertSerializedWithPendingWrite(
                () -> teamService.deleteTeam(teamId, captain),
                () -> (inviting ? invite(captain, outsider, "") : transfer(captain, first))
                        .andReturn().getResponse().getStatus(),
                inviting ? 400 : 404);
        assertCaptain(captain);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM team_invites WHERE team_id = ?", Long.class, teamId)).isZero();
    }

    @Test
    void archiveWaitsForInvitationBeforeCleaningUpTheTeam() throws Exception {
        var request = new CreateTeamInviteRequest();
        request.setTeamId(teamId);
        request.setTargetUuid(outsider);
        assertSerializedWithPendingWrite(
                () -> socialService.createTeamInvite(captain, request),
                () -> archive(captain).andReturn().getResponse().getStatus(), 204);
        assertThat(jdbc.queryForObject("SELECT archived FROM teams WHERE id = ?", Boolean.class, teamId)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM team_invites WHERE team_id = ?", Long.class, teamId)).isZero();
    }

    @Test
    void archivingStillBelongsToTheFounderAfterTheCaptainChanges() throws Exception {
        transfer(captain, first).andExpect(status().isOk());
        archive(first).andExpect(status().isForbidden());
        archive(captain).andExpect(status().isNoContent());
        archive(captain).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT archived FROM teams WHERE id = ?", Boolean.class, teamId)).isTrue();
    }

    @Test
    void captainWithOtherActiveMembersMustTransferBeforeDeletingTheAccount() throws Exception {
        useGoogleAccount(captain);
        deleteAccount(captain).andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CAPTAIN_TRANSFER_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NULL FROM athletes WHERE atleta_uuid = ?",
                Boolean.class, captain)).isTrue();
        assertThat(jdbc.queryForObject("SELECT archived FROM teams WHERE id = ?", Boolean.class, teamId)).isFalse();
        transfer(captain, first).andExpect(status().isOk());
        deleteAccount(captain).andExpect(status().isNoContent());
        assertCaptain(first);
        assertThat(jdbc.queryForObject("SELECT archived FROM teams WHERE id = ?", Boolean.class, teamId)).isFalse();
    }

    @Test
    void archivesTheTeamWhenItsOnlyActiveMemberDeletesTheirAccount() throws Exception {
        transfer(captain, first).andExpect(status().isOk());
        jdbc.update("UPDATE team_members SET activo = FALSE WHERE team_id = ? AND user_id <> ?", teamId, first);
        invite(first, outsider, "").andExpect(status().isCreated());
        useGoogleAccount(first);
        deleteAccount(first).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT archived FROM teams WHERE id = ?", Boolean.class, teamId)).isTrue();
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM athletes WHERE atleta_uuid = ?",
                Boolean.class, first)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM team_members WHERE team_id = ?", Long.class, teamId)).isEqualTo(4L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM team_invites WHERE team_id = ?", Long.class, teamId)).isZero();
    }

    @Test
    void leavesAllTeamsActiveIfAnyCaptaincyStillNeedsToBeTransferred() throws Exception {
        useGoogleAccount(captain);
        jdbc.update("UPDATE team_members SET activo = FALSE WHERE team_id = ? AND user_id <> ?", teamId, captain);
        String otherName = "CAP05-" + UUID.randomUUID();
        jdbc.update("INSERT INTO teams(nombre, creador_user_id) VALUES (?, ?)", otherName, captain);
        Long otherTeam = jdbc.queryForObject("SELECT id FROM teams WHERE nombre = ?", Long.class, otherName);
        try {
            jdbc.update("INSERT INTO team_members(team_id, user_id, rol) VALUES (?, ?, 'CAPITAN'), (?, ?, 'JUGADOR')",
                    otherTeam, captain, otherTeam, first);
            deleteAccount(captain).andExpect(status().isConflict());
            assertThat(jdbc.queryForList("SELECT archived FROM teams WHERE id IN (?, ?)", Boolean.class, teamId, otherTeam))
                    .containsExactly(false, false);
        } finally {
            jdbc.update("DELETE FROM team_members WHERE team_id = ?", otherTeam);
            jdbc.update("DELETE FROM teams WHERE id = ?", otherTeam);
        }
    }

    @Test
    void anonymizedMembersDoNotPreventArchivingAndRemainInTeamHistory() throws Exception {
        useGoogleAccount(captain);
        jdbc.update("UPDATE athletes SET deleted_at = CURRENT_TIMESTAMP WHERE atleta_uuid IN (?, ?)", first, second);
        deleteAccount(captain).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT archived FROM teams WHERE id = ?", Boolean.class, teamId)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM team_members WHERE team_id = ?", Long.class, teamId)).isEqualTo(4L);
    }

    @Test
    void invitationCannotAddAMemberAfterTheSoloCaptainArchivesThroughAccountDeletion() throws Exception {
        useGoogleAccount(captain);
        jdbc.update("UPDATE team_members SET activo = FALSE WHERE team_id = ? AND user_id <> ?", teamId, captain);
        invite(captain, outsider, "").andExpect(status().isCreated());
        Long inviteId = jdbc.queryForObject("SELECT id FROM team_invites WHERE team_id = ?", Long.class, teamId);

        assertSerializedWithPendingWrite(
                () -> athleteService.deleteAccount(captain, null, "ELIMINAR"),
                () -> mvc.perform(put("/api/v1/social/team-invites/{inviteId}/decision", inviteId)
                                .with(jwt().jwt(token -> token.subject(outsider.toString())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accept\":true}"))
                        .andReturn().getResponse().getStatus(), 403);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM team_members WHERE team_id = ? AND user_id = ?",
                Long.class, teamId, outsider)).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void accountDeletionAndCaptainPromotionCheckEligibilityAfterWaiting(boolean deletingFirst) throws Exception {
        useGoogleAccount(first);
        if (deletingFirst) {
            assertSerializedWithPendingWrite(
                    () -> athleteService.deleteAccount(first, null, "ELIMINAR"),
                    () -> transfer(captain, first).andReturn().getResponse().getStatus(), 400);
            assertCaptain(captain);
        } else {
            assertSerializedWithPendingWrite(
                    () -> teamService.transferCaptain(teamId, captain, first),
                    () -> deleteAccount(first).andReturn().getResponse().getStatus(), 409);
            assertCaptain(first);
            assertThat(jdbc.queryForObject("SELECT deleted_at IS NULL FROM athletes WHERE atleta_uuid = ?",
                    Boolean.class, first)).isTrue();
        }
    }

    private void useGoogleAccount(UUID actor) {
        jdbc.update("UPDATE athletes SET auth_provider = 'GOOGLE' WHERE atleta_uuid = ?", actor);
    }

    private ResultActions deleteAccount(UUID actor) throws Exception {
        return mvc.perform(delete("/api/v1/athletes/{atletaUuid}", actor)
                .with(jwt().jwt(token -> token.subject(actor.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmation\":\"ELIMINAR\"}"));
    }

    private ResultActions archive(UUID actor) throws Exception {
        return mvc.perform(delete("/api/v1/teams/{teamId}", teamId)
                .param("actorUuid", actor.toString())
                .with(jwt().jwt(token -> token.subject(actor.toString()))));
    }

    private void assertSerializedWithPendingWrite(Runnable firstWrite, Callable<Integer> secondWrite, int expectedStatus)
            throws Exception {
        CountDownLatch pending = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var firstOperation = pool.submit(() -> transactions.executeWithoutResult(status -> {
                firstWrite.run();
                pending.countDown();
                try {
                    assertThat(allowCommit.await(10, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }));
            try {
                assertThat(pending.await(10, TimeUnit.SECONDS)).isTrue();
                var secondOperation = pool.submit(() -> {
                    started.countDown();
                    return secondWrite.call();
                });
                assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();
                assertThrows(TimeoutException.class, () -> secondOperation.get(250, TimeUnit.MILLISECONDS));
                allowCommit.countDown();
                firstOperation.get(10, TimeUnit.SECONDS);
                assertThat(secondOperation.get(10, TimeUnit.SECONDS)).isEqualTo(expectedStatus);
            } finally {
                allowCommit.countDown();
            }
        }
    }

    @Test
    void letsTheFoundingCaptainInviteUsingOnlyTheJwt() throws Exception {
        invite(captain, outsider, "").andExpect(status().isCreated())
                .andExpect(jsonPath("$.requesterUuid").value(captain.toString()));
    }

    @Test
    void grantsInvitationPermissionToTheSuccessorAndRevokesItFromTheFounder() throws Exception {
        transfer(captain, first).andExpect(status().isOk());
        invite(captain, outsider, "").andExpect(status().isForbidden());
        invite(first, outsider, ",\"requesterUuid\":\"" + captain + "\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requesterUuid").value(first.toString()));
    }

    @Test
    void deniesMembersAndOutsidersEvenIfTheySendTheCaptainsUuid() throws Exception {
        String forgedRequester = ",\"requesterUuid\":\"" + captain + "\"";
        invite(first, outsider, forgedRequester).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        invite(outsider, inactive, forgedRequester).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM team_invites WHERE team_id = ?", Long.class, teamId))
                .isZero();
    }

    @Test
    void reportsValidationErrorsForAuthorizedCaptains() throws Exception {
        invite(captain, first, "").andExpect(status().isBadRequest());
        jdbc.update("UPDATE teams SET archived = TRUE WHERE id = ?", teamId);
        invite(captain, outsider, "").andExpect(status().isBadRequest());
    }

    private ResultActions invite(UUID actor, UUID target, String extraFields) throws Exception {
        return mvc.perform(post("/api/v1/social/team-invites")
                .with(jwt().jwt(token -> token.subject(actor.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"teamId\":" + teamId + ",\"targetUuid\":\"" + target + "\"" + extraFields + "}"));
    }

    @Test
    void transfersToAnActiveMemberAndKeepsBothMemberships() throws Exception {
        transfer(captain, first).andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(teamId))
                .andExpect(jsonPath("$.captainPlayerUuid").value(first.toString()));
        assertCaptain(first);
        assertThat(activeRole(captain)).isEqualTo("JUGADOR");
        transfer(captain, second).andExpect(status().isForbidden());
        transfer(first, second).andExpect(status().isOk());
        assertCaptain(second);
        assertThat(activeRole(first)).isEqualTo("JUGADOR");
    }

    @Test
    void preservesCoachingAlongsideThePlayingRoleAfterEachCaptaincy() throws Exception {
        jdbc.update("UPDATE team_members SET rol = 'DT' WHERE team_id = ? AND user_id = ?", teamId, first);
        var membershipBefore = jdbc.queryForMap("SELECT id, joined_at FROM team_members WHERE team_id = ? AND user_id = ?",
                teamId, first);

        transfer(captain, first).andExpect(status().isOk());
        assertCaptain(first);
        assertCoachInRoster(first, "CAPITAN");
        assertThat(activeRole(captain)).isEqualTo("JUGADOR");
        transfer(first, second).andExpect(status().isOk());
        assertCaptain(second);
        assertCoachInRoster(first, "JUGADOR");
        remove(second, first).andExpect(status().isBadRequest());

        transfer(second, first).andExpect(status().isOk());
        assertThat(activeRole(second)).isEqualTo("JUGADOR");
        transfer(first, captain).andExpect(status().isOk());
        assertCaptain(captain);
        assertCoachInRoster(first, "JUGADOR");
        assertThat(jdbc.queryForMap("SELECT id, joined_at FROM team_members WHERE team_id = ? AND user_id = ?",
                teamId, first)).isEqualTo(membershipBefore);
    }

    private void assertCoachInRoster(UUID player, String expectedRole) throws Exception {
        String body = mvc.perform(get("/api/v1/teams/{teamId}/members/active", teamId)
                        .with(jwt().jwt(token -> token.subject(player.toString()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<TeamActiveMemberResponse> roster = objectMapper.readValue(body, new TypeReference<>() {});
        assertThat(roster).filteredOn(member -> member.getPlayerUuid().equals(player)).singleElement()
                .satisfies(member -> {
                    assertThat(member.getRol().name()).isEqualTo(expectedRole);
                    assertThat(member.isEsDt()).isTrue();
                });
    }

    @Test
    void rejectsANonCaptain() throws Exception {
        transfer(first, second).andExpect(status().isForbidden());
        assertCaptain(captain);
    }

    @Test
    void rejectsSelfInactiveAndNonMemberDestinations() throws Exception {
        for (UUID invalid : List.of(captain, inactive, UUID.randomUUID())) {
            transfer(captain, invalid).andExpect(status().isBadRequest());
        }
        assertCaptain(captain);
    }

    @Test
    void rejectsArchivedTeams() throws Exception {
        jdbc.update("UPDATE teams SET archived = TRUE WHERE id = ?", teamId);
        transfer(captain, first).andExpect(status().isNotFound());
        assertCaptain(captain);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = "jdbc:postgresql:.*")
    void rollsBackTheDemotionWhenPromotionFails() throws Exception {
        jdbc.execute("""
                ALTER TABLE team_members ADD CONSTRAINT cap05_reject_promotion
                CHECK (user_id <> CAST('%s' AS UUID) OR rol <> 'CAPITAN')
                """.formatted(first));
        try {
            transfer(captain, first).andExpect(status().isConflict());
            assertCaptain(captain);
            assertThat(activeRole(first)).isEqualTo("JUGADOR");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM team_captain_transfers WHERE team_id = ?",
                    Long.class, teamId)).isZero();
        } finally {
            jdbc.execute("ALTER TABLE team_members DROP CONSTRAINT cap05_reject_promotion");
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = "jdbc:postgresql:.*")
    void serializesCompetingTransfersInPostgres() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var one = pool.submit(() -> compete(first, ready, start));
            var two = pool.submit(() -> compete(second, ready, start));
            try {
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            int firstStatus = one.get(15, TimeUnit.SECONDS);
            int secondStatus = two.get(15, TimeUnit.SECONDS);
            assertThat(List.of(firstStatus, secondStatus)).containsExactlyInAnyOrder(200, 403);
            UUID winner = firstStatus == 200 ? first : second;
            assertCaptain(winner);
            assertThat(jdbc.queryForList("SELECT incoming_captain_uuid FROM team_captain_transfers WHERE team_id = ?",
                    UUID.class, teamId)).containsExactly(winner);
            assertThat(activeRole(captain)).isEqualTo("JUGADOR");
            assertThat(activeRole(firstStatus == 200 ? second : first)).isEqualTo("JUGADOR");
        }
    }

    private int compete(UUID target, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
        return transfer(captain, target).andReturn().getResponse().getStatus();
    }

    private ResultActions transfer(UUID actor, UUID target) throws Exception {
        return mvc.perform(put("/api/v1/teams/{teamId}/captain", teamId)
                .with(jwt().jwt(token -> token.subject(actor.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetPlayerUuid\":\"" + target + "\"}"));
    }

    private void assertCaptain(UUID expected) {
        assertThat(jdbc.queryForList("""
                SELECT user_id FROM team_members WHERE team_id = ? AND activo AND rol = 'CAPITAN'
                """, UUID.class, teamId)).containsExactly(expected);
    }

    private String activeRole(UUID player) {
        return jdbc.queryForObject("SELECT rol FROM team_members WHERE team_id = ? AND user_id = ? AND activo",
                String.class, teamId, player);
    }

    private UUID player() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO athletes(atleta_uuid, email, password_hash, nombre) VALUES (?, ?, 'hash', 'CAP05')",
                id, id + "@example.test");
        jdbc.update("INSERT INTO player_profiles(atleta_uuid) VALUES (?)", id);
        return id;
    }

    private void addMember(UUID player, String role, boolean active) {
        jdbc.update("INSERT INTO team_members(team_id, user_id, rol, activo) VALUES (?, ?, ?, ?)",
                teamId, player, role, active);
    }
}
