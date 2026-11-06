package com.atleta.demo.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE, printOnlyOnFailure = false)
@ActiveProfiles("test")
class PasswordResetIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @MockBean JavaMailSender mailSender;
    private final String email = "reset-" + UUID.randomUUID() + "@atleta.test";

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from password_reset_tokens where athlete_uuid in (select atleta_uuid from athletes where email = ?)", email);
        jdbc.update("delete from refresh_sessions where athlete_uuid in (select atleta_uuid from athletes where email = ?)", email);
        jdbc.update("delete from athletes where email = ?", email);
    }

    @Test
    void resetLinkChangesPasswordOnceAndRevokesExistingSession() throws Exception {
        postJson("/api/v1/athletes/register", Map.of("email", email, "password", "old-password-123",
                "nombre", "Reset Test", "genero", "MASCULINO"), 201);
        JsonNode login = json.readTree(postJson("/api/v1/athletes/login",
                Map.of("email", email, "password", "old-password-123"), 200));
        String access = login.path("accessToken").asText();
        String refresh = login.path("refreshToken").asText();
        assertThat(access).isNotBlank();
        assertThat(refresh).isNotBlank();

        postJson("/api/v1/athletes/password-reset/request", Map.of("email", "missing@atleta.test"), 202);
        verifyNoInteractions(mailSender);
        postJson("/api/v1/athletes/password-reset/request", Map.of("email", email), 202);

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly(email);
        var link = Pattern.compile("/password-reset\\?token=([A-Za-z0-9_-]+)")
                .matcher(message.getValue().getText());
        assertThat(link.find()).isTrue();
        String token = link.group(1);
        Map<String, String> confirmation = Map.of("token", token, "newPassword", "new-password-456");
        postJson("/api/v1/athletes/password-reset/confirm", confirmation, 204);
        postJson("/api/v1/athletes/password-reset/confirm", confirmation, 400);
        postJson("/api/v1/athletes/auth/refresh", Map.of("refreshToken", refresh), 401);
        mvc.perform(get("/api/v1/positions").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());
        postJson("/api/v1/athletes/login", Map.of("email", email, "password", "old-password-123"), 401);
        postJson("/api/v1/athletes/login", Map.of("email", email, "password", "new-password-456"), 200);
    }

    private String postJson(String path, Object body, int expectedStatus) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
    }
}
