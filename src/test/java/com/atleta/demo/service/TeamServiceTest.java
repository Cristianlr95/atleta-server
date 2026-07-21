package com.atleta.demo.service;

import com.atleta.demo.dto.request.CreateTeamRequest;
import com.atleta.demo.dto.response.TeamResponse;
import com.atleta.demo.entity.*;
import com.atleta.demo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para TeamService.
 * Valida la lógica de negocio para gestión de equipos, membresías y estadísticas.
 */
@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamStatsRepository teamStatsRepository;

    @Mock
    private PlayerProfileRepository playerProfileRepository;

    @Mock
    private PlayerPositionRepository playerPositionRepository;

    @Mock
    private TeamInviteRepository teamInviteRepository;

    @Mock
    private MatchInviteRepository matchInviteRepository;

    private TeamService teamService;

    private CreateTeamRequest validCreateRequest;
    private PlayerProfile samplePlayer;
    private Team sampleTeam;
    private TeamStats sampleStats;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(
                teamRepository,
                playerProfileRepository,
                teamMemberRepository,
                teamStatsRepository,
                playerPositionRepository,
                teamInviteRepository,
                matchInviteRepository
        );

        UUID playerId = UUID.randomUUID();
        
        samplePlayer = new PlayerProfile();
        samplePlayer.setAtletaUuid(playerId);
        samplePlayer.setAlias("TestPlayer");
        samplePlayer.setTrustScore(100);

        validCreateRequest = new CreateTeamRequest();
        validCreateRequest.setNombre("Test Team");
        validCreateRequest.setCreadorUuid(playerId);
        validCreateRequest.setLogoUrl("http://example.com/logo.png");
        validCreateRequest.setAnioFundacion(2024);

        sampleTeam = new Team();
        sampleTeam.setId(1L);
        sampleTeam.setNombre("Test Team");
        sampleTeam.setCreador(samplePlayer);
        sampleTeam.setLogoUrl("http://example.com/logo.png");
        sampleTeam.setAnioFundacion(2024);

        sampleStats = new TeamStats();
        sampleStats.setId(1L);
        sampleStats.setTeam(sampleTeam);

    }

    @Test
    void createTeam_ValidRequest_ShouldCreateTeam() {
        // Arrange
        when(teamRepository.existsByNombre(validCreateRequest.getNombre())).thenReturn(false);
        when(playerProfileRepository.findById(validCreateRequest.getCreadorUuid())).thenReturn(Optional.of(samplePlayer));
        when(teamRepository.saveAndFlush(any(Team.class))).thenReturn(sampleTeam);
        when(teamStatsRepository.save(any(TeamStats.class))).thenReturn(sampleStats);
        when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(new TeamMember());

        // Act
        TeamResponse response = teamService.createTeam(validCreateRequest);

        // Assert
        assertNotNull(response);
        assertEquals(sampleTeam.getNombre(), response.getNombre());
        assertEquals(sampleTeam.getLogoUrl(), response.getLogoUrl());
        assertEquals(sampleTeam.getAnioFundacion(), response.getAnioFundacion());

        verify(teamRepository).existsByNombre(validCreateRequest.getNombre());
        verify(playerProfileRepository).findById(validCreateRequest.getCreadorUuid());
        verify(teamRepository).saveAndFlush(any(Team.class));
        verify(teamStatsRepository).save(any(TeamStats.class));
        verify(teamMemberRepository).save(any(TeamMember.class));
    }

    @Test
    void createTeam_DuplicateName_ShouldThrowException() {
        // Arrange
        when(teamRepository.existsByNombre(validCreateRequest.getNombre())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teamService.createTeam(validCreateRequest)
        );

        assertEquals("Ya existe un equipo con ese nombre", exception.getMessage());
        verify(teamRepository).existsByNombre(validCreateRequest.getNombre());
        verify(playerProfileRepository, never()).findById(any());
        verify(teamRepository, never()).saveAndFlush(any(Team.class));
    }

    @Test
    void createTeam_CreatorNotFound_ShouldThrowException() {
        // Arrange
        when(teamRepository.existsByNombre(validCreateRequest.getNombre())).thenReturn(false);
        when(playerProfileRepository.findById(validCreateRequest.getCreadorUuid())).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teamService.createTeam(validCreateRequest)
        );

        assertEquals("Creador no encontrado: " + validCreateRequest.getCreadorUuid(), exception.getMessage());
        verify(teamRepository).existsByNombre(validCreateRequest.getNombre());
        verify(playerProfileRepository).findById(validCreateRequest.getCreadorUuid());
        verify(teamRepository, never()).saveAndFlush(any(Team.class));
    }

    @Test
    void getTeamById_ActiveTeam_ShouldReturnPublicDetail() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(sampleTeam));

        TeamResponse response = teamService.getTeamById(1L);

        assertEquals(1L, response.getId());
        assertEquals("Test Team", response.getNombre());
        assertEquals(samplePlayer.getAtletaUuid(), response.getCreador().getAtletaUuid());
    }

    @Test
    void getTeamById_ArchivedTeam_ShouldBehaveAsMissing() {
        sampleTeam.setArchived(true);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(sampleTeam));

        assertThrows(IllegalArgumentException.class, () -> teamService.getTeamById(1L));
    }

    @Test
    void storeTeamLogo_ValidPng_ShouldStoreFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                new byte[] {
                        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                        0x00, 0x00, 0x00, 0x0D
                }
        );

        String storedPath = teamService.storeTeamLogo(file);

        assertTrue(storedPath.startsWith("/uploads/team-logos/"));
        assertTrue(storedPath.endsWith(".png"));
        Path created = Path.of(storedPath.substring(1));
        assertTrue(Files.exists(created));
        Files.deleteIfExists(created);
    }

    @Test
    void storeTeamLogo_SpoofedContentType_ShouldThrowException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                "not-an-image".getBytes()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teamService.storeTeamLogo(file)
        );

        assertEquals("El contenido del logo no coincide con una imagen valida", exception.getMessage());
    }

}
