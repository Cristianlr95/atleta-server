package com.atleta.demo.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PlayerRatingReadIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    private final String email = "rating-read-" + UUID.randomUUID() + "@atleta.test";

    @AfterEach
    void cleanUp() {
        String player = "(select atleta_uuid from athletes where email = ?)";
        jdbc.update("delete from player_ratings where player_profile_id in " + player, email);
        jdbc.update("delete from player_positions where player_id in " + player, email);
        jdbc.update("delete from refresh_sessions where athlete_uuid in " + player, email);
        jdbc.update("delete from player_profiles where atleta_uuid in " + player, email);
        jdbc.update("delete from athletes where email = ?", email);
    }

    @Test
    void returnsPlayerAliasAfterOnboardingWithoutAnOpenPersistenceSession() throws Exception {
        String password = "TestLocal-1234";
        mvc.perform(post("/api/v1/athletes/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password,
                        "nombre", "Jugador", "genero", "MASCULINO"))))
                .andExpect(status().isCreated());
        String body = mvc.perform(post("/api/v1/athletes/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode login = json.readTree(body);
        String uuid = login.path("atletaUuid").asText();
        String auth = "Bearer " + login.path("accessToken").asText();
        mvc.perform(post("/api/v1/player-profiles").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("atletaUuid", uuid, "alias", "Prueba"))))
                .andExpect(status().isCreated());
        for (int i = 1; i <= 3; i++) {
            mvc.perform(post("/api/v1/player-profiles/positions").header("Authorization", auth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("playerUuid", uuid, "positionId", i, "prioridad", i))))
                    .andExpect(status().isCreated());
        }
        String base = "/api/v1/ratings/player/" + uuid;
        mvc.perform(post(base + "/initialize-base").header("Authorization", auth)).andExpect(status().isOk());
        for (String path : new String[]{base, base + "/role/ARQUERO", base + "/priority/PRINCIPAL"}) {
            mvc.perform(get(path).header("Authorization", auth)).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].playerAlias").value("Prueba"));
        }
    }
}
