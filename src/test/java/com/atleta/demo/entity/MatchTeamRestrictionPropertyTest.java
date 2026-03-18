package com.atleta.demo.entity;

import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchStatus;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de propiedades para la restricción de equipos por partido.
 * 
 * Feature: api-foundation, Property 11: Restricción de equipos por partido
 * Valida: Requisitos 6.1, 6.2
 */
class MatchTeamRestrictionPropertyTest {

    /**
     * Property 11: Restricción de equipos por partido
     * Para cualquier partido válido, debe tener exactamente 2 equipos asociados 
     * (uno local y uno visitante)
     * 
     * Validates: Requirements 6.1, 6.2
     */
    @Property(tries = 100)
    @Label("Feature: api-foundation, Property 11: Restricción de equipos por partido")
    void matchMustHaveExactlyTwoTeamsProperty(
            @ForAll("validMatch") Match match,
            @ForAll("validTeam") Team teamLocal,
            @ForAll("validTeam") Team teamVisitante) {
        
        // Crear MatchTeam para equipo local
        MatchTeam matchTeamLocal = new MatchTeam(match, teamLocal, true);
        
        // Crear MatchTeam para equipo visitante
        MatchTeam matchTeamVisitante = new MatchTeam(match, teamVisitante, false);
        
        // Agregar equipos al partido
        List<MatchTeam> matchTeams = new ArrayList<>();
        matchTeams.add(matchTeamLocal);
        matchTeams.add(matchTeamVisitante);
        match.setMatchTeams(matchTeams);
        
        // Verificar que el partido tiene exactamente 2 equipos
        assertThat(match.getMatchTeams()).hasSize(2);
        assertThat(match.hasExactlyTwoTeams()).isTrue();
        
        // Verificar que hay exactamente un equipo local y uno visitante
        long equiposLocales = match.getMatchTeams().stream()
            .filter(mt -> Boolean.TRUE.equals(mt.getEsLocal()))
            .count();
        long equiposVisitantes = match.getMatchTeams().stream()
            .filter(mt -> Boolean.FALSE.equals(mt.getEsLocal()))
            .count();
        
        assertThat(equiposLocales).isEqualTo(1);
        assertThat(equiposVisitantes).isEqualTo(1);
        
        // Verificar que los equipos son diferentes
        assertThat(matchTeamLocal.getTeam()).isNotEqualTo(matchTeamVisitante.getTeam());
        
        // Verificar propiedades de cada MatchTeam
        assertThat(matchTeamLocal.isLocal()).isTrue();
        assertThat(matchTeamLocal.isVisitante()).isFalse();
        assertThat(matchTeamVisitante.isLocal()).isFalse();
        assertThat(matchTeamVisitante.isVisitante()).isTrue();
        
        // Verificar que ambos equipos tienen goles inicializados en 0
        assertThat(matchTeamLocal.getGoles()).isEqualTo(0);
        assertThat(matchTeamVisitante.getGoles()).isEqualTo(0);
    }

    /**
     * Propiedad que verifica que un partido sin equipos no es válido
     */
    @Property(tries = 100)
    @Label("Feature: api-foundation, Property 11: Partido sin equipos no es válido")
    void matchWithoutTeamsIsInvalid(@ForAll("validMatch") Match match) {
        
        // Partido sin equipos
        match.setMatchTeams(new ArrayList<>());
        
        // Verificar que no tiene exactamente 2 equipos
        assertThat(match.getMatchTeams()).hasSize(0);
        assertThat(match.hasExactlyTwoTeams()).isFalse();
    }

    /**
     * Propiedad que verifica que un partido con solo un equipo no es válido
     */
    @Property(tries = 100)
    @Label("Feature: api-foundation, Property 11: Partido con un equipo no es válido")
    void matchWithOneTeamIsInvalid(
            @ForAll("validMatch") Match match,
            @ForAll("validTeam") Team team) {
        
        // Crear partido con solo un equipo
        MatchTeam matchTeam = new MatchTeam(match, team, true);
        List<MatchTeam> matchTeams = new ArrayList<>();
        matchTeams.add(matchTeam);
        match.setMatchTeams(matchTeams);
        
        // Verificar que no tiene exactamente 2 equipos
        assertThat(match.getMatchTeams()).hasSize(1);
        assertThat(match.hasExactlyTwoTeams()).isFalse();
    }

    /**
     * Propiedad que verifica que un partido con más de dos equipos no es válido
     */
    @Property(tries = 100)
    @Label("Feature: api-foundation, Property 11: Partido con más de dos equipos no es válido")
    void matchWithMoreThanTwoTeamsIsInvalid(
            @ForAll("validMatch") Match match,
            @ForAll("validTeam") Team team1,
            @ForAll("validTeam") Team team2,
            @ForAll("validTeam") Team team3) {
        
        // Crear partido con tres equipos
        MatchTeam matchTeam1 = new MatchTeam(match, team1, true);
        MatchTeam matchTeam2 = new MatchTeam(match, team2, false);
        MatchTeam matchTeam3 = new MatchTeam(match, team3, false);
        
        List<MatchTeam> matchTeams = new ArrayList<>();
        matchTeams.add(matchTeam1);
        matchTeams.add(matchTeam2);
        matchTeams.add(matchTeam3);
        match.setMatchTeams(matchTeams);
        
        // Verificar que no tiene exactamente 2 equipos
        assertThat(match.getMatchTeams()).hasSize(3);
        assertThat(match.hasExactlyTwoTeams()).isFalse();
    }

    /**
     * Propiedad que verifica la unicidad de equipos local/visitante
     */
    @Property(tries = 100)
    @Label("Feature: api-foundation, Property 11: Unicidad de equipos local/visitante")
    void matchTeamLocalVisitanteUniqueness(
            @ForAll("validMatch") Match match,
            @ForAll("validTeam") Team teamLocal,
            @ForAll("validTeam") Team teamVisitante) {
        
        // Crear MatchTeams válidos
        MatchTeam matchTeamLocal = new MatchTeam(match, teamLocal, true);
        MatchTeam matchTeamVisitante = new MatchTeam(match, teamVisitante, false);
        
        List<MatchTeam> matchTeams = new ArrayList<>();
        matchTeams.add(matchTeamLocal);
        matchTeams.add(matchTeamVisitante);
        match.setMatchTeams(matchTeams);
        
        // Verificar que solo hay un equipo local
        long equiposLocales = match.getMatchTeams().stream()
            .filter(MatchTeam::isLocal)
            .count();
        assertThat(equiposLocales).isEqualTo(1);
        
        // Verificar que solo hay un equipo visitante
        long equiposVisitantes = match.getMatchTeams().stream()
            .filter(MatchTeam::isVisitante)
            .count();
        assertThat(equiposVisitantes).isEqualTo(1);
        
        // Verificar que no hay equipos con estado ambiguo
        assertThat(match.getMatchTeams()).allSatisfy(mt -> 
            assertThat(mt.getEsLocal()).isNotNull()
        );
    }

    /**
     * Propiedad que verifica la funcionalidad de incremento/decremento de goles
     */
    @Property(tries = 100)
    @Label("Feature: api-foundation, Property 11: Funcionalidad de goles por equipo")
    void matchTeamGoalsFunctionality(
            @ForAll("validMatch") Match match,
            @ForAll("validTeam") Team team,
            @ForAll("validGoles") Integer golesIniciales) {
        
        MatchTeam matchTeam = new MatchTeam(match, team, true);
        matchTeam.setGoles(golesIniciales);
        
        // Verificar estado inicial
        assertThat(matchTeam.getGoles()).isEqualTo(golesIniciales);
        
        // Incrementar goles
        matchTeam.incrementarGoles();
        assertThat(matchTeam.getGoles()).isEqualTo(golesIniciales + 1);
        
        // Decrementar goles (solo si es mayor a 0)
        if (golesIniciales + 1 > 0) {
            matchTeam.decrementarGoles();
            assertThat(matchTeam.getGoles()).isEqualTo(golesIniciales);
        }
        
        // Verificar que no se puede decrementar por debajo de 0
        matchTeam.setGoles(0);
        matchTeam.decrementarGoles();
        assertThat(matchTeam.getGoles()).isEqualTo(0);
    }

    // Generators
    @Provide
    Arbitrary<Match> validMatch() {
        return Arbitraries.create(() -> {
            Athlete creatorAthlete = new Athlete();
            creatorAthlete.setAtletaUuid(UUID.randomUUID());
            creatorAthlete.setEmail("matchcreator" + System.nanoTime() + "@example.com");
            creatorAthlete.setNombre("Match Creator");
            creatorAthlete.setPasswordHash("hashedPassword");
            
            PlayerProfile creator = new PlayerProfile(creatorAthlete);
            
            Match match = new Match();
            match.setModalidad(MatchMode.CINCO_VS_CINCO);
            match.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
            match.setCreador(creator);
            match.setEstado(MatchStatus.CREADO);
            match.setLatitud(new BigDecimal("40.7128"));
            match.setLongitud(new BigDecimal("-74.0060"));
            match.setCuota(new BigDecimal("10.00"));
            return match;
        });
    }

    @Provide
    Arbitrary<Team> validTeam() {
        return Arbitraries.create(() -> {
            Athlete creatorAthlete = new Athlete();
            creatorAthlete.setAtletaUuid(UUID.randomUUID());
            creatorAthlete.setEmail("teamcreator" + System.nanoTime() + "@example.com");
            creatorAthlete.setNombre("Team Creator");
            creatorAthlete.setPasswordHash("hashedPassword");
            
            PlayerProfile creator = new PlayerProfile(creatorAthlete);
            
            Team team = new Team();
            team.setNombre("Team " + System.nanoTime());
            team.setCreador(creator);
            return team;
        });
    }

    @Provide
    Arbitrary<Integer> validGoles() {
        return Arbitraries.integers().between(0, 10);
    }
}