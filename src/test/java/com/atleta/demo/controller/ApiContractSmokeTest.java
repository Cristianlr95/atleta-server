package com.atleta.demo.controller;

import com.atleta.demo.dto.response.AthleteResponse;
import com.atleta.demo.dto.response.LeaderboardEntryResponse;
import com.atleta.demo.dto.response.MatchClosePreviewResponse;
import com.atleta.demo.dto.response.MatchMvpResponse;
import com.atleta.demo.dto.response.MatchResponse;
import com.atleta.demo.dto.response.PlayerProfileResponse;
import com.atleta.demo.dto.response.TeamActiveMemberResponse;
import com.atleta.demo.dto.response.TeamResponse;
import com.atleta.demo.config.TestConfig;
import com.atleta.demo.entity.Athlete;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.PlayerRating;
import com.atleta.demo.entity.RatingHistory;
import com.atleta.demo.enums.GenderType;
import com.atleta.demo.enums.MatchGenderCategory;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchResultType;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;
import com.atleta.demo.repository.PlayerProfileRepository;
import com.atleta.demo.service.AthleteService;
import com.atleta.demo.service.GoogleAuthService;
import com.atleta.demo.service.JwtService;
import com.atleta.demo.service.MatchLiveEventService;
import com.atleta.demo.service.MatchMvpService;
import com.atleta.demo.service.MatchService;
import com.atleta.demo.service.RatingService;
import com.atleta.demo.service.TeamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AthleteController.class,
        TeamController.class,
        MatchController.class,
        RatingController.class
})
@AutoConfigureMockMvc
@Import(TestConfig.class)
class ApiContractSmokeTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AthleteService athleteService;

    @MockBean
    private GoogleAuthService googleAuthService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TeamService teamService;

    @MockBean
    private MatchService matchService;

    @MockBean
    private MatchLiveEventService matchLiveEventService;

    @MockBean
    private MatchMvpService matchMvpService;

    @MockBean
    private RatingService ratingService;

    @MockBean
    private PlayerProfileRepository playerProfileRepository;

    @Test
    void authRoutesKeepFrontendContract() throws Exception {
        AthleteResponse registered = new AthleteResponse(
                USER_ID,
                "demo@atleta.test",
                "Jugador Demo",
                GenderType.MASCULINO,
                LocalDateTime.now()
        );
        Athlete athlete = athlete(USER_ID, "demo@atleta.test", "Jugador Demo");

        when(athleteService.registerAthlete(any())).thenReturn(registered);
        when(athleteService.authenticateEntity("demo@atleta.test", "secret123"))
                .thenReturn(Optional.of(athlete));
        when(googleAuthService.authenticateWithGoogle("google-token")).thenReturn(athlete);
        when(jwtService.generateToken(athlete)).thenReturn("jwt-token");

        mockMvc.perform(post("/api/v1/athletes/register")
                        .with(jwtFor(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "demo@atleta.test",
                                "password", "secret123",
                                "nombre", "Jugador Demo",
                                "genero", "MASCULINO"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.atletaUuid").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("demo@atleta.test"))
                .andExpect(jsonPath("$.genero").value("MASCULINO"));

        mockMvc.perform(post("/api/v1/athletes/login")
                        .with(jwtFor(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "demo@atleta.test",
                                "password", "secret123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atletaUuid").value(USER_ID.toString()))
                .andExpect(jsonPath("$.accessToken").value("jwt-token"));

        mockMvc.perform(post("/api/v1/athletes/auth/google")
                        .with(jwtFor(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idToken", "google-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atletaUuid").value(USER_ID.toString()))
                .andExpect(jsonPath("$.accessToken").value("jwt-token"));
    }

    @Test
    void teamRoutesKeepFrontendContract() throws Exception {
        TeamResponse team = teamResponse(77L, "Atleta FC");
        TeamActiveMemberResponse member = new TeamActiveMemberResponse(
                USER_ID,
                "Demo10",
                PlayerRole.CAPITAN,
                9L,
                "Delantero"
        );

        when(teamService.createTeam(any())).thenReturn(team);
        when(teamService.getTeamsByCreator(USER_ID)).thenReturn(List.of(team));
        when(teamService.getTeamsByPlayer(USER_ID)).thenReturn(List.of(team));
        when(teamService.getActiveMembersByTeam(77L)).thenReturn(List.of(member));

        mockMvc.perform(post("/api/v1/teams")
                        .with(jwtFor(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombre", "Atleta FC",
                                "creadorUuid", USER_ID.toString()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(77))
                .andExpect(jsonPath("$.nombre").value("Atleta FC"))
                .andExpect(jsonPath("$.creador.atletaUuid").value(USER_ID.toString()));

        mockMvc.perform(get("/api/v1/teams/by-creator/{creatorUuid}", USER_ID)
                        .with(jwtFor(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(77));

        mockMvc.perform(get("/api/v1/teams/by-player/{playerUuid}", USER_ID)
                        .with(jwtFor(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Atleta FC"));

        mockMvc.perform(get("/api/v1/teams/{teamId}/members/active", 77L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playerUuid").value(USER_ID.toString()))
                .andExpect(jsonPath("$[0].primaryPositionName").value("Delantero"));

        mockMvc.perform(delete("/api/v1/teams/{teamId}", 77L)
                        .with(jwtFor(USER_ID))
                        .param("actorUuid", USER_ID.toString()))
                .andExpect(status().isNoContent());

        verify(teamService).deleteTeam(77L, USER_ID);
    }

    @Test
    void matchAndMvpRoutesKeepFrontendContract() throws Exception {
        MatchResponse match = matchResponse(42L);
        MatchClosePreviewResponse preview = new MatchClosePreviewResponse();
        preview.setMatchId(42L);
        preview.setFinalScoreLocal(2);
        preview.setFinalScoreAway(1);

        MatchMvpResponse mvp = new MatchMvpResponse();
        mvp.setMatchId(42L);
        mvp.setOpen(true);

        when(matchService.createMatch(any())).thenReturn(match);
        when(matchService.getMatchById(42L)).thenReturn(match);
        when(matchService.getMatchesByPlayer(USER_ID)).thenReturn(List.of(match));
        when(matchService.getMatchesByPlayerOrCreator(USER_ID)).thenReturn(List.of(match));
        when(matchService.changeMatchStatus(42L, MatchStatus.FINALIZADO, USER_ID)).thenReturn(match);
        when(matchService.updateTeamAssignments(eq(42L), eq(USER_ID), any(), any())).thenReturn(match);
        when(matchService.getClosePreview(eq(42L), any())).thenReturn(preview);
        when(matchMvpService.getMvpState(42L, USER_ID)).thenReturn(mvp);
        when(matchMvpService.vote(42L, USER_ID, OTHER_USER_ID)).thenReturn(mvp);

        mockMvc.perform(post("/api/v1/matches")
                        .with(jwtFor(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "creadorUuid", USER_ID.toString(),
                                "modalidad", "CINCO_VS_CINCO",
                                "categoriaGenero", "MIXTO",
                                "fechaHoraProgramada", "2026-05-07T20:00:00"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.modalidad").value("CINCO_VS_CINCO"))
                .andExpect(jsonPath("$.categoriaGenero").value("MIXTO"));

        mockMvc.perform(get("/api/v1/matches/{matchId}", 42L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));

        mockMvc.perform(get("/api/v1/matches/by-player/{playerUuid}", USER_ID)
                        .with(jwtFor(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42));

        mockMvc.perform(get("/api/v1/matches/by-player-or-creator/{playerUuid}", USER_ID)
                        .with(jwtFor(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42));

        mockMvc.perform(put("/api/v1/matches/{matchId}/status", 42L)
                        .with(jwtFor(USER_ID))
                        .param("status", "FINALIZADO")
                        .param("actorUuid", USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CREADO"));

        mockMvc.perform(put("/api/v1/matches/{matchId}/teams/assignment", 42L)
                        .with(jwtFor(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "actorUuid", USER_ID.toString(),
                                "homePlayerUuids", List.of(USER_ID.toString()),
                                "awayPlayerUuids", List.of(OTHER_USER_ID.toString())
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));

        mockMvc.perform(post("/api/v1/matches/{matchId}/close/preview", 42L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "finalScoreLocal", 2,
                                "finalScoreAway", 1
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(42))
                .andExpect(jsonPath("$.finalScoreLocal").value(2));

        mockMvc.perform(get("/api/v1/matches/{matchId}/mvp", 42L)
                        .with(jwtFor(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(42))
                .andExpect(jsonPath("$.open").value(true));

        mockMvc.perform(post("/api/v1/matches/{matchId}/mvp/vote", 42L)
                        .with(jwtFor(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("votedUserId", OTHER_USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(42));
    }

    @Test
    void ratingsRoutesKeepFrontendContract() throws Exception {
        PlayerProfile profile = playerProfile(USER_ID, "Demo10");
        PlayerRating rating = playerRating(profile);
        RatingHistory history = ratingHistory(rating);
        RatingService.PlayerOverallStats stats = overallStats(USER_ID);
        LeaderboardEntryResponse leaderboardEntry = leaderboardEntry(USER_ID);

        when(ratingService.getPlayerRatings(USER_ID)).thenReturn(List.of(rating));
        when(ratingService.getRatingHistory(USER_ID)).thenReturn(List.of(history));
        when(ratingService.initializeBaseRatings(USER_ID)).thenReturn(List.of(rating));
        when(ratingService.calculateCompleteOverall(USER_ID)).thenReturn(stats);
        when(ratingService.getLeaderboard(RoleType.ATAQUE, null)).thenReturn(List.of(leaderboardEntry));
        when(playerProfileRepository.findById(USER_ID)).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/api/v1/ratings/player/{playerProfileId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playerProfileId").value(USER_ID.toString()))
                .andExpect(jsonPath("$[0].roleType").value("ATAQUE"));

        mockMvc.perform(get("/api/v1/ratings/player/{playerProfileId}/history", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playerProfileId").value(USER_ID.toString()))
                .andExpect(jsonPath("$[0].matchResult").value("GANADO"));

        mockMvc.perform(post("/api/v1/ratings/player/{playerProfileId}/initialize-base", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currentRating").value(72));

        mockMvc.perform(get("/api/v1/ratings/player/{playerProfileId}/overall", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerProfileId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.alias").value("Demo10"))
                .andExpect(jsonPath("$.hybridOVR").value(72));

        mockMvc.perform(get("/api/v1/ratings/leaderboard")
                        .param("roleType", "ATAQUE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playerProfileId").value(USER_ID.toString()))
                .andExpect(jsonPath("$[0].alias").value("Demo10"));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtFor(UUID subject) {
        return jwt().jwt(jwt -> jwt.subject(subject.toString()));
    }

    private Athlete athlete(UUID uuid, String email, String nombre) {
        Athlete athlete = new Athlete();
        athlete.setAtletaUuid(uuid);
        athlete.setEmail(email);
        athlete.setNombre(nombre);
        athlete.setGenero(GenderType.MASCULINO);
        return athlete;
    }

    private PlayerProfileResponse profileResponse(UUID uuid, String alias) {
        return new PlayerProfileResponse(uuid, alias, GenderType.MASCULINO, 100, LocalDateTime.now());
    }

    private TeamResponse teamResponse(Long id, String nombre) {
        return new TeamResponse(id, nombre, null, null, profileResponse(USER_ID, "Demo10"), LocalDateTime.now());
    }

    private MatchResponse matchResponse(Long id) {
        MatchResponse response = new MatchResponse();
        response.setId(id);
        response.setModalidad(MatchMode.CINCO_VS_CINCO);
        response.setCategoriaGenero(MatchGenderCategory.MIXTO);
        response.setFechaHoraProgramada(LocalDateTime.of(2026, 5, 7, 20, 0));
        response.setCreador(profileResponse(USER_ID, "Demo10"));
        response.setEstado(MatchStatus.CREADO);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    private PlayerProfile playerProfile(UUID uuid, String alias) {
        PlayerProfile profile = new PlayerProfile();
        profile.setAtletaUuid(uuid);
        profile.setAlias(alias);
        return profile;
    }

    private PlayerRating playerRating(PlayerProfile profile) {
        PlayerRating rating = new PlayerRating(profile, RoleType.ATAQUE, PriorityLevel.PRINCIPAL, BigDecimal.valueOf(72));
        rating.setId(10L);
        rating.setMatchesPlayed(5);
        return rating;
    }

    private RatingHistory ratingHistory(PlayerRating rating) {
        Match match = new Match();
        match.setId(42L);
        RatingHistory history = new RatingHistory(
                rating,
                match,
                BigDecimal.valueOf(70),
                BigDecimal.valueOf(72),
                BigDecimal.valueOf(2),
                2,
                1,
                true,
                MatchResultType.GANADO
        );
        history.setId(99L);
        return history;
    }

    private RatingService.PlayerOverallStats overallStats(UUID playerProfileId) {
        RatingService.PlayerOverallStats stats = new RatingService.PlayerOverallStats();
        stats.setPlayerProfileId(playerProfileId);
        stats.setHybridOVR(BigDecimal.valueOf(72));
        stats.setWeightedOVR(BigDecimal.valueOf(71));
        stats.setSimpleOVR(BigDecimal.valueOf(70));
        stats.setClassification("INTERMEDIO");
        stats.setVersatilityIndex(BigDecimal.valueOf(0.65));
        stats.setConsistencyScore(BigDecimal.valueOf(0.8));
        stats.setBestRole(RoleType.ATAQUE);
        stats.setBestRoleRating(BigDecimal.valueOf(72));
        stats.setTotalRatings(1);
        stats.setTotalMatchesPlayed(5);
        return stats;
    }

    private LeaderboardEntryResponse leaderboardEntry(UUID playerProfileId) {
        LeaderboardEntryResponse entry = new LeaderboardEntryResponse();
        entry.setPlayerProfileId(playerProfileId);
        entry.setPlayerId(playerProfileId.toString());
        entry.setAlias("Demo10");
        entry.setName("Jugador Demo");
        entry.setScore(BigDecimal.valueOf(72));
        entry.setRating(BigDecimal.valueOf(72));
        entry.setRoleType(RoleType.ATAQUE);
        entry.setMatchesPlayed(5);
        return entry;
    }
}
