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
        jdbc.update("delete from rating_history where player_rating_id in (select id from player_ratings where player_profile_id in " + player + ")", email);
        jdbc.update("delete from matches where creador_user_id in " + player, email);
        jdbc.update("delete from player_ratings where player_profile_id in " + player, email);
        jdbc.update("delete from player_positions where player_id in " + player, email);
        jdbc.update("delete from refresh_sessions where athlete_uuid in " + player, email);
        jdbc.update("delete from player_profiles where atleta_uuid in " + player, email);
        jdbc.update("delete from athletes where email = ?", email);
    }

    @Test
    void returnsRatingsAndHistoryWithoutAnOpenPersistenceSession() throws Exception {
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
        mvc.perform(get(base + "/history").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(post(base + "/initialize-base").header("Authorization", auth)).andExpect(status().isOk());
        for (String path : new String[]{base, base + "/role/ARQUERO", base + "/priority/PRINCIPAL"}) {
            mvc.perform(get(path).header("Authorization", auth)).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].playerAlias").value("Prueba"));
        }

        String matchBody = mvc.perform(post("/api/v1/matches").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("modalidad", "CINCO_VS_CINCO",
                        "fechaHoraProgramada", "2026-11-27T04:00:00", "creadorUuid", uuid))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int matchId = json.readTree(matchBody).path("id").asInt();
        jdbc.update("""
                insert into rating_history (player_rating_id, match_id, previous_rating, new_rating, rating_delta,
                    goals_scored, assists_made, was_mvp, match_result, created_at)
                select id, ?, 70, 75, 5, 2, 0, true, 'GANADO', timestamp '2026-11-27 05:00:00'
                from player_ratings where player_profile_id = ? and role_type = 'ARQUERO'
                """, matchId, UUID.fromString(uuid));

        for (String path : new String[]{"/history", "/history/role/ARQUERO",
                "/history/period?startDate=2026-11-27T00:00:00&endDate=2026-11-28T00:00:00"}) {
            mvc.perform(get(base + path).header("Authorization", auth)).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].playerAlias").value("Prueba"))
                    .andExpect(jsonPath("$[0].roleType").value("ARQUERO"))
                    .andExpect(jsonPath("$[0].matchId").value(matchId))
                    .andExpect(jsonPath("$[0].goalsScored").value(2))
                    .andExpect(jsonPath("$[0].assistsMade").value(0))
                    .andExpect(jsonPath("$[0].wasMvp").value(true));
        }
    }
}
