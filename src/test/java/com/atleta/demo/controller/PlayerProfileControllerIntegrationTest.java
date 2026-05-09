package com.atleta.demo.controller;

import com.atleta.demo.dto.request.*;
import com.atleta.demo.entity.Athlete;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Position;
import com.atleta.demo.enums.GenderType;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.repository.AthleteRepository;
import com.atleta.demo.repository.MatchRepository;
import com.atleta.demo.repository.PlayerProfileRepository;
import com.atleta.demo.repository.PositionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.atleta.demo.config.TestConfig;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PlayerProfileController.
 * Tests complete endpoint functionality with MockMvc.
 * 
 * Requisitos probados:
 * - 2.1: Asociación de perfil a atleta existente
 * - 2.2: Trust score inicial de 100
 * - 2.3: Alias único para contexto de fútbol
 * - 2.4: Relación uno-a-uno entre atleta y perfil
 * - 2.5: Registro de cambios de trust score en trust_logs
 * - 3.2: Asignación de prioridades (1, 2, 3)
 * - 3.3: Contador de experiencia (XP) por posición
 * - 3.4: Validación de prioridades únicas por jugador
 * - 3.5: Múltiples posiciones con diferentes prioridades
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestConfig.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
public class PlayerProfileControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AthleteRepository athleteRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private UUID testAthleteUuid;
    private Long testPositionId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Create test athlete
        Athlete athlete = new Athlete();
        athlete.setEmail("test@example.com");
        athlete.setPasswordHash("hashedpassword");
        athlete.setNombre("Test Athlete");
        athlete.setGenero(GenderType.MASCULINO);
        athlete = athleteRepository.save(athlete);
        testAthleteUuid = athlete.getAtletaUuid();

        // Create test position
        Position position = findOrCreatePosition("Delantero");
        testPositionId = position.getId();
    }

    @Test
    void testCreatePlayerProfile_Success() throws Exception {
        // Given
        CreatePlayerProfileRequest request = new CreatePlayerProfileRequest();
        request.setAtletaUuid(testAthleteUuid);
        request.setAlias("TestPlayer");

        // When & Then
        mockMvc.perform(post("/api/v1/player-profiles")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.atletaUuid").value(testAthleteUuid.toString()))
                .andExpect(jsonPath("$.alias").value("TestPlayer"))
                .andExpect(jsonPath("$.trustScore").value(100))
                .andExpect(jsonPath("$.genero").value(GenderType.MASCULINO.name()));
    }

    @Test
    void testCreatePlayerProfile_AthleteNotFound() throws Exception {
        // Given - Non-existent athlete UUID
        UUID missingAthleteUuid = UUID.randomUUID();
        CreatePlayerProfileRequest request = new CreatePlayerProfileRequest();
        request.setAtletaUuid(missingAthleteUuid);
        request.setAlias("TestPlayer");

        // When & Then
        mockMvc.perform(post("/api/v1/player-profiles")
                .with(jwtFor(missingAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreatePlayerProfile_DuplicateProfile() throws Exception {
        // Given - Create first profile
        CreatePlayerProfileRequest request1 = new CreatePlayerProfileRequest();
        request1.setAtletaUuid(testAthleteUuid);
        request1.setAlias("FirstProfile");

        mockMvc.perform(post("/api/v1/player-profiles")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // When - Try to create second profile for same athlete
        CreatePlayerProfileRequest request2 = new CreatePlayerProfileRequest();
        request2.setAtletaUuid(testAthleteUuid);
        request2.setAlias("SecondProfile");

        // Then
        mockMvc.perform(post("/api/v1/player-profiles")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());
    }

    @Test
    void testGetPlayerProfileByUuid_Success() throws Exception {
        // Given - Create profile first
        CreatePlayerProfileRequest createRequest = new CreatePlayerProfileRequest();
        createRequest.setAtletaUuid(testAthleteUuid);
        createRequest.setAlias("GetTest");

        mockMvc.perform(post("/api/v1/player-profiles")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get("/api/v1/player-profiles/{atletaUuid}", testAthleteUuid)
                .with(jwtFor(testAthleteUuid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atletaUuid").value(testAthleteUuid.toString()))
                .andExpect(jsonPath("$.alias").value("GetTest"))
                .andExpect(jsonPath("$.trustScore").value(100));
    }

    @Test
    void testGetPlayerProfileByAlias_Success() throws Exception {
        // Given - Create profile first
        CreatePlayerProfileRequest createRequest = new CreatePlayerProfileRequest();
        createRequest.setAtletaUuid(testAthleteUuid);
        createRequest.setAlias("AliasTest");

        mockMvc.perform(post("/api/v1/player-profiles")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get("/api/v1/player-profiles/by-alias/{alias}", "AliasTest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alias").value("AliasTest"))
                .andExpect(jsonPath("$.atletaUuid").value(testAthleteUuid.toString()));
    }

    @Test
    void testUpdatePlayerProfile_Success() throws Exception {
        // Given - Create profile first
        CreatePlayerProfileRequest createRequest = new CreatePlayerProfileRequest();
        createRequest.setAtletaUuid(testAthleteUuid);
        createRequest.setAlias("OriginalAlias");

        mockMvc.perform(post("/api/v1/player-profiles")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // When - Update alias
        UpdatePlayerProfileRequest updateRequest = new UpdatePlayerProfileRequest();
        updateRequest.setAlias("UpdatedAlias");

        // Then
        mockMvc.perform(put("/api/v1/player-profiles/{atletaUuid}", testAthleteUuid)
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alias").value("UpdatedAlias"));
    }

    @Test
    void testAddPlayerPosition_Success() throws Exception {
        // Given - Create profile first
        CreatePlayerProfileRequest createRequest = new CreatePlayerProfileRequest();
        createRequest.setAtletaUuid(testAthleteUuid);
        createRequest.setAlias("PositionTest");

        mockMvc.perform(post("/api/v1/player-profiles")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // When - Add position
        AddPlayerPositionRequest positionRequest = new AddPlayerPositionRequest();
        positionRequest.setPlayerUuid(testAthleteUuid);
        positionRequest.setPositionId(testPositionId);
        positionRequest.setPrioridad(1);

        // Then
        mockMvc.perform(post("/api/v1/player-profiles/positions")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(positionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prioridad").value(1))
                .andExpect(jsonPath("$.xp").value(0))
                .andExpect(jsonPath("$.position.nombre").value("Delantero"));
    }

    @Test
    void testAddPlayerPosition_DuplicatePriority() throws Exception {
        // Given - Create profile and add first position
        CreatePlayerProfileRequest createRequest = new CreatePlayerProfileRequest();
        createRequest.setAtletaUuid(testAthleteUuid);
        createRequest.setAlias("PriorityTest");

        mockMvc.perform(post("/api/v1/player-profiles")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        AddPlayerPositionRequest firstPosition = new AddPlayerPositionRequest();
        firstPosition.setPlayerUuid(testAthleteUuid);
        firstPosition.setPositionId(testPositionId);
        firstPosition.setPrioridad(1);

        mockMvc.perform(post("/api/v1/player-profiles/positions")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstPosition)))
                .andExpect(status().isCreated());

        // Create another position
        Position position2 = findOrCreatePosition("Mediocampista");

        // When - Try to add second position with same priority
        AddPlayerPositionRequest secondPosition = new AddPlayerPositionRequest();
        secondPosition.setPlayerUuid(testAthleteUuid);
        secondPosition.setPositionId(position2.getId());
        secondPosition.setPrioridad(1); // Same priority

        // Then
        mockMvc.perform(post("/api/v1/player-profiles/positions")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondPosition)))
                .andExpect(status().isConflict());
    }

    @Test
    void testGetPlayerPositions_Success() throws Exception {
        // Given - Create profile and add positions
        CreatePlayerProfileRequest createRequest = new CreatePlayerProfileRequest();
        createRequest.setAtletaUuid(testAthleteUuid);
        createRequest.setAlias("PositionsTest");

        mockMvc.perform(post("/api/v1/player-profiles")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // Add first position
        AddPlayerPositionRequest position1 = new AddPlayerPositionRequest();
        position1.setPlayerUuid(testAthleteUuid);
        position1.setPositionId(testPositionId);
        position1.setPrioridad(1);

        mockMvc.perform(post("/api/v1/player-profiles/positions")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(position1)))
                .andExpect(status().isCreated());

        // Add second position
        Position position2 = findOrCreatePosition("Mediocampista");

        AddPlayerPositionRequest position2Request = new AddPlayerPositionRequest();
        position2Request.setPlayerUuid(testAthleteUuid);
        position2Request.setPositionId(position2.getId());
        position2Request.setPrioridad(2);

        mockMvc.perform(post("/api/v1/player-profiles/positions")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(position2Request)))
                .andExpect(status().isCreated());

        // When & Then - Get all positions
        mockMvc.perform(get("/api/v1/player-profiles/{atletaUuid}/positions", testAthleteUuid)
                .with(jwtFor(testAthleteUuid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].prioridad").value(1))
                .andExpect(jsonPath("$[1].prioridad").value(2));
    }

    @Test
    void testRemovePlayerPosition_Success() throws Exception {
        // Given - Create profile and add position
        CreatePlayerProfileRequest createRequest = new CreatePlayerProfileRequest();
        createRequest.setAtletaUuid(testAthleteUuid);
        createRequest.setAlias("RemoveTest");

        mockMvc.perform(post("/api/v1/player-profiles")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        AddPlayerPositionRequest positionRequest = new AddPlayerPositionRequest();
        positionRequest.setPlayerUuid(testAthleteUuid);
        positionRequest.setPositionId(testPositionId);
        positionRequest.setPrioridad(1);

        mockMvc.perform(post("/api/v1/player-profiles/positions")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(positionRequest)))
                .andExpect(status().isCreated());

        // When & Then - Remove position
        mockMvc.perform(delete("/api/v1/player-profiles/{atletaUuid}/positions/{positionId}", 
                testAthleteUuid, testPositionId)
                .with(jwtFor(testAthleteUuid)))
                .andExpect(status().isNoContent());

        // Verify position was removed
        mockMvc.perform(get("/api/v1/player-profiles/{atletaUuid}/positions", testAthleteUuid)
                .with(jwtFor(testAthleteUuid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testUpdateTrustScore_Success() throws Exception {
        // Given - Create profile first
        CreatePlayerProfileRequest createRequest = new CreatePlayerProfileRequest();
        createRequest.setAtletaUuid(testAthleteUuid);
        createRequest.setAlias("TrustTest");

        mockMvc.perform(post("/api/v1/player-profiles")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // When - Update trust score
        UpdateTrustScoreRequest trustRequest = new UpdateTrustScoreRequest();
        trustRequest.setPlayerUuid(testAthleteUuid);
        trustRequest.setCambio(10);
        trustRequest.setMotivo("Good behavior");

        // Then
        mockMvc.perform(put("/api/v1/player-profiles/trust-score")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(trustRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustScore").value(110));
    }

    @Test
    void testGetTrustScoreHistory_Success() throws Exception {
        // Given - Create profile and update trust score
        CreatePlayerProfileRequest createRequest = new CreatePlayerProfileRequest();
        createRequest.setAtletaUuid(testAthleteUuid);
        createRequest.setAlias("HistoryTest");

        mockMvc.perform(post("/api/v1/player-profiles")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        UpdateTrustScoreRequest trustRequest = new UpdateTrustScoreRequest();
        trustRequest.setPlayerUuid(testAthleteUuid);
        trustRequest.setCambio(15);
        trustRequest.setMotivo("Excellent performance");

        mockMvc.perform(put("/api/v1/player-profiles/trust-score")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(trustRequest)))
                .andExpect(status().isOk());

        // When & Then - Get trust history
        mockMvc.perform(get("/api/v1/player-profiles/{atletaUuid}/trust-history", testAthleteUuid)
                .with(jwtFor(testAthleteUuid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cambio").value(15))
                .andExpect(jsonPath("$[0].motivo").value("Excellent performance"));
    }

    @Test
    void testTrustScoreHistory_IncludesMatchWhenRequestHasMatchId() throws Exception {
        CreatePlayerProfileRequest createRequest = new CreatePlayerProfileRequest();
        createRequest.setAtletaUuid(testAthleteUuid);
        createRequest.setAlias("TrustMatchTest");

        mockMvc.perform(post("/api/v1/player-profiles")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        PlayerProfile player = playerProfileRepository.findById(testAthleteUuid).orElseThrow();
        Match match = new Match();
        match.setCreador(player);
        match.setModalidad(MatchMode.CINCO_VS_CINCO);
        match.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
        match = matchRepository.save(match);

        UpdateTrustScoreRequest trustRequest = new UpdateTrustScoreRequest();
        trustRequest.setPlayerUuid(testAthleteUuid);
        trustRequest.setCambio(-5);
        trustRequest.setMotivo("Late cancellation");
        trustRequest.setMatchId(match.getId());

        mockMvc.perform(put("/api/v1/player-profiles/trust-score")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(trustRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustScore").value(95));

        mockMvc.perform(get("/api/v1/player-profiles/{atletaUuid}/trust-history", testAthleteUuid)
                .with(jwtFor(testAthleteUuid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cambio").value(-5))
                .andExpect(jsonPath("$[0].match.id").value(match.getId()));
    }

    @Test
    void testGetPlayersByTrustScoreRange_Success() throws Exception {
        // Given - Create multiple profiles with different trust scores
        for (int i = 0; i < 3; i++) {
            Athlete athlete = new Athlete();
            athlete.setEmail("trust" + i + "@example.com");
            athlete.setPasswordHash("hashedpassword");
            athlete.setNombre("Trust Athlete " + i);
            athlete.setGenero(GenderType.MASCULINO);
            athlete = athleteRepository.save(athlete);

            CreatePlayerProfileRequest createRequest = new CreatePlayerProfileRequest();
            createRequest.setAtletaUuid(athlete.getAtletaUuid());
            createRequest.setAlias("TrustPlayer" + i);

            mockMvc.perform(post("/api/v1/player-profiles")
                    .with(jwtFor(athlete.getAtletaUuid()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated());

            // Update trust score
            UpdateTrustScoreRequest trustRequest = new UpdateTrustScoreRequest();
            trustRequest.setPlayerUuid(athlete.getAtletaUuid());
            trustRequest.setCambio(i * 10); // 0, 10, 20
            trustRequest.setMotivo("Test adjustment");

            mockMvc.perform(put("/api/v1/player-profiles/trust-score")
                    .with(jwtFor(athlete.getAtletaUuid()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(trustRequest)))
                    .andExpect(status().isOk());
        }

        // When & Then - Search by trust score range
        mockMvc.perform(get("/api/v1/player-profiles/by-trust-score")
                .param("minScore", "105")
                .param("maxScore", "125"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].alias", hasItems("TrustPlayer1", "TrustPlayer2"))); // Seed data can add more matching profiles
    }

    @Test
    void testSearchPlayersByAthleteName_Success() throws Exception {
        // Given - Create profiles with different athlete names
        Athlete athlete1 = new Athlete();
        athlete1.setEmail("search1@example.com");
        athlete1.setPasswordHash("hashedpassword");
        athlete1.setNombre("John Smith");
        athlete1.setGenero(GenderType.MASCULINO);
        athlete1 = athleteRepository.save(athlete1);

        Athlete athlete2 = new Athlete();
        athlete2.setEmail("search2@example.com");
        athlete2.setPasswordHash("hashedpassword");
        athlete2.setNombre("John Doe");
        athlete2.setGenero(GenderType.MASCULINO);
        athlete2 = athleteRepository.save(athlete2);

        CreatePlayerProfileRequest profile1 = new CreatePlayerProfileRequest();
        profile1.setAtletaUuid(athlete1.getAtletaUuid());
        profile1.setAlias("JohnS");

        CreatePlayerProfileRequest profile2 = new CreatePlayerProfileRequest();
        profile2.setAtletaUuid(athlete2.getAtletaUuid());
        profile2.setAlias("JohnD");

        mockMvc.perform(post("/api/v1/player-profiles")
                .with(jwtFor(athlete1.getAtletaUuid()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profile1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/player-profiles")
                .with(jwtFor(athlete2.getAtletaUuid()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profile2)))
                .andExpect(status().isCreated());

        // When & Then - Search by athlete name
        mockMvc.perform(get("/api/v1/player-profiles/search")
                .param("nombre", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].alias", containsInAnyOrder("JohnS", "JohnD")));
    }

    @Test
    void testAddExperienceToPosition_Success() throws Exception {
        // Given - Create profile and add position
        CreatePlayerProfileRequest createRequest = new CreatePlayerProfileRequest();
        createRequest.setAtletaUuid(testAthleteUuid);
        createRequest.setAlias("XPTest");

        mockMvc.perform(post("/api/v1/player-profiles")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        AddPlayerPositionRequest positionRequest = new AddPlayerPositionRequest();
        positionRequest.setPlayerUuid(testAthleteUuid);
        positionRequest.setPositionId(testPositionId);
        positionRequest.setPrioridad(1);

        mockMvc.perform(post("/api/v1/player-profiles/positions")
                .with(jwtFor(testAthleteUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(positionRequest)))
                .andExpect(status().isCreated());

        // When & Then - Add experience
        mockMvc.perform(put("/api/v1/player-profiles/{atletaUuid}/positions/{positionId}/experience", 
                testAthleteUuid, testPositionId)
                .with(jwtFor(testAthleteUuid))
                .param("xp", "50"))
                .andExpect(status().isOk());

        // Verify XP was added
        mockMvc.perform(get("/api/v1/player-profiles/{atletaUuid}/positions", testAthleteUuid)
                .with(jwtFor(testAthleteUuid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].xp").value(50));
    }

    private Position findOrCreatePosition(String name) {
        return positionRepository.findByNombre(name)
                .orElseGet(() -> {
                    Position position = new Position();
                    position.setNombre(name);
                    return positionRepository.save(position);
                });
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtFor(UUID subject) {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(subject.toString()));
    }
}
