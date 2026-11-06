package com.atleta.demo.security;

import com.atleta.demo.dto.request.CreateAthleteRequest;
import com.atleta.demo.dto.request.LoginRequest;
import com.atleta.demo.enums.GenderType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class JwtAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JdbcTemplate jdbc;

    @Value("${security.jwt.issuer}")
    private String issuer;

    private String athleteUuid;

    @BeforeEach
    void setUp() throws Exception {
        CreateAthleteRequest request = new CreateAthleteRequest();
        request.setEmail("secure@example.com");
        request.setPassword("password123");
        request.setNombre("Secure Athlete");
        request.setGenero(GenderType.MASCULINO);

        String response = mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        athleteUuid = json.get("atletaUuid").asText();
    }

    @Test
    void loginReturnsSignedJwtWithExpectedClaims() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("secure@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/athletes/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken", not(emptyOrNullString())))
            .andExpect(jsonPath("$.atletaUuid").value(athleteUuid))
            .andExpect(jsonPath("$.email").value("secure@example.com"));
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/athletes/stats"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointAcceptsValidToken() throws Exception {
        String token = loginAndExtractToken();

        mockMvc.perform(get("/api/v1/athletes/stats")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void ratingsLeaderboardRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/ratings/leaderboard"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void globalReadEndpointsRejectMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/positions"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/fields"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/matches/upcoming"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/player-profiles/search")
                .param("nombre", "Secure"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/player-profiles/by-trust-score")
                .param("minScore", "0")
                .param("maxScore", "1000"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void ratingsWriteEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(post("/api/v1/ratings/update")
                .contentType(APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void sensitiveManualWritesAreForbiddenForAuthenticatedUsers() throws Exception {
        String token = loginAndExtractToken();
        String auth = "Bearer " + token;
        mockMvc.perform(put("/api/v1/player-profiles/trust-score")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/player-profiles/" + athleteUuid + "/positions/1/experience?xp=10000"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/fields/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/ratings/update").header("Authorization", auth)
                .contentType(APPLICATION_JSON).content("{\"matchId\":1,\"performances\":[{\"playerProfileId\":\"" + athleteUuid + "\",\"roleType\":\"ATAQUE\",\"priorityLevel\":\"PRINCIPAL\",\"goalsScored\":1,\"assistsMade\":0,\"wasMvp\":false,\"matchResult\":\"GANADO\"}]}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/player-profiles/trust-score")
                .header("Authorization", auth).contentType(APPLICATION_JSON).content("{\"cambio\":10,\"motivo\":\"Prueba\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/player-profiles/" + athleteUuid + "/positions/1/experience?xp=10000")
                .header("Authorization", auth)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/fields/1")
                .header("Authorization", auth).contentType(APPLICATION_JSON)
                .content("{\"nombre\":\"Cancha\",\"direccion\":\"Calle\",\"ciudad\":\"Ciudad\",\"latitud\":-36,\"longitud\":-73,\"activo\":false}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/fields").header("Authorization", auth)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/fields").header("Authorization", auth).contentType(APPLICATION_JSON)
                .content("{\"nombre\":\"Cancha\",\"direccion\":\"Calle\",\"ciudad\":\"Ciudad\",\"latitud\":-36,\"longitud\":-73}"))
                .andExpect(status().isCreated());
    }

    @Test
    void rotativeGoalkeeperUpdateRemainsBlockedWithValidToken() throws Exception {
        String body = "{\"matchId\":42,\"matchResult\":\"GANADO\"}";
        mockMvc.perform(post("/api/v1/ratings/update-rotative-goalkeeper")
                .contentType(APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/ratings/update-rotative-goalkeeper")
                .header("Authorization", "Bearer " + loginAndExtractToken())
                .contentType(APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    void fieldLocationWriteEndpointsRejectMissingToken() throws Exception {
        mockMvc.perform(post("/api/v1/fields")
                .contentType(APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void onlyCreatorCanEditFieldAndLegacyFieldsRemainProtected() throws Exception {
        String ownerAuth = "Bearer " + loginAndExtractToken();
        String body = "{\"nombre\":\"Cancha Propia\",\"direccion\":\"Calle\",\"ciudad\":\"Ciudad\",\"latitud\":-36,\"longitud\":-73,\"creatorUuid\":\"11111111-1111-1111-1111-111111111111\"}";
        String created = mockMvc.perform(post("/api/v1/fields").header("Authorization", ownerAuth)
                .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "select creator_uuid from field_locations where id = ?", java.util.UUID.class, id).toString())
                .isEqualTo(athleteUuid);

        mockMvc.perform(post("/api/v1/athletes/register").contentType(APPLICATION_JSON)
                .content("{\"email\":\"other-field@example.com\",\"password\":\"password123\",\"nombre\":\"Other\",\"genero\":\"MASCULINO\"}"))
                .andExpect(status().isCreated());
        String login = mockMvc.perform(post("/api/v1/athletes/login").contentType(APPLICATION_JSON)
                .content("{\"email\":\"other-field@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String otherAuth = "Bearer " + objectMapper.readTree(login).get("accessToken").asText();
        String update = "{\"nombre\":\"Cancha Propia\",\"direccion\":\"Nueva\",\"ciudad\":\"Ciudad\",\"latitud\":-36,\"longitud\":-73,\"activo\":false}";

        mockMvc.perform(put("/api/v1/fields/{id}", id).contentType(APPLICATION_JSON).content(update))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/fields/{id}", id).header("Authorization", otherAuth)
                .contentType(APPLICATION_JSON).content(update)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/fields/{id}", id).header("Authorization", ownerAuth)
                .contentType(APPLICATION_JSON).content(update)).andExpect(status().isOk())
                .andExpect(jsonPath("$.direccion").value("Nueva"))
                .andExpect(jsonPath("$.activo").value(false));
        mockMvc.perform(put("/api/v1/fields/{id}", 999999).header("Authorization", ownerAuth)
                .contentType(APPLICATION_JSON).content(update)).andExpect(status().isNotFound());

        jdbc.update("insert into field_locations (nombre,direccion,ciudad,latitud,longitud,activo) values (?,?,?,?,?,?)",
                "Cancha Anterior", "Calle", "Ciudad", -36, -73, true);
        Long legacyId = jdbc.queryForObject("select id from field_locations where nombre = ?", Long.class, "Cancha Anterior");
        mockMvc.perform(put("/api/v1/fields/{id}", legacyId).header("Authorization", ownerAuth)
                .contentType(APPLICATION_JSON).content(update)).andExpect(status().isForbidden());
    }

    @Test
    void ratingsLeaderboardAcceptsValidToken() throws Exception {
        String token = loginAndExtractToken();

        mockMvc.perform(get("/api/v1/ratings/leaderboard")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void globalReadEndpointsAcceptValidToken() throws Exception {
        String token = loginAndExtractToken();

        mockMvc.perform(get("/api/v1/positions")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/fields")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/matches/upcoming")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/player-profiles/search")
                .header("Authorization", "Bearer " + token)
                .param("nombre", "Secure"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/player-profiles/by-trust-score")
                .header("Authorization", "Bearer " + token)
                .param("minScore", "0")
                .param("maxScore", "1000"))
            .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointRejectsInvalidToken() throws Exception {
        String token = loginAndExtractToken() + "tampered";

        mockMvc.perform(get("/api/v1/athletes/stats")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsExpiredToken() throws Exception {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(now.minus(2, ChronoUnit.HOURS))
            .expiresAt(now.minus(1, ChronoUnit.HOURS))
            .subject(athleteUuid)
            .claim("email", "secure@example.com")
            .build();

        String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(),
            claims
        )).getTokenValue();

        mockMvc.perform(get("/api/v1/athletes/stats")
                .header("Authorization", "Bearer " + expiredToken))
            .andExpect(status().isUnauthorized());
    }

    private String loginAndExtractToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("secure@example.com");
        request.setPassword("password123");

        String response = mockMvc.perform(post("/api/v1/athletes/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
