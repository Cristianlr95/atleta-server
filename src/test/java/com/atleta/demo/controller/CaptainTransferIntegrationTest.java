package com.atleta.demo.controller;

import com.atleta.demo.dto.request.CreateTeamInviteRequest;
import com.atleta.demo.dto.request.CreateTeamRequest;
import com.atleta.demo.dto.request.RespondRequestDecision;
import com.atleta.demo.dto.request.UpdateTeamRequest;
import com.atleta.demo.dto.response.AuthResponse;
import com.atleta.demo.dto.response.TeamActiveMemberResponse;
import com.atleta.demo.repository.AthleteRepository;
import com.atleta.demo.service.AthleteService;
import com.atleta.demo.service.JwtService;
import com.atleta.demo.service.SessionLifecycleService;
import com.atleta.demo.service.SocialService;
import com.atleta.demo.service.TeamService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
class CaptainTransferIntegrationTest {
    private static final String PASSWORD = "cap06-test-password";
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TeamService teamService;
    @Autowired private AthleteService athleteService;
    @Autowired private AthleteRepository athleteRepository;
    @Autowired private JwtService jwtService;
    @Autowired private SessionLifecycleService sessions;
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
        jdbc.update("DELETE FROM match_invites WHERE team_id = ?", teamId);
        if (pastMatchId != null) {
            jdbc.update("DELETE FROM player_history WHERE match_id = ?", pastMatchId);
            jdbc.update("DELETE FROM match_players WHERE match_id = ?", pastMatchId);
            jdbc.update("DELETE FROM matches WHERE id = ?", pastMatchId);
            jdbc.update("DELETE FROM player_ratings WHERE player_profile_id = ?", first);
        }
        jdbc.update("DELETE FROM player_positions WHERE player_id = ?", first);
        jdbc.update("DELETE FROM notifications WHERE recipient_user_id IN (?, ?) AND context_type = 'TEAM_INVITE'",
                captain, outsider);
        jdbc.update("DELETE FROM team_stats WHERE team_id = ?", teamId);
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
        seedSportsHistory();
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

    private void seedSportsHistory() {
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
    void foundingCaptainCanCreateAndArchiveATeam() throws Exception {
        String authorization = login(captain);
        String body = mvc.perform(post("/api/v1/teams").header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", "CAP03A-" + UUID.randomUUID(), "creadorUuid", captain))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long createdTeamId = objectMapper.readTree(body).get("id").asLong();
        try {
            assertThat(jdbc.queryForList("SELECT user_id FROM team_members WHERE team_id = ? AND activo AND rol = 'CAPITAN'",
                    UUID.class, createdTeamId)).containsExactly(captain);
            mvc.perform(delete("/api/v1/teams/{teamId}", createdTeamId).param("actorUuid", captain.toString())
                            .header("Authorization", authorization))
                    .andExpect(status().isNoContent());
            assertThat(jdbc.queryForObject("SELECT archived FROM teams WHERE id = ?", Boolean.class, createdTeamId))
                    .isTrue();
        } finally {
            jdbc.update("DELETE FROM team_members WHERE team_id = ?", createdTeamId);
            jdbc.update("DELETE FROM team_stats WHERE team_id = ?", createdTeamId);
            jdbc.update("DELETE FROM teams WHERE id = ?", createdTeamId);
        }
    }

    @Test
    void successorCanArchiveAndRetryWithoutChangingTheArchiveTimestamp() throws Exception {
        String successorAuth = login(first);
        invite(captain, outsider, "").andExpect(status().isCreated());
        transfer(captain, first).andExpect(status().isOk());
        assertThat(jdbc.queryForList("SELECT status FROM team_invites WHERE team_id = ?", String.class, teamId))
                .containsExactly("PENDIENTE");

        archiveWithJwt(successorAuth, first).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT archived FROM teams WHERE id = ?", Boolean.class, teamId)).isTrue();
        var timestamp = jdbc.queryForObject("SELECT archived_at FROM teams WHERE id = ?", LocalDateTime.class, teamId);
        assertThat(timestamp).isNotNull();
        archiveWithJwt(successorAuth, first).andExpect(status().isNoContent());
        archiveWithJwt(login(captain), captain).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT archived_at FROM teams WHERE id = ?", LocalDateTime.class, teamId))
                .isEqualTo(timestamp);
        assertThat(archiveInvites()).isEmpty();
        assertCaptain(first);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void formerFounderCannotArchiveWithOrWithoutActiveMembership(boolean leaves) throws Exception {
        String founderAuth = login(captain);
        invite(captain, outsider, "").andExpect(status().isCreated());
        transfer(captain, first).andExpect(status().isOk());
        if (leaves) {
            mvc.perform(delete("/api/v1/teams/{teamId}/members/me", teamId).header("Authorization", founderAuth))
                    .andExpect(status().isNoContent());
        }
        var beforeTeam = jdbc.queryForMap("SELECT * FROM teams WHERE id = ?", teamId);
        var beforeInvites = archiveInvites();
        int result = archiveWithJwt(founderAuth, captain).andReturn().getResponse().getStatus();

        assertSoftly(softly -> {
            softly.assertThat(result).as("former founder archive status").isEqualTo(403);
            softly.assertThat(jdbc.queryForMap("SELECT * FROM teams WHERE id = ?", teamId))
                    .as("team is unchanged").isEqualTo(beforeTeam);
            softly.assertThat(archiveInvites()).as("pending invites are unchanged").isEqualTo(beforeInvites);
        });
        assertCaptain(first);
    }

    @Test
    void rejectsUnauthorizedArchiveRequestsWithoutChangingTeamOrInvitations() throws Exception {
        invite(captain, outsider, "").andExpect(status().isCreated());
        var beforeTeam = jdbc.queryForMap("SELECT * FROM teams WHERE id = ?", teamId);
        var beforeInvites = archiveInvites();
        var beforeMembers = archiveMembers();
        String outsiderAuth = login(outsider);

        for (UUID actor : List.of(second, inactive)) {
            archiveWithJwt(login(actor), actor).andExpect(status().isForbidden());
        }
        archiveWithJwt(outsiderAuth, outsider).andExpect(status().isForbidden());
        archiveWithJwt(outsiderAuth, captain).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/teams/{teamId}", teamId).param("actorUuid", outsider.toString())
                        .header("Authorization", outsiderAuth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actorUuid\":\"" + captain + "\",\"role\":\"CAPITAN\",\"rol\":\"CAPITAN\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/teams/{teamId}", teamId).param("actorUuid", captain.toString()))
                .andExpect(status().isUnauthorized());

        assertThat(jdbc.queryForMap("SELECT * FROM teams WHERE id = ?", teamId)).isEqualTo(beforeTeam);
        assertThat(archiveInvites()).isEqualTo(beforeInvites);
        assertThat(archiveMembers()).isEqualTo(beforeMembers);
        assertCaptain(captain);
    }

    private ResultActions archiveWithJwt(String authorization, UUID actorUuid) throws Exception {
        return mvc.perform(delete("/api/v1/teams/{teamId}", teamId)
                .param("actorUuid", actorUuid.toString()).header("Authorization", authorization));
    }

    @Test
    void foundingCaptainCanEditTheNameWithASignedJwt() throws Exception {
        rename(teamId, login(captain), "{\"nombre\":\"  Atlética Ñandú ⚽  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(teamId))
                .andExpect(jsonPath("$.nombre").value("Atlética Ñandú ⚽"));
        assertThat(jdbc.queryForObject("SELECT nombre FROM teams WHERE id = ?", String.class, teamId))
                .isEqualTo("Atlética Ñandú ⚽");
    }

    @Test
    void editingPermissionFollowsTheCaptaincyAndNotTheFounder() throws Exception {
        String founderAuth = login(captain);
        String successorAuth = login(first);
        transfer(captain, first).andExpect(status().isOk());
        rename(teamId, founderAuth, "{\"nombre\":\"Otro nombre\"}").andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/teams/{teamId}/members/me", teamId).header("Authorization", founderAuth))
                .andExpect(status().isNoContent());
        rename(teamId, founderAuth, "{\"nombre\":\"Otro nombre\"}").andExpect(status().isForbidden());
        rename(teamId, successorAuth, "{\"nombre\":\"Nombre del capitán vigente\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre del capitán vigente"));
        assertCaptain(first);
    }

    @Test
    void nonCaptainsCannotEditAndArchivedTeamsAreUnavailable() throws Exception {
        String body = "{\"nombre\":\"Nuevo nombre\"}";
        var original = jdbc.queryForMap("SELECT * FROM teams WHERE id = ?", teamId);
        rename(teamId, null, body).andExpect(status().isUnauthorized());
        for (UUID actor : List.of(second, inactive, outsider)) {
            rename(teamId, login(actor), body).andExpect(status().isForbidden());
        }
        assertThat(jdbc.queryForMap("SELECT * FROM teams WHERE id = ?", teamId)).isEqualTo(original);
        String authorization = login(captain);
        rename(Long.MAX_VALUE, authorization, body).andExpect(status().isNotFound());
        archiveWithJwt(authorization, captain).andExpect(status().isNoContent());
        var archived = jdbc.queryForMap("SELECT * FROM teams WHERE id = ?", teamId);
        rename(teamId, authorization, body).andExpect(status().isNotFound());
        assertThat(jdbc.queryForMap("SELECT * FROM teams WHERE id = ?", teamId)).isEqualTo(archived);
    }

    private ResultActions rename(long id, String authorization, String body) throws Exception {
        var request = patch("/api/v1/teams/{teamId}", id).contentType(MediaType.APPLICATION_JSON).content(body);
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return mvc.perform(request);
    }

    @Test
    void rejectsInvalidNamesAndProtectedFieldsWithoutChangingTheTeam() throws Exception {
        String authorization = login(captain);
        invite(captain, outsider, "").andExpect(status().isCreated());
        var original = jdbc.queryForMap("SELECT * FROM teams WHERE id = ?", teamId);
        var invites = archiveInvites();
        var members = archiveMembers();

        for (String body : List.of("null", "[]", "{}", "{\"nombre\":null}", "{\"nombre\":\"\"}",
                "{\"nombre\":\"\\u00a0\\u2003\"}", "{\"nombre\":42}", "{\"nombre\":\"A\\u0000B\"}",
                "{\"nombre\":\"" + "A".repeat(101) + "\"}")) {
            rename(teamId, authorization, body).andExpect(status().isBadRequest());
        }
        for (String key : List.of("id", "creador", "creadorUuid", "actorUuid", "archived", "archivedAt",
                "version", "createdAt", "updatedAt", "members", "stats", "role", "rol", "descripcion",
                "logoUrl", "anioFundacion")) {
            rename(teamId, authorization, objectMapper.writeValueAsString(Map.of("nombre", "Otro FC", key, "ignored")))
                    .andExpect(status().isBadRequest());
        }
        assertThat(jdbc.queryForMap("SELECT * FROM teams WHERE id = ?", teamId)).isEqualTo(original);
        assertThat(archiveInvites()).isEqualTo(invites);
        assertThat(archiveMembers()).isEqualTo(members);
    }

    @Test
    void renamingChangesOnlyTheNameAndItsAuditMetadata() throws Exception {
        seedSportsHistory();
        jdbc.update("INSERT INTO player_positions(player_id, position_id, prioridad, xp) "
                + "SELECT ?, MIN(id), 1, 25 FROM positions", first);
        invite(captain, outsider, "").andExpect(status().isCreated());
        jdbc.update("""
                INSERT INTO match_invites(match_id, team_id, requester_user_id, target_user_id, status)
                VALUES (?, ?, ?, ?, 'PENDIENTE')
                """, pastMatchId, teamId, captain, outsider);
        jdbc.update("UPDATE teams SET logo_url = ?, anio_fundacion = ? WHERE id = ?", "/uploads/test-logo.png", 1998, teamId);
        var original = new LinkedHashMap<>(jdbc.queryForMap("SELECT * FROM teams WHERE id = ?", teamId));
        original.keySet().removeAll(List.of("nombre", "version", "updated_at"));
        var sports = sportsRecords();
        var members = archiveMembers();
        var teamInvites = archiveInvites();
        var matchInvites = archiveMatchInvites();
        String name = "Ñ".repeat(100);

        rename(teamId, login(captain), objectMapper.writeValueAsString(Map.of("nombre", name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(name))
                .andExpect(jsonPath("$.anioFundacion").value(1998));

        assertThat(jdbc.queryForMap("SELECT * FROM teams WHERE id = ?", teamId))
                .containsAllEntriesOf(original).containsEntry("nombre", name);
        assertThat(sportsRecords()).isEqualTo(sports);
        assertThat(archiveMembers()).isEqualTo(members);
        assertThat(archiveInvites()).isEqualTo(teamInvites);
        assertThat(archiveMatchInvites()).isEqualTo(matchInvites);
    }

    @Test
    void preservesGlobalNameUniquenessAndTreatsTheCurrentNameAsANoOp() throws Exception {
        long otherTeamId = teamService.createTeam(new CreateTeamRequest("Club Reservado", first)).getId();
        try {
            teamService.deleteTeam(otherTeamId, first);
            String authorization = login(captain);
            var original = jdbc.queryForMap("SELECT * FROM teams WHERE id = ?", teamId);

            rename(teamId, authorization, "{\"nombre\":\"   Club Reservado  \"}").andExpect(status().isConflict());
            rename(teamId, authorization, objectMapper.writeValueAsString(Map.of("nombre", original.get("nombre"))))
                    .andExpect(status().isOk());
            assertThat(jdbc.queryForMap("SELECT * FROM teams WHERE id = ?", teamId)).isEqualTo(original);
            rename(teamId, authorization, "{\"nombre\":\"club reservado\"}").andExpect(status().isOk());
        } finally {
            deleteExtraTeam(otherTeamId);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = "jdbc:postgresql:.*")
    void concurrentRenamesToTheSameNameLeaveOneWinnerAcrossTeams() throws Exception {
        long otherTeamId = teamService.createTeam(new CreateTeamRequest("Otro equipo", first)).getId();
        try {
            String authorization = login(first);
            assertSerializedWithPendingWrite(
                    () -> teamService.updateTeam(teamId, captain, new UpdateTeamRequest("Nombre común")),
                    () -> rename(otherTeamId, authorization, "{\"nombre\":\"Nombre común\"}")
                            .andReturn().getResponse().getStatus(), 409);
            assertThat(jdbc.queryForList("SELECT id FROM teams WHERE nombre = ?", Long.class, "Nombre común"))
                    .containsExactly(teamId);
            assertThat(jdbc.queryForObject("SELECT nombre FROM teams WHERE id = ?", String.class, otherTeamId))
                    .isEqualTo("Otro equipo");
        } finally {
            deleteExtraTeam(otherTeamId);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void waitingEditRechecksTheCaptaincyAndArchiveStatus(boolean archiveFirst) throws Exception {
        String authorization = login(captain);
        String original = jdbc.queryForObject("SELECT nombre FROM teams WHERE id = ?", String.class, teamId);
        assertSerializedWithPendingWrite(
                () -> {
                    if (archiveFirst) {
                        teamService.deleteTeam(teamId, captain);
                    } else {
                        teamService.transferCaptain(teamId, captain, first);
                    }
                },
                () -> rename(teamId, authorization, "{\"nombre\":\"No se debe aplicar\"}")
                        .andReturn().getResponse().getStatus(), archiveFirst ? 404 : 403);
        assertThat(jdbc.queryForObject("SELECT nombre FROM teams WHERE id = ?", String.class, teamId))
                .isEqualTo(original);
    }

    private void deleteExtraTeam(long id) {
        jdbc.update("DELETE FROM team_stats WHERE team_id = ?", id);
        jdbc.update("DELETE FROM team_members WHERE team_id = ?", id);
        jdbc.update("DELETE FROM teams WHERE id = ?", id);
    }

    private List<Map<String, Object>> archiveInvites() {
        return jdbc.queryForList("SELECT * FROM team_invites WHERE team_id = ? ORDER BY id", teamId);
    }

    @Test
    void archivePreservesSportsRecordsAndRollsBackInviteCleanupOnFailure() throws Exception {
        seedSportsHistory();
        jdbc.update("INSERT INTO player_positions(player_id, position_id, prioridad, xp) "
                + "SELECT ?, MIN(id), 1, 25 FROM positions", first);
        invite(captain, outsider, "").andExpect(status().isCreated());
        jdbc.update("""
                INSERT INTO match_invites(match_id, team_id, requester_user_id, target_user_id, status)
                VALUES (?, ?, ?, ?, 'PENDIENTE')
                """, pastMatchId, teamId, captain, outsider);
        var members = archiveMembers();
        var sports = sportsRecords();
        var teamInvitations = archiveInvites();
        var matchInvitations = archiveMatchInvites();
        var teamBefore = jdbc.queryForMap("SELECT * FROM teams WHERE id = ?", teamId);
        jdbc.update("ALTER TABLE teams ADD CONSTRAINT cap03a_test_keep_active CHECK (id <> " + teamId + " OR NOT archived)");
        try {
            assertThrows(DataIntegrityViolationException.class, () -> teamService.deleteTeam(teamId, captain));
        } finally {
            jdbc.update("ALTER TABLE teams DROP CONSTRAINT cap03a_test_keep_active");
        }
        assertThat(jdbc.queryForMap("SELECT * FROM teams WHERE id = ?", teamId)).isEqualTo(teamBefore);
        assertThat(archiveInvites()).isEqualTo(teamInvitations);
        assertThat(archiveMatchInvites()).isEqualTo(matchInvitations);

        String authorization = login(captain);
        archiveWithJwt(authorization, captain).andExpect(status().isNoContent());
        assertThat(archiveInvites()).isEmpty();
        assertThat(archiveMatchInvites()).isEmpty();
        assertThat(archiveMembers()).isEqualTo(members);
        assertThat(sportsRecords()).isEqualTo(sports);
        mvc.perform(get("/api/v1/teams/{teamId}/members/active", teamId).header("Authorization", authorization))
                .andExpect(status().isNotFound());
    }

    private List<Map<String, Object>> archiveMembers() {
        return jdbc.queryForList("SELECT * FROM team_members WHERE team_id = ? ORDER BY id", teamId);
    }

    private List<Map<String, Object>> archiveMatchInvites() {
        return jdbc.queryForList("SELECT * FROM match_invites WHERE team_id = ? ORDER BY id", teamId);
    }

    private List<Map<String, Object>> sportsRecords() {
        return List.of(jdbc.queryForMap("SELECT * FROM matches WHERE id = ?", pastMatchId),
                jdbc.queryForMap("SELECT * FROM match_players WHERE match_id = ?", pastMatchId),
                jdbc.queryForMap("SELECT * FROM player_history WHERE match_id = ?", pastMatchId),
                jdbc.queryForMap("SELECT * FROM player_ratings WHERE player_profile_id = ?", first),
                jdbc.queryForMap("SELECT * FROM player_positions WHERE player_id = ?", first),
                jdbc.queryForMap("SELECT * FROM team_stats WHERE team_id = ?", teamId));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void archiveWaitingForTransferUsesTheCommittedCaptain(boolean successor) throws Exception {
        UUID actor = successor ? first : captain;
        String authorization = login(actor);
        assertSerializedWithPendingWrite(
                () -> teamService.transferCaptain(teamId, captain, first),
                () -> archiveWithJwt(authorization, actor).andReturn().getResponse().getStatus(),
                successor ? 204 : 403);
        assertThat(jdbc.queryForObject("SELECT archived FROM teams WHERE id = ?", Boolean.class, teamId))
                .isEqualTo(successor);
        assertCaptain(first);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void archiveAndInviteAcceptanceSerializeWithoutPartialChanges(boolean acceptFirst) throws Exception {
        long inviteId = objectMapper.readTree(invite(captain, outsider, "").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        if (acceptFirst) {
            var decision = new RespondRequestDecision();
            decision.setAccept(true);
            decision.setActorUuid(outsider);
            assertSerializedWithPendingWrite(
                    () -> socialService.respondTeamInvite(inviteId, decision),
                    () -> archive(captain).andReturn().getResponse().getStatus(), 204);
        } else {
            String authorization = login(outsider);
            assertSerializedWithPendingWrite(
                    () -> teamService.deleteTeam(teamId, captain),
                    () -> mvc.perform(put("/api/v1/social/team-invites/{inviteId}/decision", inviteId)
                                    .header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"accept\":true}"))
                            .andReturn().getResponse().getStatus(), 403);
        }
        assertThat(jdbc.queryForObject("SELECT archived FROM teams WHERE id = ?", Boolean.class, teamId)).isTrue();
        assertThat(archiveInvites()).isEmpty();
        assertThat(jdbc.queryForList("SELECT rol FROM team_members WHERE team_id = ? AND user_id = ? AND activo",
                String.class, teamId, outsider)).containsExactlyElementsOf(acceptFirst ? List.of("JUGADOR") : List.of());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void archiveAndRemovalSerializeWithoutPartialChanges(boolean removeFirst) throws Exception {
        if (removeFirst) {
            assertSerializedWithPendingWrite(
                    () -> teamService.removeMember(teamId, captain, second),
                    () -> archive(captain).andReturn().getResponse().getStatus(), 204);
        } else {
            assertSerializedWithPendingWrite(
                    () -> teamService.deleteTeam(teamId, captain),
                    () -> remove(captain, second).andReturn().getResponse().getStatus(), 404);
        }
        assertThat(jdbc.queryForObject("SELECT archived FROM teams WHERE id = ?", Boolean.class, teamId)).isTrue();
        assertThat(jdbc.queryForObject("SELECT activo FROM team_members WHERE team_id = ? AND user_id = ?",
                Boolean.class, teamId, second)).isEqualTo(!removeFirst);
        assertCaptain(captain);
    }

    @Test
    void concurrentArchivesKeepTheAuthorizedRetryIdempotent() throws Exception {
        invite(captain, outsider, "").andExpect(status().isCreated());
        String authorization = login(captain);
        assertSerializedWithPendingWrite(
                () -> teamService.deleteTeam(teamId, captain),
                () -> archiveWithJwt(authorization, captain).andReturn().getResponse().getStatus(), 204);
        assertThat(jdbc.queryForObject("SELECT archived FROM teams WHERE id = ?", Boolean.class, teamId)).isTrue();
        assertThat(archiveInvites()).isEmpty();
        assertCaptain(captain);
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
    void authenticatesFounderAndSuccessorThroughTheCaptaincyLifecycle() throws Exception {
        String founderAuth = login(captain);
        String successorAuth = login(first);
        String memberAuth = login(second);
        String inviteBody = """
                {"teamId":%d,"targetUuid":"%s","requesterUuid":"%s"}
                """.formatted(teamId, outsider, captain);
        String transferBody = "{\"targetPlayerUuid\":\"" + first + "\"}";

        mvc.perform(put("/api/v1/teams/{teamId}/captain", teamId)
                        .contentType(MediaType.APPLICATION_JSON).content(transferBody))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/social/team-invites").header("Authorization", memberAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(inviteBody))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/teams/{teamId}/captain", teamId).header("Authorization", founderAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(transferBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captainPlayerUuid").value(first.toString()));
        mvc.perform(post("/api/v1/social/team-invites").header("Authorization", founderAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(inviteBody))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/social/team-invites").header("Authorization", successorAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(inviteBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requesterUuid").value(first.toString()));
        mvc.perform(delete("/api/v1/teams/{teamId}/members/{playerUuid}", teamId, second)
                        .header("Authorization", successorAuth))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/teams/{teamId}/members/me", teamId).header("Authorization", successorAuth))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/teams/{teamId}/members/me", teamId).header("Authorization", founderAuth))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/teams/{teamId}/members/active", teamId).header("Authorization", successorAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].playerUuid").value(first.toString()))
                .andExpect(jsonPath("$[0].rol").value("CAPITAN"));
        assertThat(jdbc.queryForList("SELECT actor_uuid FROM team_captain_transfers WHERE team_id = ?",
                UUID.class, teamId)).containsExactly(captain);
    }

    private String login(UUID playerUuid) throws Exception {
        return authorization(loginSession(playerUuid));
    }

    private AuthResponse loginSession(UUID playerUuid) throws Exception {
        jdbc.update("UPDATE athletes SET password_hash = ? WHERE atleta_uuid = ?", passwordEncoder.encode(PASSWORD), playerUuid);
        String body = loginWithPassword(playerUuid)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atletaUuid").value(playerUuid.toString()))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, AuthResponse.class);
    }

    private ResultActions loginWithPassword(UUID playerUuid) throws Exception {
        return mvc.perform(post("/api/v1/athletes/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", playerUuid + "@example.test", "password", PASSWORD))));
    }

    @Test
    void revokesEverySessionWhenTheFormerCaptainDeletesTheirAccount() throws Exception {
        AuthResponse primary = loginSession(captain);
        AuthResponse secondary = loginSession(captain);
        AuthResponse other = loginSession(first);
        transfer(captain, first).andExpect(status().isOk());
        jdbc.update("INSERT INTO team_stats(team_id, partidos_jugados, goles_favor) VALUES (?, 1, 2)", teamId);
        var stats = jdbc.queryForMap("SELECT * FROM team_stats WHERE team_id = ?", teamId);
        var members = jdbc.queryForList("SELECT * FROM team_members WHERE team_id = ? ORDER BY id", teamId);

        deleteAccount(primary).andExpect(status().isNoContent());

        int accessStatus = protectedRead(authorization(primary));
        int secondaryStatus = protectedRead(authorization(secondary));
        int archiveStatus = mvc.perform(delete("/api/v1/teams/{teamId}", teamId)
                        .param("actorUuid", captain.toString()).header("Authorization", authorization(primary)))
                .andReturn().getResponse().getStatus();
        int refreshStatus = refresh(primary).andReturn().getResponse().getStatus();
        int secondaryRefreshStatus = refresh(secondary).andReturn().getResponse().getStatus();
        assertSoftly(softly -> {
            softly.assertThat(accessStatus).as("original access").isEqualTo(401);
            softly.assertThat(secondaryStatus).as("other access").isEqualTo(401);
            softly.assertThat(refreshStatus).as("original refresh").isEqualTo(401);
            softly.assertThat(secondaryRefreshStatus).as("other refresh").isEqualTo(401);
            softly.assertThat(archiveStatus).as("former founder cannot archive after deletion").isEqualTo(401);
            softly.assertThat(unrevokedSessions(captain)).isZero();
            softly.assertThat(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM athletes WHERE atleta_uuid = ?",
                    Boolean.class, captain)).isTrue();
            softly.assertThat(jdbc.queryForObject("SELECT archived FROM teams WHERE id = ?", Boolean.class, teamId)).isFalse();
            softly.assertThat(jdbc.queryForList("SELECT * FROM team_members WHERE team_id = ? ORDER BY id", teamId)).isEqualTo(members);
            softly.assertThat(jdbc.queryForMap("SELECT * FROM team_stats WHERE team_id = ?", teamId)).isEqualTo(stats);
        });
        assertThat(protectedRead(authorization(other))).isEqualTo(200);
        loginWithPassword(captain).andExpect(status().isUnauthorized());
    }

    @Test
    void deniedCaptainDeletionKeepsBothSessionsUsable() throws Exception {
        AuthResponse primary = loginSession(captain);
        AuthResponse secondary = loginSession(captain);

        deleteAccount(primary).andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CAPTAIN_TRANSFER_REQUIRED"));

        assertThat(unrevokedSessions(captain)).isEqualTo(2);
        assertThat(protectedRead(authorization(primary))).isEqualTo(200);
        assertThat(protectedRead(authorization(secondary))).isEqualTo(200);
        refresh(primary).andExpect(status().isOk());
        refresh(primary).andExpect(status().isUnauthorized());
        assertCaptain(captain);
    }

    @Test
    void persistenceFailureRollsBackDeletionArchivingAndSessionRevocation() throws Exception {
        AuthResponse session = loginSession(captain);
        jdbc.update("UPDATE team_members SET activo = FALSE WHERE team_id = ? AND user_id <> ?", teamId, captain);
        jdbc.update("ALTER TABLE athletes ADD CONSTRAINT sec01_test_keep_account CHECK "
                + "(atleta_uuid <> CAST('" + captain + "' AS UUID) OR deleted_at IS NULL)");
        try {
            assertThrows(DataIntegrityViolationException.class,
                    () -> athleteService.deleteAccount(captain, PASSWORD, "ELIMINAR"));
        } finally {
            jdbc.update("ALTER TABLE athletes DROP CONSTRAINT sec01_test_keep_account");
        }
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NULL FROM athletes WHERE atleta_uuid = ?",
                Boolean.class, captain)).isTrue();
        assertThat(jdbc.queryForObject("SELECT archived FROM teams WHERE id = ?", Boolean.class, teamId)).isFalse();
        assertCaptain(captain);
        assertThat(unrevokedSessions(captain)).isEqualTo(1);
        assertThat(protectedRead(authorization(session))).isEqualTo(200);
        refresh(session).andExpect(status().isOk());
    }

    @Test
    void rejectsOldSignedCredentialsForAnAlreadyDeletedAccount() throws Exception {
        AuthResponse session = loginSession(outsider);
        var athlete = athleteRepository.findById(outsider).orElseThrow();
        String legacyAuthorization = "Bearer " + jwtService.generateToken(athlete);
        assertThat(protectedRead(legacyAuthorization)).isEqualTo(200);
        jdbc.update("UPDATE athletes SET deleted_at = CURRENT_TIMESTAMP WHERE atleta_uuid = ?", outsider);

        int accessStatus = protectedRead(authorization(session));
        int legacyStatus = protectedRead(legacyAuthorization);
        int refreshStatus = refresh(session).andReturn().getResponse().getStatus();
        assertSoftly(softly -> {
            softly.assertThat(accessStatus).as("unrevoked session for deleted account").isEqualTo(401);
            softly.assertThat(legacyStatus).as("token issued without sid").isEqualTo(401);
            softly.assertThat(refreshStatus).as("refresh for already deleted account").isEqualTo(401);
        });
        assertThrows(IllegalArgumentException.class, () -> sessions.createSession(athlete));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void accountDeletionAndRefreshUseTheSameAccountLock(boolean deletingFirst) throws Exception {
        AuthResponse old = loginSession(outsider);
        if (deletingFirst) {
            assertSerializedWithPendingWrite(
                    () -> athleteService.deleteAccount(outsider, PASSWORD, "ELIMINAR"),
                    () -> refresh(old).andReturn().getResponse().getStatus(), 401);
        } else {
            var rotated = new AtomicReference<AuthResponse>();
            assertSerializedWithPendingWrite(
                    () -> rotated.set(sessions.rotateRefreshToken(old.getRefreshToken())),
                    () -> deleteAccount(old).andReturn().getResponse().getStatus(), 204);
            assertThat(protectedRead(authorization(rotated.get()))).isEqualTo(401);
            refresh(rotated.get()).andExpect(status().isUnauthorized());
        }
        assertThat(unrevokedSessions(outsider)).isZero();
    }

    private static String authorization(AuthResponse session) {
        return "Bearer " + session.getAccessToken();
    }

    private ResultActions deleteAccount(AuthResponse session) throws Exception {
        return mvc.perform(delete("/api/v1/athletes/{atletaUuid}", session.getAtletaUuid())
                .header("Authorization", authorization(session)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("confirmation", "ELIMINAR",
                        "currentPassword", PASSWORD))));
    }

    private int protectedRead(String authorization) throws Exception {
        return mvc.perform(get("/api/v1/athletes/stats").header("Authorization", authorization))
                .andReturn().getResponse().getStatus();
    }

    private ResultActions refresh(AuthResponse session) throws Exception {
        return mvc.perform(post("/api/v1/athletes/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", session.getRefreshToken()))));
    }

    private long unrevokedSessions(UUID player) {
        return jdbc.queryForObject("SELECT count(*) FROM refresh_sessions WHERE athlete_uuid = ? AND revoked_at IS NULL",
                Long.class, player);
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
