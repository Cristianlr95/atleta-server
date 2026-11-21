package com.atleta.demo.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CaptainTransferIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    private Long teamId;
    private UUID captain;
    private UUID first;
    private UUID second;
    private UUID inactive;
    private UUID outsider;

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
        jdbc.update("DELETE FROM notifications WHERE recipient_user_id = ? AND context_type = 'TEAM_INVITE'", outsider);
        jdbc.update("DELETE FROM team_invites WHERE team_id = ?", teamId);
        jdbc.update("DELETE FROM team_members WHERE team_id = ?", teamId);
        jdbc.update("DELETE FROM teams WHERE id = ?", teamId);
        for (UUID player : List.of(captain, first, second, inactive, outsider)) {
            jdbc.update("DELETE FROM player_profiles WHERE atleta_uuid = ?", player);
            jdbc.update("DELETE FROM athletes WHERE atleta_uuid = ?", player);
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
            assertCaptain(firstStatus == 200 ? first : second);
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
