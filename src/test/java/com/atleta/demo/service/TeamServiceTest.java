package com.atleta.demo.service;

import com.atleta.demo.dto.request.CreateTeamRequest;
import com.atleta.demo.dto.request.JoinTeamRequest;
import com.atleta.demo.dto.response.TeamResponse;
import com.atleta.demo.entity.*;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private JoinTeamRequest validJoinRequest;
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

        validJoinRequest = new JoinTeamRequest();
        validJoinRequest.setPlayerUuid(playerId);
        validJoinRequest.setTeamId(1L);
        validJoinRequest.setRol(PlayerRole.JUGADOR);
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

    // TODO: Uncomment these tests when the corresponding methods are implemented in TeamService
    /*
    @Test
    void joinTeam_ValidRequest_ShouldAddMember() {
        // Arrange
        when(teamRepository.findById(validJoinRequest.getTeamId())).thenReturn(Optional.of(sampleTeam));
        when(playerProfileRepository.findById(validJoinRequest.getPlayerUuid())).thenReturn(Optional.of(samplePlayer));
        when(teamMemberRepository.findActiveByTeamAndPlayer(sampleTeam, samplePlayer)).thenReturn(Optional.empty());
        
        TeamMember newMember = new TeamMember(sampleTeam, samplePlayer, validJoinRequest.getRol());
        newMember.setId(1L);
        when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(newMember);

        // Act
        TeamMemberResponse response = teamService.joinTeam(validJoinRequest);

        // Assert
        assertNotNull(response);
        assertEquals(validJoinRequest.getRol(), response.getRol());
        assertTrue(response.getActivo());

        verify(teamRepository).findById(validJoinRequest.getTeamId());
        verify(playerProfileRepository).findById(validJoinRequest.getPlayerUuid());
        verify(teamMemberRepository).findActiveByTeamAndPlayer(sampleTeam, samplePlayer);
        verify(teamMemberRepository).save(any(TeamMember.class));
    }

    @Test
    void joinTeam_TeamNotFound_ShouldThrowException() {
        // Arrange
        when(teamRepository.findById(validJoinRequest.getTeamId())).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teamService.joinTeam(validJoinRequest)
        );

        assertEquals("Equipo no encontrado: " + validJoinRequest.getTeamId(), exception.getMessage());
        verify(teamRepository).findById(validJoinRequest.getTeamId());
        verify(playerProfileRepository, never()).findById(any());
    }

    @Test
    void joinTeam_PlayerNotFound_ShouldThrowException() {
        // Arrange
        when(teamRepository.findById(validJoinRequest.getTeamId())).thenReturn(Optional.of(sampleTeam));
        when(playerProfileRepository.findById(validJoinRequest.getPlayerUuid())).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teamService.joinTeam(validJoinRequest)
        );

        assertEquals("Jugador no encontrado: " + validJoinRequest.getPlayerUuid(), exception.getMessage());
        verify(teamRepository).findById(validJoinRequest.getTeamId());
        verify(playerProfileRepository).findById(validJoinRequest.getPlayerUuid());
    }

    @Test
    void joinTeam_AlreadyActiveMember_ShouldThrowException() {
        // Arrange
        TeamMember existingMember = new TeamMember(sampleTeam, samplePlayer);
        when(teamRepository.findById(validJoinRequest.getTeamId())).thenReturn(Optional.of(sampleTeam));
        when(playerProfileRepository.findById(validJoinRequest.getPlayerUuid())).thenReturn(Optional.of(samplePlayer));
        when(teamMemberRepository.findActiveByTeamAndPlayer(sampleTeam, samplePlayer)).thenReturn(Optional.of(existingMember));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teamService.joinTeam(validJoinRequest)
        );

        assertEquals("El jugador ya es miembro activo del equipo", exception.getMessage());
        verify(teamRepository).findById(validJoinRequest.getTeamId());
        verify(playerProfileRepository).findById(validJoinRequest.getPlayerUuid());
        verify(teamMemberRepository).findActiveByTeamAndPlayer(sampleTeam, samplePlayer);
        verify(teamMemberRepository, never()).save(any(TeamMember.class));
    }

    @Test
    void getTeamById_ExistingTeam_ShouldReturnTeam() {
        // Arrange
        Long teamId = 1L;
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(sampleTeam));
        when(teamMemberRepository.findActiveByTeam(sampleTeam)).thenReturn(java.util.Collections.emptyList());

        // Act
        TeamResponse response = teamService.getTeamById(teamId);

        // Assert
        assertNotNull(response);
        assertEquals(sampleTeam.getId(), response.getId());
        assertEquals(sampleTeam.getNombre(), response.getNombre());

        verify(teamRepository).findById(teamId);
        verify(teamMemberRepository).findActiveByTeam(sampleTeam);
    }

    @Test
    void getTeamById_NonExistingTeam_ShouldThrowException() {
        // Arrange
        Long nonExistingId = 999L;
        when(teamRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teamService.getTeamById(nonExistingId)
        );

        assertEquals("Equipo no encontrado: " + nonExistingId, exception.getMessage());
        verify(teamRepository).findById(nonExistingId);
    }

    @Test
    void deactivateMember_ValidRequest_ShouldDeactivateMember() {
        // Arrange
        Long teamId = 1L;
        UUID playerUuid = samplePlayer.getAtletaUuid();
        TeamMember activeMember = new TeamMember(sampleTeam, samplePlayer);
        activeMember.setActivo(true);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(sampleTeam));
        when(playerProfileRepository.findById(playerUuid)).thenReturn(Optional.of(samplePlayer));
        when(teamMemberRepository.findActiveByTeamAndPlayer(sampleTeam, samplePlayer)).thenReturn(Optional.of(activeMember));
        when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(activeMember);

        // Act
        teamService.deactivateMember(teamId, playerUuid);

        // Assert
        verify(teamRepository).findById(teamId);
        verify(playerProfileRepository).findById(playerUuid);
        verify(teamMemberRepository).findActiveByTeamAndPlayer(sampleTeam, samplePlayer);
        verify(teamMemberRepository).save(activeMember);
    }

    @Test
    void changeRole_ValidRequest_ShouldChangeRole() {
        // Arrange
        Long teamId = 1L;
        UUID playerUuid = samplePlayer.getAtletaUuid();
        PlayerRole newRole = PlayerRole.CAPITAN;
        TeamMember member = new TeamMember(sampleTeam, samplePlayer, PlayerRole.JUGADOR);
        member.setId(1L);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(sampleTeam));
        when(playerProfileRepository.findById(playerUuid)).thenReturn(Optional.of(samplePlayer));
        when(teamMemberRepository.findActiveByTeamAndPlayer(sampleTeam, samplePlayer)).thenReturn(Optional.of(member));
        when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(member);

        // Act
        TeamMemberResponse response = teamService.changeRole(teamId, playerUuid, newRole);

        // Assert
        assertNotNull(response);
        assertEquals(newRole, response.getRol());

        verify(teamRepository).findById(teamId);
        verify(playerProfileRepository).findById(playerUuid);
        verify(teamMemberRepository).findActiveByTeamAndPlayer(sampleTeam, samplePlayer);
        verify(teamMemberRepository).save(member);
    }
    */
}
