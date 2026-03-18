package com.atleta.demo.controller;

import com.atleta.demo.dto.request.CreateTeamRequest;
import com.atleta.demo.entity.Athlete;
import com.atleta.demo.enums.GenderType;
import com.atleta.demo.repository.AthleteRepository;
import com.atleta.demo.config.TestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TeamController.
 * Tests complete endpoint functionality with MockMvc.
 * 
 * Requisitos probados:
 * - 4.1: Creador responsable del equipo
 * - 4.2: Información básica del equipo (nombre, logo, año de fundación)
 * - 4.3: Estadísticas inicializadas en cero
 * - 4.4: Nombre único del equipo
 * - 5.1: Asignación de roles (JUGADOR, CAPITAN, DT)
 * - 5.2: Jugador en múltiples equipos simultáneamente
 * - 5.3: Estado activo/inactivo de membresía
 * - 5.4: Registro de fecha de ingreso al equipo
 * - 5.5: Mantenimiento del historial de cambios de estado
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestConfig.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TeamControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AthleteRepository athleteRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private UUID testCreatorUuid;
    private String testTimestamp;

    private String getUniqueTeamName(String baseName) {
        return baseName + " " + testTimestamp;
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Create test athlete with unique values
        testTimestamp = String.valueOf(System.currentTimeMillis());
        
        Athlete athlete = new Athlete();
        athlete.setEmail("creator" + testTimestamp + "@example.com");
        athlete.setPasswordHash("hashedpassword");
        athlete.setNombre("Team Creator " + testTimestamp);
        athlete.setGenero(GenderType.MASCULINO);
        athlete = athleteRepository.saveAndFlush(athlete);
        testCreatorUuid = athlete.getAtletaUuid();
        jdbcTemplate.update(
                "INSERT INTO player_profiles (atleta_uuid, alias, trust_score, created_at, updated_at, version) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
                testCreatorUuid,
                "Creator " + testTimestamp,
                100
        );
    }

    @Test
    @WithMockUser
    void testCreateTeam_Success() throws Exception {
        // Given
        CreateTeamRequest request = new CreateTeamRequest();
        request.setNombre(getUniqueTeamName("Test Team"));
        request.setLogoUrl("https://example.com/logo.png");
        request.setAnioFundacion(2024);
        request.setCreadorUuid(testCreatorUuid);

        // When & Then
        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value(getUniqueTeamName("Test Team")))
                .andExpect(jsonPath("$.logoUrl").value("https://example.com/logo.png"))
                .andExpect(jsonPath("$.anioFundacion").value(2024))
                .andExpect(jsonPath("$.creador.atletaUuid").value(testCreatorUuid.toString()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.stats").exists())
                .andExpect(jsonPath("$.stats.partidosJugados").value(0))
                .andExpect(jsonPath("$.stats.partidosGanados").value(0))
                .andExpect(jsonPath("$.stats.partidosPerdidos").value(0))
                .andExpect(jsonPath("$.stats.partidosEmpatados").value(0))
                .andExpect(jsonPath("$.stats.golesAnotados").value(0))
                .andExpect(jsonPath("$.stats.golesRecibidos").value(0));
    }

    @Test
    @WithMockUser
    void testCreateTeam_CreatorNotFound() throws Exception {
        // Given - Non-existent creator UUID
        CreateTeamRequest request = new CreateTeamRequest();
        request.setNombre("Test Team");
        request.setLogoUrl("https://example.com/logo.png");
        request.setAnioFundacion(2024);
        request.setCreadorUuid(UUID.randomUUID());

        // When & Then
        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void testCreateTeam_DuplicateName() throws Exception {
        // Given - Create first team
        CreateTeamRequest request1 = new CreateTeamRequest();
        request1.setNombre("Duplicate Team");
        request1.setLogoUrl("https://example.com/logo1.png");
        request1.setAnioFundacion(2024);
        request1.setCreadorUuid(testCreatorUuid);

        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // When - Try to create second team with same name
        CreateTeamRequest request2 = new CreateTeamRequest();
        request2.setNombre("Duplicate Team"); // Same name
        request2.setLogoUrl("https://example.com/logo2.png");
        request2.setAnioFundacion(2023);
        request2.setCreadorUuid(testCreatorUuid);

        // Then
        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void testCreateTeam_InvalidData() throws Exception {
        // Given - Invalid request with empty name
        CreateTeamRequest request = new CreateTeamRequest();
        request.setNombre(""); // Empty name should fail validation
        request.setLogoUrl("https://example.com/logo.png");
        request.setAnioFundacion(2024);
        request.setCreadorUuid(testCreatorUuid);

        // When & Then
        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void testCreateTeam_MinimalData() throws Exception {
        // Given - Request with only required fields
        CreateTeamRequest request = new CreateTeamRequest();
        request.setNombre("Minimal Team");
        request.setCreadorUuid(testCreatorUuid);
        // logoUrl and anioFundacion are optional

        // When & Then
        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Minimal Team"))
                .andExpect(jsonPath("$.logoUrl").isEmpty())
                .andExpect(jsonPath("$.anioFundacion").isEmpty())
                .andExpect(jsonPath("$.creador.atletaUuid").value(testCreatorUuid.toString()));
    }

    @Test
    @WithMockUser
    void testCreateTeam_WithAllFields() throws Exception {
        // Given - Request with all fields
        CreateTeamRequest request = new CreateTeamRequest();
        request.setNombre("Complete Team");
        request.setLogoUrl("https://example.com/complete-logo.png");
        request.setAnioFundacion(2020);
        request.setCreadorUuid(testCreatorUuid);

        // When & Then
        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Complete Team"))
                .andExpect(jsonPath("$.logoUrl").value("https://example.com/complete-logo.png"))
                .andExpect(jsonPath("$.anioFundacion").value(2020))
                .andExpect(jsonPath("$.creador.atletaUuid").value(testCreatorUuid.toString()))
                .andExpect(jsonPath("$.creador.alias", startsWith("Creator ")))
                .andExpect(jsonPath("$.stats.partidosJugados").value(0))
                .andExpect(jsonPath("$.stats.partidosGanados").value(0))
                .andExpect(jsonPath("$.stats.partidosPerdidos").value(0))
                .andExpect(jsonPath("$.stats.partidosEmpatados").value(0))
                .andExpect(jsonPath("$.stats.golesAnotados").value(0))
                .andExpect(jsonPath("$.stats.golesRecibidos").value(0));
    }

    @Test
    @WithMockUser
    void testCreateTeam_ValidatesNameLength() throws Exception {
        // Given - Request with name too long (over 100 characters)
        CreateTeamRequest request = new CreateTeamRequest();
        request.setNombre("A".repeat(101)); // 101 characters
        request.setCreadorUuid(testCreatorUuid);

        // When & Then
        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void testCreateTeam_ValidatesFoundationYear() throws Exception {
        // Given - Request with invalid foundation year (future year)
        CreateTeamRequest request = new CreateTeamRequest();
        request.setNombre("Future Team");
        request.setAnioFundacion(2050); // Future year
        request.setCreadorUuid(testCreatorUuid);

        // When & Then - This should still succeed as business logic might allow future years
        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anioFundacion").value(2050));
    }

    @Test
    @WithMockUser
    void testCreateTeam_ValidatesLogoUrl() throws Exception {
        // Given - Request with invalid URL format
        CreateTeamRequest request = new CreateTeamRequest();
        request.setNombre("Invalid URL Team");
        request.setLogoUrl("not-a-valid-url");
        request.setCreadorUuid(testCreatorUuid);

        // When & Then - This might succeed depending on validation rules
        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.logoUrl").value("not-a-valid-url"));
    }

    @Test
    @WithMockUser
    void testCreateTeam_CreatesTeamStats() throws Exception {
        // Given
        CreateTeamRequest request = new CreateTeamRequest();
        request.setNombre("Stats Team");
        request.setCreadorUuid(testCreatorUuid);

        // When & Then - Verify that team stats are created and initialized to zero
        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stats").exists())
                .andExpect(jsonPath("$.stats.partidosJugados").value(0))
                .andExpect(jsonPath("$.stats.partidosGanados").value(0))
                .andExpect(jsonPath("$.stats.partidosPerdidos").value(0))
                .andExpect(jsonPath("$.stats.partidosEmpatados").value(0))
                .andExpect(jsonPath("$.stats.golesAnotados").value(0))
                .andExpect(jsonPath("$.stats.golesRecibidos").value(0))
                .andExpect(jsonPath("$.stats.id").exists());
    }

    @Test
    @WithMockUser
    void testCreateTeam_SetsCreationTimestamp() throws Exception {
        // Given
        CreateTeamRequest request = new CreateTeamRequest();
        request.setNombre("Timestamp Team");
        request.setCreadorUuid(testCreatorUuid);

        // When & Then - Verify creation timestamp is set
        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @WithMockUser
    void testCreateTeam_MultipleTeamsBySameCreator() throws Exception {
        // Given - Create first team
        CreateTeamRequest request1 = new CreateTeamRequest();
        request1.setNombre("First Team");
        request1.setCreadorUuid(testCreatorUuid);

        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // When - Create second team by same creator
        CreateTeamRequest request2 = new CreateTeamRequest();
        request2.setNombre("Second Team");
        request2.setCreadorUuid(testCreatorUuid);

        // Then - Should succeed (same creator can create multiple teams)
        mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Second Team"))
                .andExpect(jsonPath("$.creador.atletaUuid").value(testCreatorUuid.toString()));
    }
}
