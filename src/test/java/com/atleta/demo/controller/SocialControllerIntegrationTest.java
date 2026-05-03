package com.atleta.demo.controller;

import com.atleta.demo.config.TestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestConfig.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SocialControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private UUID playerUuid;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        playerUuid = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO athletes (atleta_uuid, nombre, email, password_hash, created_at, updated_at, version) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
                playerUuid,
                "Jugador Social",
                "social-" + playerUuid + "@example.com",
                "hashedpassword"
        );
        jdbcTemplate.update(
                "INSERT INTO player_profiles (atleta_uuid, alias, trust_score, created_at, updated_at, version) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
                playerUuid,
                "social-player",
                100
        );
    }

    @Test
    void registerPushToken_shouldCreateOrRefreshTokenForAuthenticatedUser() throws Exception {
        Map<String, Object> payload = Map.of(
                "token", "push-token-123",
                "platform", "android",
                "deviceId", "device-a"
        );

        mockMvc.perform(post("/api/v1/social/notifications/push-tokens")
                        .with(jwtFor(playerUuid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.playerUuid").value(playerUuid.toString()))
                .andExpect(jsonPath("$.platform").value("android"))
                .andExpect(jsonPath("$.deviceId").value("device-a"))
                .andExpect(jsonPath("$.active").value(true));

        Integer stored = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM push_notification_tokens WHERE recipient_user_id = ? AND token = ?",
                Integer.class,
                playerUuid,
                "push-token-123"
        );

        Assertions.assertEquals(1, stored);
    }

    @Test
    void unreadCount_shouldReturnOnlyUnreadNotificationsForAuthenticatedUser() throws Exception {
        jdbcTemplate.update(
                """
                INSERT INTO notifications
                    (recipient_user_id, type, title, message, is_read, created_at, updated_at, version)
                VALUES
                    (?, 'INVITACION_PARTIDO', 'Nueva', 'Pendiente', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
                    (?, 'INVITACION_EQUIPO', 'Leida', 'Ya vista', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
                    (?, 'SOLICITUD_AMISTAD', 'Nueva 2', 'Pendiente 2', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                """,
                playerUuid,
                playerUuid,
                playerUuid
        );

        mockMvc.perform(get("/api/v1/social/notifications/unread-count")
                        .with(jwtFor(playerUuid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtFor(UUID subject) {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(subject.toString()));
    }
}
