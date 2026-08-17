package com.atleta.demo.controller;

import com.atleta.demo.dto.request.CreateMatchEventRequest;
import com.atleta.demo.dto.request.CreateMatchRequest;
import com.atleta.demo.dto.request.JoinMatchRequest;
import com.atleta.demo.entity.*;
import com.atleta.demo.enums.EventType;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchType;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.repository.*;
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
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.web.context.WebApplicationContext;

import com.atleta.demo.config.TestConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MatchController.
 * Tests complete endpoint functionality with MockMvc.
 * 
 * Requisitos probados:
 * - 6.1: Modalidad del partido (5v5, 6v6, 7v7)
 * - 6.2: Programación con fecha, hora y ubicación (latitud, longitud)
 * - 6.3: Estado inicial 'CREADO'
 * - 6.4: Cuota económica para el partido
 * - 6.5: Cambios de estado (CREADO, INICIADO, FINALIZADO, INVALIDO)
 * - 7.1: Asociación de jugador a equipo específico
 * - 7.2: Especificación de posición para el partido
 * - 7.3: Confirmación del jugador
 * - 7.4: Roles específicos para el partido (JUGADOR, CAPITAN, DT)
 * - 7.5: Actualización de lista de jugadores del partido
 * - 8.1: Registro de goles y asistencias
 * - 8.2: Confirmación de equipos local y visitante para eventos
 * - 8.3: Asociación de asistente opcional para goles
 * - 8.4: Trazabilidad de quién registró cada evento
 * - 8.5: Actualización automática de estadísticas
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestConfig.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
public class MatchControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AthleteRepository athleteRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchInviteRepository matchInviteRepository;

    @Autowired
    private MatchTeamRepository matchTeamRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private UUID testCreatorUuid;
    private UUID testPlayerUuid;
    private Long testTeamId;
    private Long testPositionId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Create test athlete and player profile for creator
        Athlete creator = new Athlete();
        creator.setEmail("creator@example.com");
        creator.setPasswordHash("hashedpassword");
        creator.setNombre("Match Creator");
        creator = athleteRepository.save(creator);
        testCreatorUuid = creator.getAtletaUuid();

        PlayerProfile creatorProfile = new PlayerProfile();
        creatorProfile.setAtletaUuid(testCreatorUuid);
        creatorProfile.setAthlete(creator);
        creatorProfile.setAlias("Creator");
        creatorProfile.setTrustScore(100);
        playerProfileRepository.save(creatorProfile);

        // Create test athlete and player profile for player
        Athlete player = new Athlete();
        player.setEmail("player@example.com");
        player.setPasswordHash("hashedpassword");
        player.setNombre("Test Player");
        player = athleteRepository.save(player);
        testPlayerUuid = player.getAtletaUuid();

        PlayerProfile playerProfile = new PlayerProfile();
        playerProfile.setAtletaUuid(testPlayerUuid);
        playerProfile.setAthlete(player);
        playerProfile.setAlias("Player");
        playerProfile.setTrustScore(100);
        playerProfileRepository.save(playerProfile);

        // Create test team
        Team team = new Team();
        team.setNombre("Test Team");
        team.setCreador(creatorProfile);
        team = teamRepository.save(team);
        testTeamId = team.getId();

        // Create test position
        Position position = positionRepository.findByNombre("Delantero")
                .orElseGet(() -> {
                    Position newPosition = new Position();
                    newPosition.setNombre("Delantero");
                    return positionRepository.save(newPosition);
                });
        testPositionId = position.getId();
    }

    @Test
    void testCreateMatch_Success() throws Exception {
        // Given
        CreateMatchRequest request = new CreateMatchRequest();
        request.setModalidad(MatchMode.CINCO_VS_CINCO);
        request.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
        request.setLatitud(new BigDecimal("40.7128"));
        request.setLongitud(new BigDecimal("-74.0060"));
        request.setCuota(new BigDecimal("25.00"));
        request.setCreadorUuid(testCreatorUuid);

        // When & Then
        mockMvc.perform(post("/api/v1/matches")
                .with(jwtFor(testCreatorUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modalidad").value("CINCO_VS_CINCO"))
                .andExpect(jsonPath("$.matchType").value("FRIENDLY"))
                .andExpect(jsonPath("$.latitud").value(40.7128))
                .andExpect(jsonPath("$.longitud").value(-74.0060))
                .andExpect(jsonPath("$.cuota").value(25.00))
                .andExpect(jsonPath("$.estado").value("CREADO"))
                .andExpect(jsonPath("$.creador.atletaUuid").value(testCreatorUuid.toString()))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void testCreateMatchOrchestrated_IsAtomicAndIdempotent() throws Exception {
        Map<String, Object> payload = orchestratedPayload(List.of(testPlayerUuid));

        String firstResponse = mockMvc.perform(post("/api/v1/matches/orchestrated")
                        .with(jwtFor(testCreatorUuid))
                        .header("Idempotency-Key", "create-match-abc-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.replayed").value(false))
                .andExpect(jsonPath("$.match.matchType").value("POINTS"))
                .andExpect(jsonPath("$.match.matchTeams", hasSize(1)))
                .andExpect(jsonPath("$.invitations", hasSize(1)))
                .andExpect(jsonPath("$.invitations[0].targetUuid").value(testPlayerUuid.toString()))
                .andReturn().getResponse().getContentAsString();

        long matchId = objectMapper.readTree(firstResponse).path("match").path("id").asLong();

        mockMvc.perform(post("/api/v1/matches/orchestrated")
                        .with(jwtFor(testCreatorUuid))
                        .header("Idempotency-Key", "create-match-abc-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.match.id").value(matchId))
                .andExpect(jsonPath("$.invitations", hasSize(1)));

        assertThat(matchRepository.count(), is(1L));
        assertThat(matchTeamRepository.count(), is(1L));
        assertThat(matchInviteRepository.count(), is(1L));
    }

    @Test
    void testCreateMatchOrchestrated_RollsBackEverythingWhenAnInviteFails() throws Exception {
        Map<String, Object> payload = orchestratedPayload(List.of(testPlayerUuid, UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/matches/orchestrated")
                        .with(jwtFor(testCreatorUuid))
                        .header("Idempotency-Key", "create-match-rollback-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        TestTransaction.end();
        TestTransaction.start();

        assertThat(matchRepository.count(), is(0L));
        assertThat(matchTeamRepository.count(), is(0L));
        assertThat(matchInviteRepository.count(), is(0L));
    }

    private Map<String, Object> orchestratedPayload(List<UUID> targetUuids) {
        Map<String, Object> match = Map.of(
                "creadorUuid", testCreatorUuid,
                "modalidad", "CINCO_VS_CINCO",
                "matchType", "POINTS",
                "categoriaGenero", "MIXTO",
                "fechaHoraProgramada", LocalDateTime.now().plusDays(1).toString()
        );
        return Map.of(
                "match", match,
                "teamId", testTeamId,
                "targetUuids", targetUuids,
                "invitationMessage", "Sumate al partido"
        );
    }

    @Test
    void testCreateMatch_PersistsEveryMatchTypeAcrossCreateGetAndList() throws Exception {
        for (MatchType matchType : MatchType.values()) {
            CreateMatchRequest request = new CreateMatchRequest();
            request.setModalidad(MatchMode.CINCO_VS_CINCO);
            request.setMatchType(matchType);
            request.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
            request.setCreadorUuid(testCreatorUuid);

            String response = mockMvc.perform(post("/api/v1/matches")
                            .with(jwtFor(testCreatorUuid))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.matchType").value(matchType.name()))
                    .andReturn().getResponse().getContentAsString();

            long matchId = objectMapper.readTree(response).get("id").asLong();

            mockMvc.perform(get("/api/v1/matches/{matchId}", matchId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matchType").value(matchType.name()));

            mockMvc.perform(get("/api/v1/matches"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == " + matchId + ")].matchType").value(matchType.name()));
        }
    }

    @Test
    void testCreateMatch_CreatorNotFound() throws Exception {
        // Given - Non-existent creator UUID
        UUID missingCreatorUuid = UUID.randomUUID();
        CreateMatchRequest request = new CreateMatchRequest();
        request.setModalidad(MatchMode.CINCO_VS_CINCO);
        request.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
        request.setLatitud(new BigDecimal("40.7128"));
        request.setLongitud(new BigDecimal("-74.0060"));
        request.setCuota(new BigDecimal("25.00"));
        request.setCreadorUuid(missingCreatorUuid);

        // When & Then
        mockMvc.perform(post("/api/v1/matches")
                .with(jwtFor(missingCreatorUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateMatch_InvalidCoordinates() throws Exception {
        // Given - Invalid latitude (out of range)
        CreateMatchRequest request = new CreateMatchRequest();
        request.setModalidad(MatchMode.CINCO_VS_CINCO);
        request.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
        request.setLatitud(new BigDecimal("91.0")); // Invalid latitude > 90
        request.setLongitud(new BigDecimal("-74.0060"));
        request.setCuota(new BigDecimal("25.00"));
        request.setCreadorUuid(testCreatorUuid);

        // When & Then
        mockMvc.perform(post("/api/v1/matches")
                .with(jwtFor(testCreatorUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateMatch_AllModalidades() throws Exception {
        // Test all match modes
        MatchMode[] modes = {MatchMode.CINCO_VS_CINCO, MatchMode.SEIS_VS_SEIS, MatchMode.SIETE_VS_SIETE};
        
        for (MatchMode mode : modes) {
            CreateMatchRequest request = new CreateMatchRequest();
            request.setModalidad(mode);
            request.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
            request.setLatitud(new BigDecimal("40.7128"));
            request.setLongitud(new BigDecimal("-74.0060"));
            request.setCuota(new BigDecimal("25.00"));
            request.setCreadorUuid(testCreatorUuid);

            mockMvc.perform(post("/api/v1/matches")
                    .with(jwtFor(testCreatorUuid))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.modalidad").value(mode.name()));
        }
    }

    @Test
    void testGetMatchById_Success() throws Exception {
        // Given - Create match first
        CreateMatchRequest createRequest = new CreateMatchRequest();
        createRequest.setModalidad(MatchMode.CINCO_VS_CINCO);
        createRequest.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
        createRequest.setLatitud(new BigDecimal("40.7128"));
        createRequest.setLongitud(new BigDecimal("-74.0060"));
        createRequest.setCuota(new BigDecimal("25.00"));
        createRequest.setCreadorUuid(testCreatorUuid);

        String response = mockMvc.perform(post("/api/v1/matches")
                .with(jwtFor(testCreatorUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long matchId = objectMapper.readTree(response).get("id").asLong();

        // When & Then
        mockMvc.perform(get("/api/v1/matches/{matchId}", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(matchId))
                .andExpect(jsonPath("$.modalidad").value("CINCO_VS_CINCO"))
                .andExpect(jsonPath("$.estado").value("CREADO"));
    }

    @Test
    void testGetMatchById_NotFound() throws Exception {
        // Given - Non-existent match ID
        Long nonExistentId = 999L;

        // When & Then
        mockMvc.perform(get("/api/v1/matches/{matchId}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllMatches_Success() throws Exception {
        // Given - Create multiple matches
        for (int i = 0; i < 3; i++) {
            CreateMatchRequest request = new CreateMatchRequest();
            request.setModalidad(MatchMode.CINCO_VS_CINCO);
            request.setFechaHoraProgramada(LocalDateTime.now().plusDays(i + 1));
            request.setLatitud(new BigDecimal("40.7128"));
            request.setLongitud(new BigDecimal("-74.0060"));
            request.setCuota(new BigDecimal("25.00"));
            request.setCreadorUuid(testCreatorUuid);

            mockMvc.perform(post("/api/v1/matches")
                    .with(jwtFor(testCreatorUuid))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // When & Then
        mockMvc.perform(get("/api/v1/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void testGetUpcomingMatches_Success() throws Exception {
        // Given - Create matches with different dates
        CreateMatchRequest pastMatch = new CreateMatchRequest();
        pastMatch.setModalidad(MatchMode.CINCO_VS_CINCO);
        pastMatch.setFechaHoraProgramada(LocalDateTime.now().minusDays(1)); // Past
        pastMatch.setLatitud(new BigDecimal("40.7128"));
        pastMatch.setLongitud(new BigDecimal("-74.0060"));
        pastMatch.setCuota(new BigDecimal("25.00"));
        pastMatch.setCreadorUuid(testCreatorUuid);

        CreateMatchRequest futureMatch = new CreateMatchRequest();
        futureMatch.setModalidad(MatchMode.CINCO_VS_CINCO);
        futureMatch.setFechaHoraProgramada(LocalDateTime.now().plusDays(1)); // Future
        futureMatch.setLatitud(new BigDecimal("40.7128"));
        futureMatch.setLongitud(new BigDecimal("-74.0060"));
        futureMatch.setCuota(new BigDecimal("25.00"));
        futureMatch.setCreadorUuid(testCreatorUuid);

        mockMvc.perform(post("/api/v1/matches")
                .with(jwtFor(testCreatorUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pastMatch)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/matches")
                .with(jwtFor(testCreatorUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(futureMatch)))
                .andExpect(status().isCreated());

        // When & Then - Should only return future matches
        mockMvc.perform(get("/api/v1/matches/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void testChangeMatchStatus_Success() throws Exception {
        // Given - Create match first
        CreateMatchRequest createRequest = new CreateMatchRequest();
        createRequest.setModalidad(MatchMode.CINCO_VS_CINCO);
        createRequest.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
        createRequest.setLatitud(new BigDecimal("40.7128"));
        createRequest.setLongitud(new BigDecimal("-74.0060"));
        createRequest.setCuota(new BigDecimal("25.00"));
        createRequest.setCreadorUuid(testCreatorUuid);

        String response = mockMvc.perform(post("/api/v1/matches")
                .with(jwtFor(testCreatorUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long matchId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/v1/matches/{matchId}/teams/{teamId}", matchId, testTeamId)
                .with(jwtFor(testCreatorUuid))
                .param("esLocal", "true"))
                .andExpect(status().isOk());

        // When & Then - Change status to INICIADO
        mockMvc.perform(put("/api/v1/matches/{matchId}/status", matchId)
                .with(jwtFor(testCreatorUuid))
                .param("status", "INICIADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INICIADO"))
                .andExpect(jsonPath("$.startedAt").exists());
    }

    @Test
    void testAddTeamToMatch_Success() throws Exception {
        // Given - Create match first
        CreateMatchRequest createRequest = new CreateMatchRequest();
        createRequest.setModalidad(MatchMode.CINCO_VS_CINCO);
        createRequest.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
        createRequest.setLatitud(new BigDecimal("40.7128"));
        createRequest.setLongitud(new BigDecimal("-74.0060"));
        createRequest.setCuota(new BigDecimal("25.00"));
        createRequest.setCreadorUuid(testCreatorUuid);

        String response = mockMvc.perform(post("/api/v1/matches")
                .with(jwtFor(testCreatorUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long matchId = objectMapper.readTree(response).get("id").asLong();

        // When & Then - Add team as local
        mockMvc.perform(post("/api/v1/matches/{matchId}/teams/{teamId}", matchId, testTeamId)
                .with(jwtFor(testCreatorUuid))
                .param("esLocal", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchTeams", hasSize(1)))
                .andExpect(jsonPath("$.matchTeams[0].esLocal").value(true))
                .andExpect(jsonPath("$.matchTeams[0].team.id").value(testTeamId));
    }

    @Test
    void testJoinMatch_Success() throws Exception {
        // Given - Create match and add team first
        CreateMatchRequest createRequest = new CreateMatchRequest();
        createRequest.setModalidad(MatchMode.CINCO_VS_CINCO);
        createRequest.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
        createRequest.setLatitud(new BigDecimal("40.7128"));
        createRequest.setLongitud(new BigDecimal("-74.0060"));
        createRequest.setCuota(new BigDecimal("25.00"));
        createRequest.setCreadorUuid(testCreatorUuid);

        String response = mockMvc.perform(post("/api/v1/matches")
                .with(jwtFor(testCreatorUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long matchId = objectMapper.readTree(response).get("id").asLong();

        // Add team to match
        mockMvc.perform(post("/api/v1/matches/{matchId}/teams/{teamId}", matchId, testTeamId)
                .with(jwtFor(testCreatorUuid))
                .param("esLocal", "true"))
                .andExpect(status().isOk());

        // When - Player joins match
        JoinMatchRequest joinRequest = new JoinMatchRequest();
        joinRequest.setMatchId(matchId);
        joinRequest.setPlayerUuid(testPlayerUuid);
        joinRequest.setTeamId(testTeamId);
        joinRequest.setPositionId(testPositionId);
        joinRequest.setRol(PlayerRole.JUGADOR);

        // Then
        mockMvc.perform(post("/api/v1/matches/join")
                .with(jwtFor(testPlayerUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.confirmado").value(false))
                .andExpect(jsonPath("$.rol").value("JUGADOR"))
                .andExpect(jsonPath("$.player.atletaUuid").value(testPlayerUuid.toString()))
                .andExpect(jsonPath("$.team.id").value(testTeamId))
                .andExpect(jsonPath("$.position.id").value(testPositionId));
    }

    @Test
    void testConfirmParticipation_Success() throws Exception {
        // Given - Create match, add team, and join match
        CreateMatchRequest createRequest = new CreateMatchRequest();
        createRequest.setModalidad(MatchMode.CINCO_VS_CINCO);
        createRequest.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
        createRequest.setLatitud(new BigDecimal("40.7128"));
        createRequest.setLongitud(new BigDecimal("-74.0060"));
        createRequest.setCuota(new BigDecimal("25.00"));
        createRequest.setCreadorUuid(testCreatorUuid);

        String response = mockMvc.perform(post("/api/v1/matches")
                .with(jwtFor(testCreatorUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long matchId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/v1/matches/{matchId}/teams/{teamId}", matchId, testTeamId)
                .with(jwtFor(testCreatorUuid))
                .param("esLocal", "true"))
                .andExpect(status().isOk());

        JoinMatchRequest joinRequest = new JoinMatchRequest();
        joinRequest.setMatchId(matchId);
        joinRequest.setPlayerUuid(testPlayerUuid);
        joinRequest.setTeamId(testTeamId);
        joinRequest.setPositionId(testPositionId);
        joinRequest.setRol(PlayerRole.JUGADOR);

        mockMvc.perform(post("/api/v1/matches/join")
                .with(jwtFor(testPlayerUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isCreated());

        // When & Then - Confirm participation
        mockMvc.perform(put("/api/v1/matches/{matchId}/players/{playerUuid}/confirm", 
                matchId, testPlayerUuid)
                .with(jwtFor(testPlayerUuid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmado").value(true));
    }

    @Test
    void testRegisterEvent_Success() throws Exception {
        // Given - Create match, add team, join match, and confirm participation
        CreateMatchRequest createRequest = new CreateMatchRequest();
        createRequest.setModalidad(MatchMode.CINCO_VS_CINCO);
        createRequest.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
        createRequest.setLatitud(new BigDecimal("40.7128"));
        createRequest.setLongitud(new BigDecimal("-74.0060"));
        createRequest.setCuota(new BigDecimal("25.00"));
        createRequest.setCreadorUuid(testCreatorUuid);

        String response = mockMvc.perform(post("/api/v1/matches")
                .with(jwtFor(testCreatorUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long matchId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/v1/matches/{matchId}/teams/{teamId}", matchId, testTeamId)
                .with(jwtFor(testCreatorUuid))
                .param("esLocal", "true"))
                .andExpect(status().isOk());

        JoinMatchRequest joinRequest = new JoinMatchRequest();
        joinRequest.setMatchId(matchId);
        joinRequest.setPlayerUuid(testPlayerUuid);
        joinRequest.setTeamId(testTeamId);
        joinRequest.setPositionId(testPositionId);
        joinRequest.setRol(PlayerRole.JUGADOR);

        mockMvc.perform(post("/api/v1/matches/join")
                .with(jwtFor(testPlayerUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/matches/{matchId}/players/{playerUuid}/confirm", 
                matchId, testPlayerUuid)
                .with(jwtFor(testPlayerUuid)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/matches/{matchId}/status", matchId)
                .with(jwtFor(testCreatorUuid))
                .param("status", "INICIADO"))
                .andExpect(status().isOk());

        // When - Register goal event
        CreateMatchEventRequest eventRequest = new CreateMatchEventRequest();
        eventRequest.setMatchId(matchId);
        eventRequest.setPlayerUuid(testPlayerUuid);
        eventRequest.setTeamId(testTeamId);
        eventRequest.setEventType(EventType.GOL);
        eventRequest.setRegisteredByUuid(testCreatorUuid);

        // Then
        mockMvc.perform(post("/api/v1/matches/events")
                .with(jwtFor(testCreatorUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventType").value("GOL"))
                .andExpect(jsonPath("$.player.atletaUuid").value(testPlayerUuid.toString()))
                .andExpect(jsonPath("$.confirmedByLocal").value(true))
                .andExpect(jsonPath("$.confirmedByVisitante").value(true));
    }

    @Test
    void testGetMatchEvents_Success() throws Exception {
        // Given - Create match with events (simplified setup)
        CreateMatchRequest createRequest = new CreateMatchRequest();
        createRequest.setModalidad(MatchMode.CINCO_VS_CINCO);
        createRequest.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
        createRequest.setLatitud(new BigDecimal("40.7128"));
        createRequest.setLongitud(new BigDecimal("-74.0060"));
        createRequest.setCuota(new BigDecimal("25.00"));
        createRequest.setCreadorUuid(testCreatorUuid);

        String response = mockMvc.perform(post("/api/v1/matches")
                .with(jwtFor(testCreatorUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long matchId = objectMapper.readTree(response).get("id").asLong();

        // When & Then - Get events (should be empty initially)
        mockMvc.perform(get("/api/v1/matches/{matchId}/events", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testGetMatchesByPlayer_Success() throws Exception {
        // Given - Create match and have player join
        CreateMatchRequest createRequest = new CreateMatchRequest();
        createRequest.setModalidad(MatchMode.CINCO_VS_CINCO);
        createRequest.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
        createRequest.setLatitud(new BigDecimal("40.7128"));
        createRequest.setLongitud(new BigDecimal("-74.0060"));
        createRequest.setCuota(new BigDecimal("25.00"));
        createRequest.setCreadorUuid(testCreatorUuid);

        String response = mockMvc.perform(post("/api/v1/matches")
                .with(jwtFor(testCreatorUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long matchId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/v1/matches/{matchId}/teams/{teamId}", matchId, testTeamId)
                .with(jwtFor(testCreatorUuid))
                .param("esLocal", "true"))
                .andExpect(status().isOk());

        JoinMatchRequest joinRequest = new JoinMatchRequest();
        joinRequest.setMatchId(matchId);
        joinRequest.setPlayerUuid(testPlayerUuid);
        joinRequest.setTeamId(testTeamId);
        joinRequest.setPositionId(testPositionId);
        joinRequest.setRol(PlayerRole.JUGADOR);

        mockMvc.perform(post("/api/v1/matches/join")
                .with(jwtFor(testPlayerUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isCreated());

        // When & Then - Get matches by player
        mockMvc.perform(get("/api/v1/matches/by-player/{playerUuid}", testPlayerUuid)
                .with(jwtFor(testPlayerUuid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(matchId));
    }

    @Test
    void testGetMatchesByTeam_Success() throws Exception {
        // Given - Create match and add team
        CreateMatchRequest createRequest = new CreateMatchRequest();
        createRequest.setModalidad(MatchMode.CINCO_VS_CINCO);
        createRequest.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
        createRequest.setLatitud(new BigDecimal("40.7128"));
        createRequest.setLongitud(new BigDecimal("-74.0060"));
        createRequest.setCuota(new BigDecimal("25.00"));
        createRequest.setCreadorUuid(testCreatorUuid);

        String response = mockMvc.perform(post("/api/v1/matches")
                .with(jwtFor(testCreatorUuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long matchId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/v1/matches/{matchId}/teams/{teamId}", matchId, testTeamId)
                .with(jwtFor(testCreatorUuid))
                .param("esLocal", "true"))
                .andExpect(status().isOk());

        // When & Then - Get matches by team
        mockMvc.perform(get("/api/v1/matches/by-team/{teamId}", testTeamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(matchId));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtFor(UUID subject) {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(subject.toString()));
    }
}
