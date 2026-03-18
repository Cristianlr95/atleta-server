package com.atleta.demo;

import com.atleta.demo.config.BaseIntegrationTest;
import com.atleta.demo.dto.request.CreateAthleteRequest;
import com.atleta.demo.dto.request.CreatePlayerProfileRequest;
import com.atleta.demo.dto.request.CreateTeamRequest;
import com.atleta.demo.dto.response.AthleteResponse;
import com.atleta.demo.dto.response.PositionResponse;
import com.atleta.demo.dto.response.TeamResponse;
import com.atleta.demo.enums.GenderType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for the athlete domain.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("End-to-End Integration Tests - Atleta Domain")
class EndToEndAtletaIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    @Test
    @DisplayName("Flujo completo: Crear atleta, perfil, equipo y verificar datos")
    void shouldCompleteFullAtletaWorkflow() throws Exception {
        MvcResult positionsResult = mockMvc.perform(get("/api/v1/positions"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        String positionsJson = positionsResult.getResponse().getContentAsString();
        List<PositionResponse> positions = objectMapper.readValue(
            positionsJson,
            objectMapper.getTypeFactory().constructCollectionType(List.class, PositionResponse.class)
        );

        assertThat(positions).isNotEmpty();
        assertThat(positions).extracting(PositionResponse::getNombre).isNotEmpty();

        CreateAthleteRequest createAthleteRequest = new CreateAthleteRequest();
        createAthleteRequest.setEmail("test.integration@atleta.com");
        createAthleteRequest.setPassword("hashedPasswordForTesting");
        createAthleteRequest.setNombre("Test Integration User");
        createAthleteRequest.setGenero(GenderType.MASCULINO);

        MvcResult createAthleteResult = mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAthleteRequest)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.email").value("test.integration@atleta.com"))
            .andExpect(jsonPath("$.nombre").value("Test Integration User"))
            .andExpect(jsonPath("$.atletaUuid").exists())
            .andReturn();

        AthleteResponse createdAthlete = objectMapper.readValue(
            createAthleteResult.getResponse().getContentAsString(),
            AthleteResponse.class
        );
        UUID athleteUuid = createdAthlete.getAtletaUuid();

        mockMvc.perform(get("/api/v1/athletes/{id}", athleteUuid))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.email").value("test.integration@atleta.com"))
            .andExpect(jsonPath("$.nombre").value("Test Integration User"))
            .andExpect(jsonPath("$.atletaUuid").value(athleteUuid.toString()));

        CreatePlayerProfileRequest createProfileRequest = new CreatePlayerProfileRequest();
        createProfileRequest.setAtletaUuid(athleteUuid);
        createProfileRequest.setAlias("TestUser");

        mockMvc.perform(post("/api/v1/player-profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createProfileRequest)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/player-profiles/{id}", athleteUuid))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.atletaUuid").value(athleteUuid.toString()))
            .andExpect(jsonPath("$.trustScore").value(100));

        CreateTeamRequest createTeamRequest = new CreateTeamRequest();
        createTeamRequest.setNombre("Equipo Integration Test");
        createTeamRequest.setLogoUrl("https://example.com/logo-integration.png");
        createTeamRequest.setAnioFundacion(2024);
        createTeamRequest.setCreadorUuid(athleteUuid);

        MvcResult createTeamResult = mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createTeamRequest)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.nombre").value("Equipo Integration Test"))
            .andExpect(jsonPath("$.anioFundacion").value(2024))
            .andExpect(jsonPath("$.creador.atletaUuid").value(athleteUuid.toString()))
            .andReturn();

        TeamResponse createdTeam = objectMapper.readValue(
            createTeamResult.getResponse().getContentAsString(),
            TeamResponse.class
        );
        assertThat(createdTeam.getId()).isNotNull();

        MvcResult athletesResult = mockMvc.perform(get("/api/v1/athletes/search")
                .param("nombre", "Test Integration"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        List<AthleteResponse> athletes = objectMapper.readValue(
            athletesResult.getResponse().getContentAsString(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, AthleteResponse.class)
        );

        assertThat(athletes).isNotEmpty();
        assertThat(athletes.stream().anyMatch(athlete -> athlete.getAtletaUuid().equals(athleteUuid))).isTrue();
    }

    @Test
    @DisplayName("Verificar manejo de errores en flujo completo")
    void shouldHandleErrorsInCompleteWorkflow() throws Exception {
        CreateAthleteRequest firstRequest = new CreateAthleteRequest();
        firstRequest.setEmail("duplicate.test@example.com");
        firstRequest.setPassword("hashedPassword");
        firstRequest.setNombre("First User");
        firstRequest.setGenero(GenderType.MASCULINO);

        mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
            .andExpect(status().isCreated());

        CreateAthleteRequest duplicateEmailRequest = new CreateAthleteRequest();
        duplicateEmailRequest.setEmail("duplicate.test@example.com");
        duplicateEmailRequest.setPassword("hashedPassword");
        duplicateEmailRequest.setNombre("Duplicate Email User");
        duplicateEmailRequest.setGenero(GenderType.MASCULINO);

        mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateEmailRequest)))
            .andExpect(status().isConflict());

        UUID nonExistentUuid = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/athletes/{id}", nonExistentUuid))
            .andExpect(status().isNotFound());

        CreateTeamRequest invalidCreatorRequest = new CreateTeamRequest();
        invalidCreatorRequest.setNombre("Equipo Invalid Creator");
        invalidCreatorRequest.setCreadorUuid(nonExistentUuid);

        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCreatorRequest)))
            .andExpect(status().isConflict());

        CreateAthleteRequest validAthleteRequest = new CreateAthleteRequest();
        validAthleteRequest.setEmail("valid.creator@example.com");
        validAthleteRequest.setPassword("hashedPassword");
        validAthleteRequest.setNombre("Valid Creator");
        validAthleteRequest.setGenero(GenderType.MASCULINO);

        String athleteResponse = mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAthleteRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        AthleteResponse validAthlete = objectMapper.readValue(athleteResponse, AthleteResponse.class);

        CreatePlayerProfileRequest validProfileRequest = new CreatePlayerProfileRequest();
        validProfileRequest.setAtletaUuid(validAthlete.getAtletaUuid());
        validProfileRequest.setAlias("ValidCreator");

        mockMvc.perform(post("/api/v1/player-profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validProfileRequest)))
            .andExpect(status().isCreated());

        CreateTeamRequest firstTeamRequest = new CreateTeamRequest();
        firstTeamRequest.setNombre("Unique Team Name " + UUID.randomUUID());
        firstTeamRequest.setCreadorUuid(validAthlete.getAtletaUuid());

        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstTeamRequest)))
            .andExpect(status().isCreated());

        CreateTeamRequest duplicateNameRequest = new CreateTeamRequest();
        duplicateNameRequest.setNombre(firstTeamRequest.getNombre());
        duplicateNameRequest.setCreadorUuid(validAthlete.getAtletaUuid());

        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateNameRequest)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Verificar endpoints de salud y metricas")
    void shouldExposeHealthAndMetricsEndpoints() throws Exception {
        try {
            mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        } catch (AssertionError e) {
            mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable());
        }

        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/metrics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.names").isArray());

        mockMvc.perform(get("/actuator/flyway"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Verificar documentacion OpenAPI")
    void shouldExposeOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.openapi").exists())
            .andExpect(jsonPath("$.info.title").isNotEmpty())
            .andExpect(jsonPath("$.info.version").exists());

        mockMvc.perform(get("/swagger-ui.html"))
            .andExpect(status().is3xxRedirection());
    }
}
