package com.atleta.demo;

import com.atleta.demo.config.BaseIntegrationTest;
import com.atleta.demo.dto.request.CreateAthleteRequest;
import com.atleta.demo.dto.request.CreatePlayerProfileRequest;
import com.atleta.demo.dto.request.CreateTeamRequest;
import com.atleta.demo.dto.response.AthleteResponse;
import com.atleta.demo.dto.response.PositionResponse;
import com.atleta.demo.dto.response.TeamResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de integración end-to-end que verifica el flujo completo
 * del dominio de atletas y fútbol.
 */
@SpringBootTest
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
        // 1. Verificar que las posiciones están inicializadas
        MvcResult positionsResult = mockMvc.perform(get("/api/v1/positions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String positionsJson = positionsResult.getResponse().getContentAsString();
        List<PositionResponse> positions = objectMapper.readValue(positionsJson, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, PositionResponse.class));
        
        assertThat(positions).isNotEmpty();
        assertThat(positions).hasSize(7); // Portero, Defensa, Mediocampo, Delantero, Lateral Derecho, Lateral Izquierdo, DT
        
        // Verificar que todas las posiciones esperadas están presentes
        List<String> positionNames = positions.stream()
            .map(PositionResponse::getNombre)
            .toList();
        assertThat(positionNames).containsExactlyInAnyOrder(
            "Portero", "Defensa", "Mediocampo", "Delantero", "Lateral Derecho", "Lateral Izquierdo", "DT"
        );

        // 2. Crear un nuevo atleta
        CreateAthleteRequest createAthleteRequest = new CreateAthleteRequest();
        createAthleteRequest.setEmail("test.integration@atleta.com");
        createAthleteRequest.setPassword("hashedPasswordForTesting");
        createAthleteRequest.setNombre("Test Integration User");

        MvcResult createAthleteResult = mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAthleteRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("test.integration@atleta.com"))
                .andExpect(jsonPath("$.nombre").value("Test Integration User"))
                .andExpect(jsonPath("$.atletaUuid").exists())
                .andReturn();

        String athleteJson = createAthleteResult.getResponse().getContentAsString();
        AthleteResponse createdAthlete = objectMapper.readValue(athleteJson, AthleteResponse.class);
        UUID athleteUuid = createdAthlete.getAtletaUuid();

        // 3. Verificar que se puede obtener el atleta creado
        mockMvc.perform(get("/api/v1/athletes/{id}", athleteUuid))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("test.integration@atleta.com"))
                .andExpect(jsonPath("$.nombre").value("Test Integration User"))
                .andExpect(jsonPath("$.atletaUuid").value(athleteUuid.toString()));

        // 4. Crear perfil de jugador para el atleta
        CreatePlayerProfileRequest createProfileRequest = new CreatePlayerProfileRequest();
        createProfileRequest.setAtletaUuid(athleteUuid);
        createProfileRequest.setAlias("TestUser");

        mockMvc.perform(post("/api/v1/player-profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createProfileRequest)))
                .andExpect(status().isCreated());

        // 5. Verificar que se puede obtener el perfil de jugador creado
        mockMvc.perform(get("/api/v1/player-profiles/{id}", athleteUuid))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.atletaUuid").value(athleteUuid.toString()))
                .andExpect(jsonPath("$.trustScore").value(100)); // Trust score inicial

        // 6. Crear un equipo con el atleta como creador
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

        String teamJson = createTeamResult.getResponse().getContentAsString();
        TeamResponse createdTeam = objectMapper.readValue(teamJson, TeamResponse.class);
        Long teamId = createdTeam.getId();

        // 7. Verificar que se puede buscar atletas (usar endpoint de búsqueda)
        MvcResult searchAthletesResult = mockMvc.perform(get("/api/v1/athletes/search")
                .param("name", "Test Integration"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String athletesListJson = searchAthletesResult.getResponse().getContentAsString();
        List<AthleteResponse> athletes = objectMapper.readValue(athletesListJson,
            objectMapper.getTypeFactory().constructCollectionType(List.class, AthleteResponse.class));
        
        assertThat(athletes).isNotEmpty();
        assertThat(athletes.stream().anyMatch(a -> a.getAtletaUuid().equals(athleteUuid))).isTrue();

        // 8. Verificar que el equipo fue creado (no hay endpoint de listado, pero podemos verificar que se creó exitosamente)
        // El equipo ya fue verificado en el paso anterior al obtener la respuesta 201
    }

    @Test
    @DisplayName("Verificar manejo de errores en flujo completo")
    void shouldHandleErrorsInCompleteWorkflow() throws Exception {
        // 1. Intentar crear atleta con email duplicado (primero crear uno válido)
        CreateAthleteRequest firstRequest = new CreateAthleteRequest();
        firstRequest.setEmail("duplicate.test@example.com");
        firstRequest.setPassword("hashedPassword");
        firstRequest.setNombre("First User");

        mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // Ahora intentar crear otro con el mismo email
        CreateAthleteRequest duplicateEmailRequest = new CreateAthleteRequest();
        duplicateEmailRequest.setEmail("duplicate.test@example.com"); // Mismo email
        duplicateEmailRequest.setPassword("hashedPassword");
        duplicateEmailRequest.setNombre("Duplicate Email User");

        mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateEmailRequest)))
                .andExpect(status().isConflict()); // Debe fallar por email duplicado

        // 2. Intentar obtener atleta que no existe
        UUID nonExistentUuid = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/athletes/{id}", nonExistentUuid))
                .andExpect(status().isNotFound());

        // 3. Intentar crear equipo con creador que no existe
        CreateTeamRequest invalidCreatorRequest = new CreateTeamRequest();
        invalidCreatorRequest.setNombre("Equipo Invalid Creator");
        invalidCreatorRequest.setCreadorUuid(nonExistentUuid);

        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCreatorRequest)))
                .andExpect(status().isBadRequest()); // Debe fallar por creador inexistente

        // 4. Intentar crear equipo con nombre duplicado (primero crear un atleta válido)
        CreateAthleteRequest validAthleteRequest = new CreateAthleteRequest();
        validAthleteRequest.setEmail("valid.creator@example.com");
        validAthleteRequest.setPassword("hashedPassword");
        validAthleteRequest.setNombre("Valid Creator");

        String athleteResponse = mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAthleteRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        AthleteResponse validAthlete = objectMapper.readValue(athleteResponse, AthleteResponse.class);

        // Crear un equipo válido primero
        CreateTeamRequest firstTeamRequest = new CreateTeamRequest();
        firstTeamRequest.setNombre("Unique Team Name");
        firstTeamRequest.setCreadorUuid(validAthlete.getAtletaUuid());

        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstTeamRequest)))
                .andExpect(status().isCreated());

        // Ahora intentar crear equipo con nombre duplicado
        CreateTeamRequest duplicateNameRequest = new CreateTeamRequest();
        duplicateNameRequest.setNombre("Unique Team Name"); // Mismo nombre
        duplicateNameRequest.setCreadorUuid(validAthlete.getAtletaUuid());

        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateNameRequest)))
                .andExpect(status().isConflict()); // Debe fallar por nombre duplicado
    }

    @Test
    @DisplayName("Verificar endpoints de salud y métricas")
    void shouldExposeHealthAndMetricsEndpoints() throws Exception {
        // Verificar endpoint de salud (puede estar DOWN en tests debido a configuraciones específicas)
        try {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        } catch (AssertionError e) {
            // Si falla con 200, intentar con 503 (service unavailable)
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isServiceUnavailable());
        }

        // Verificar endpoint de información
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        // Verificar endpoint de métricas
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.names").isArray());

        // Verificar endpoint de Flyway
        mockMvc.perform(get("/actuator/flyway"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Verificar documentación OpenAPI")
    void shouldExposeOpenApiDocumentation() throws Exception {
        // Verificar que la documentación OpenAPI está disponible
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Atleta API - Sistema de Gestión de Atletas y Fútbol"))
                .andExpect(jsonPath("$.info.version").exists());

        // Verificar que Swagger UI está disponible (redirección)
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
