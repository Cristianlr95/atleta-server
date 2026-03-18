package com.atleta.demo.repository;

import com.atleta.demo.entity.*;
import com.atleta.demo.enums.PlayerRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para TeamMemberRepository.
 * Verifica operaciones CRUD y consultas personalizadas.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TeamMemberRepositoryTest {

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private AthleteRepository athleteRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private TeamRepository teamRepository;

    private PlayerProfile testPlayer1;
    private PlayerProfile testPlayer2;
    private PlayerProfile testPlayer3;
    private Team testTeam1;
    private Team testTeam2;
    private TeamMember testMember1;
    private TeamMember testMember2;
    private TeamMember testMember3;
    private TeamMember testMember4;

    @BeforeEach
    void setUp() {
        // Limpiar datos existentes
        teamMemberRepository.deleteAll();
        teamRepository.deleteAll();
        playerProfileRepository.deleteAll();
        athleteRepository.deleteAll();

        // Crear atletas y perfiles
        Athlete athlete1 = new Athlete("player1@example.com", "hash1", "Player One");
        Athlete athlete2 = new Athlete("player2@example.com", "hash2", "Player Two");
        Athlete athlete3 = new Athlete("player3@example.com", "hash3", "Player Three");
        
        athlete1 = athleteRepository.save(athlete1);
        athlete2 = athleteRepository.save(athlete2);
        athlete3 = athleteRepository.save(athlete3);

        testPlayer1 = new PlayerProfile(athlete1, "Player1");
        testPlayer2 = new PlayerProfile(athlete2, "Player2");
        testPlayer3 = new PlayerProfile(athlete3, "Player3");
        
        testPlayer1 = playerProfileRepository.save(testPlayer1);
        testPlayer2 = playerProfileRepository.save(testPlayer2);
        testPlayer3 = playerProfileRepository.save(testPlayer3);

        // Crear equipos
        testTeam1 = new Team("Team Alpha", testPlayer1);
        testTeam2 = new Team("Team Beta", testPlayer2);
        
        testTeam1 = teamRepository.save(testTeam1);
        testTeam2 = teamRepository.save(testTeam2);

        // Crear membresías
        testMember1 = new TeamMember();
        testMember1.setTeam(testTeam1);
        testMember1.setPlayer(testPlayer1);
        testMember1.setRol(PlayerRole.CAPITAN);
        testMember1.setActivo(true);

        testMember2 = new TeamMember();
        testMember2.setTeam(testTeam1);
        testMember2.setPlayer(testPlayer2);
        testMember2.setRol(PlayerRole.JUGADOR);
        testMember2.setActivo(true);

        testMember3 = new TeamMember();
        testMember3.setTeam(testTeam1);
        testMember3.setPlayer(testPlayer3);
        testMember3.setRol(PlayerRole.JUGADOR);
        testMember3.setActivo(false); // Miembro inactivo

        testMember4 = new TeamMember();
        testMember4.setTeam(testTeam2);
        testMember4.setPlayer(testPlayer2);
        testMember4.setRol(PlayerRole.DT);
        testMember4.setActivo(true);

        testMember1 = teamMemberRepository.save(testMember1);
        testMember2 = teamMemberRepository.save(testMember2);
        testMember3 = teamMemberRepository.save(testMember3);
        testMember4 = teamMemberRepository.save(testMember4);
    }

    @Test
    void testFindActiveByTeam_ValidTeam_ReturnsOnlyActiveMembers() {
        // When
        List<TeamMember> results = teamMemberRepository.findActiveByTeam(testTeam1);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(TeamMember::getActivo);
        assertThat(results).extracting(tm -> tm.getPlayer().getAlias())
                .containsExactlyInAnyOrder("Player1", "Player2");
    }

    @Test
    void testFindByTeam_ValidTeam_ReturnsAllMembers() {
        // When
        List<TeamMember> results = teamMemberRepository.findByTeam(testTeam1);

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).extracting(tm -> tm.getPlayer().getAlias())
                .containsExactlyInAnyOrder("Player1", "Player2", "Player3");
    }

    @Test
    void testFindActiveByPlayer_ValidPlayer_ReturnsActiveTeams() {
        // When
        List<TeamMember> results = teamMemberRepository.findActiveByPlayer(testPlayer2);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(TeamMember::getActivo);
        assertThat(results).extracting(tm -> tm.getTeam().getNombre())
                .containsExactlyInAnyOrder("Team Alpha", "Team Beta");
    }

    @Test
    void testFindByPlayer_ValidPlayer_ReturnsAllTeams() {
        // When
        List<TeamMember> results = teamMemberRepository.findByPlayer(testPlayer2);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(tm -> tm.getTeam().getNombre())
                .containsExactlyInAnyOrder("Team Alpha", "Team Beta");
    }

    @Test
    void testFindByTeamAndPlayer_ValidTeamAndPlayer_ReturnsMembership() {
        // When
        Optional<TeamMember> result = teamMemberRepository.findByTeamAndPlayer(testTeam1, testPlayer1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getRol()).isEqualTo(PlayerRole.CAPITAN);
        assertThat(result.get().getActivo()).isTrue();
    }

    @Test
    void testFindActiveByTeamAndPlayer_ValidActiveTeamAndPlayer_ReturnsMembership() {
        // When
        Optional<TeamMember> result = teamMemberRepository.findActiveByTeamAndPlayer(testTeam1, testPlayer1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getRol()).isEqualTo(PlayerRole.CAPITAN);
    }

    @Test
    void testFindActiveByTeamAndPlayer_InactivePlayer_ReturnsEmpty() {
        // When
        Optional<TeamMember> result = teamMemberRepository.findActiveByTeamAndPlayer(testTeam1, testPlayer3);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindByTeamAndRol_ValidTeamAndRole_ReturnsMatchingMembers() {
        // When
        List<TeamMember> results = teamMemberRepository.findByTeamAndRol(testTeam1, PlayerRole.JUGADOR);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer2);
        assertThat(results.get(0).getActivo()).isTrue();
    }

    @Test
    void testFindCaptainsByTeam_ValidTeam_ReturnsCaptains() {
        // When
        List<TeamMember> results = teamMemberRepository.findCaptainsByTeam(testTeam1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer1);
        assertThat(results.get(0).getRol()).isEqualTo(PlayerRole.CAPITAN);
    }

    @Test
    void testFindCoachesByTeam_ValidTeam_ReturnsCoaches() {
        // When
        List<TeamMember> results = teamMemberRepository.findCoachesByTeam(testTeam2);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPlayer()).isEqualTo(testPlayer2);
        assertThat(results.get(0).getRol()).isEqualTo(PlayerRole.DT);
    }

    @Test
    void testIsActiveMember_ActiveMember_ReturnsTrue() {
        // When
        boolean isActive = teamMemberRepository.isActiveMember(testTeam1, testPlayer1);

        // Then
        assertThat(isActive).isTrue();
    }

    @Test
    void testIsActiveMember_InactiveMember_ReturnsFalse() {
        // When
        boolean isActive = teamMemberRepository.isActiveMember(testTeam1, testPlayer3);

        // Then
        assertThat(isActive).isFalse();
    }

    @Test
    void testIsActiveMember_NonMember_ReturnsFalse() {
        // Given
        Athlete newAthlete = new Athlete("new@example.com", "hash", "New Player");
        newAthlete = athleteRepository.save(newAthlete);
        PlayerProfile newPlayer = new PlayerProfile(newAthlete, "NewPlayer");
        newPlayer = playerProfileRepository.save(newPlayer);

        // When
        boolean isActive = teamMemberRepository.isActiveMember(testTeam1, newPlayer);

        // Then
        assertThat(isActive).isFalse();
    }

    @Test
    void testCountActiveByTeam_ValidTeam_ReturnsCorrectCount() {
        // When
        long count = teamMemberRepository.countActiveByTeam(testTeam1);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testCountActiveByPlayer_ValidPlayer_ReturnsCorrectCount() {
        // When
        long count = teamMemberRepository.countActiveByPlayer(testPlayer2);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testFindByJoinedAtAfter_RecentDate_ReturnsRecentMembers() {
        // Given
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        // When
        List<TeamMember> results = teamMemberRepository.findByJoinedAtAfter(oneHourAgo);

        // Then
        assertThat(results).hasSize(4); // All members should be recent
    }

    @Test
    void testFindByJoinedAtBetween_ValidRange_ReturnsMembersInRange() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        // When
        List<TeamMember> results = teamMemberRepository.findByJoinedAtBetween(start, end);

        // Then
        assertThat(results).hasSize(4);
    }

    @Test
    void testFindByPlayerAtletaUuid_ValidUuid_ReturnsPlayerMemberships() {
        // When
        List<TeamMember> results = teamMemberRepository.findByPlayerAtletaUuid(testPlayer2.getAtletaUuid());

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(tm -> tm.getPlayer().getAtletaUuid().equals(testPlayer2.getAtletaUuid()));
    }

    @Test
    void testFindActiveByPlayerAtletaUuid_ValidUuid_ReturnsActiveMemberships() {
        // When
        List<TeamMember> results = teamMemberRepository.findActiveByPlayerAtletaUuid(testPlayer2.getAtletaUuid());

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(TeamMember::getActivo);
        assertThat(results).allMatch(tm -> tm.getPlayer().getAtletaUuid().equals(testPlayer2.getAtletaUuid()));
    }

    @Test
    void testFindTeamsOrderByActiveMembersCount_ReturnsTeamsOrderedByMemberCount() {
        // When
        List<Team> results = teamMemberRepository.findTeamsOrderByActiveMembersCount();

        // Then
        assertThat(results).hasSize(2);
        // testTeam1 has 2 active members, testTeam2 has 1 active member
        assertThat(results.get(0).getNombre()).isEqualTo("Team Alpha");
        assertThat(results.get(1).getNombre()).isEqualTo("Team Beta");
    }

    @Test
    void testSaveAndFindById_ValidTeamMember_SavesAndRetrievesCorrectly() {
        // Given
        TeamMember newMember = new TeamMember();
        newMember.setTeam(testTeam2);
        newMember.setPlayer(testPlayer3);
        newMember.setRol(PlayerRole.JUGADOR);
        newMember.setActivo(true);

        // When
        TeamMember saved = teamMemberRepository.save(newMember);
        Optional<TeamMember> found = teamMemberRepository.findById(saved.getId());

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getRol()).isEqualTo(PlayerRole.JUGADOR);
        assertThat(found.get().getActivo()).isTrue();
        assertThat(found.get().getTeam().getNombre()).isEqualTo("Team Beta");
    }

    @Test
    void testDeleteById_ExistingTeamMember_DeletesSuccessfully() {
        // Given
        Long memberId = testMember1.getId();

        // When
        teamMemberRepository.deleteById(memberId);

        // Then
        Optional<TeamMember> found = teamMemberRepository.findById(memberId);
        assertThat(found).isEmpty();
        assertThat(teamMemberRepository.count()).isEqualTo(3);
    }

    @Test
    void testFindAll_ReturnsAllTeamMembers() {
        // When
        List<TeamMember> results = teamMemberRepository.findAll();

        // Then
        assertThat(results).hasSize(4);
    }

    @Test
    void testUpdate_ExistingTeamMember_UpdatesSuccessfully() {
        // Given
        testMember3.setActivo(true);

        // When
        TeamMember updated = teamMemberRepository.save(testMember3);

        // Then
        assertThat(updated.getActivo()).isTrue();
        
        Optional<TeamMember> found = teamMemberRepository.findById(testMember3.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getActivo()).isTrue();
    }

    @Test
    void testRoleChange_ExistingTeamMember_UpdatesRoleSuccessfully() {
        // Given
        testMember2.setRol(PlayerRole.CAPITAN);

        // When
        TeamMember updated = teamMemberRepository.save(testMember2);

        // Then
        assertThat(updated.getRol()).isEqualTo(PlayerRole.CAPITAN);
        
        // Verify we now have 2 captains in testTeam1
        List<TeamMember> captains = teamMemberRepository.findCaptainsByTeam(testTeam1);
        assertThat(captains).hasSize(2);
    }
}