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
