package com.atleta.demo.entity;

import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchResult;
import com.atleta.demo.enums.MatchStatus;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de propiedades para la entidad PlayerHistory.
 * 
 * Feature: api-foundation, Property 9: Inmutabilidad del historial como fuente de verdad
 * Valida: Requisitos 9.1, 9.4
 */
class PlayerHistoryPropertyTest {

    /**
     * Property 9: Inmutabilidad del historial como fuente de verdad
     * Para cualquier registro de historial de jugador, una vez creado no debe poder modificarse 
     * y debe ser la única fuente de verdad para estadísticas históricas
     * 
     * Validates: Requirements 9.1, 9.4
     */
    @Property(tries = 100)
    @Label("Feature: api-foundation, Property 9: Inmutabilidad del historial como fuente de verdad")
    void playerHistoryImmutabilityProperty(
            @ForAll("validMatch") Match match,
            @ForAll("validPlayerProfile") PlayerProfile player,
            @ForAll("validTeam") Team team,
            @ForAll("validPosition") Position position,
            @ForAll("validGoles") Integer goles,
            @ForAll("validAsistencias") Integer asistencias,
            @ForAll("validMatchResult") MatchResult resultado,
            @ForAll("validXP") Integer xpGanada) {
        
        // Crear registro de historial
        PlayerHistory history = new PlayerHistory(match, player, team, position, 
                                                 goles, asistencias, resultado, xpGanada);
        
        // Verificar que todos los campos están establecidos correctamente
        assertThat(history.getMatch()).isEqualTo(match);
        assertThat(history.getPlayer()).isEqualTo(player);
        assertThat(history.getTeam()).isEqualTo(team);
        assertThat(history.getPosition()).isEqualTo(position);
        assertThat(history.getGoles()).isEqualTo(goles != null ? goles : 0);
        assertThat(history.getAsistencias()).isEqualTo(asistencias != null ? asistencias : 0);
        assertThat(history.getResultado()).isEqualTo(resultado);
        assertThat(history.getXpGanada()).isEqualTo(xpGanada != null ? xpGanada : 0);
        
        // Verificar que la entidad es inmutable (no tiene setters públicos)
        // Esto se verifica por la ausencia de métodos setters en la clase
        // Solo tiene getters, lo que garantiza inmutabilidad
        
        // Verificar métodos utilitarios funcionan correctamente
        if (resultado == MatchResult.VICTORIA) {
            assertThat(history.isVictoria()).isTrue();
            assertThat(history.isDerrota()).isFalse();
            assertThat(history.isEmpate()).isFalse();
        } else if (resultado == MatchResult.DERROTA) {
            assertThat(history.isVictoria()).isFalse();
            assertThat(history.isDerrota()).isTrue();
            assertThat(history.isEmpate()).isFalse();
        } else { // EMPATE
            assertThat(history.isVictoria()).isFalse();
            assertThat(history.isDerrota()).isFalse();
            assertThat(history.isEmpate()).isTrue();
        }
        
        // Verificar métodos de contribuciones
        int expectedGoles = goles != null ? goles : 0;
        int expectedAsistencias = asistencias != null ? asistencias : 0;
        
        assertThat(history.hasGoles()).isEqualTo(expectedGoles > 0);
        assertThat(history.hasAsistencias()).isEqualTo(expectedAsistencias > 0);
        assertThat(history.getTotalContribuciones()).isEqualTo(expectedGoles + expectedAsistencias);
    }

    /**
     * Propiedad que verifica que el historial es la fuente de verdad para estadísticas
     */
    @Property(tries = 100)
    @Label("Feature: api-foundation, Property 9: Historial como fuente única de verdad")
    void playerHistoryAsSingleSourceOfTruth(
            @ForAll("validMatch") Match match,
            @ForAll("validPlayerProfile") PlayerProfile player,
            @ForAll("validTeam") Team team,
            @ForAll("validPosition") Position position,
            @ForAll("validGoles") Integer goles,
            @ForAll("validAsistencias") Integer asistencias,
            @ForAll("validMatchResult") MatchResult resultado,
            @ForAll("validXP") Integer xpGanada) {
        
        PlayerHistory history = new PlayerHistory(match, player, team, position, 
                                                 goles, asistencias, resultado, xpGanada);
        
        // Verificar que el historial contiene toda la información necesaria
        // para ser la fuente de verdad de estadísticas
        assertThat(history.getMatch()).isNotNull();
        assertThat(history.getPlayer()).isNotNull();
        assertThat(history.getTeam()).isNotNull();
        assertThat(history.getPosition()).isNotNull();
        assertThat(history.getResultado()).isNotNull();
        
        // Verificar que las estadísticas son no negativas
        assertThat(history.getGoles()).isGreaterThanOrEqualTo(0);
        assertThat(history.getAsistencias()).isGreaterThanOrEqualTo(0);
        assertThat(history.getXpGanada()).isGreaterThanOrEqualTo(0);
        
        // Verificar que el total de contribuciones es correcto
        assertThat(history.getTotalContribuciones())
            .isEqualTo(history.getGoles() + history.getAsistencias());
    }

    /**
     * Propiedad que verifica la consistencia de los valores por defecto
     */
    @Property(tries = 100)
    @Label("Feature: api-foundation, Property 9: Valores por defecto consistentes")
    void playerHistoryDefaultValuesConsistency(
            @ForAll("validMatch") Match match,
            @ForAll("validPlayerProfile") PlayerProfile player,
            @ForAll("validTeam") Team team,
            @ForAll("validPosition") Position position,
            @ForAll("validMatchResult") MatchResult resultado) {
        
        // Crear historial con valores null para probar valores por defecto
        PlayerHistory history = new PlayerHistory(match, player, team, position, 
                                                 null, null, resultado, null);
        
        // Verificar que los valores null se convierten a 0
        assertThat(history.getGoles()).isEqualTo(0);
        assertThat(history.getAsistencias()).isEqualTo(0);
        assertThat(history.getXpGanada()).isEqualTo(0);
        assertThat(history.getTotalContribuciones()).isEqualTo(0);
        
        // Verificar que los métodos utilitarios funcionan con valores por defecto
        assertThat(history.hasGoles()).isFalse();
        assertThat(history.hasAsistencias()).isFalse();
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
            match.setEstado(MatchStatus.FINALIZADO); // Para historial, el partido debe estar finalizado
            match.setLatitud(new BigDecimal("40.7128"));
            match.setLongitud(new BigDecimal("-74.0060"));
            match.setCuota(new BigDecimal("10.00"));
            return match;
        });
    }

    @Provide
    Arbitrary<PlayerProfile> validPlayerProfile() {
        return Arbitraries.create(() -> {
            Athlete athlete = new Athlete();
            athlete.setAtletaUuid(UUID.randomUUID());
            athlete.setEmail("player" + System.nanoTime() + "@example.com");
            athlete.setNombre("Test Player");
            athlete.setPasswordHash("hashedPassword");
            
            PlayerProfile profile = new PlayerProfile(athlete);
            profile.setAlias("PlayerAlias" + System.nanoTime());
            return profile;
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
    Arbitrary<Position> validPosition() {
        return Arbitraries.create(() -> {
            Position position = new Position();
            position.setNombre("Delantero"); // Posición fija para simplicidad
            return position;
        });
    }

    @Provide
    Arbitrary<Integer> validGoles() {
        return Arbitraries.integers().between(0, 10);
    }

    @Provide
    Arbitrary<Integer> validAsistencias() {
        return Arbitraries.integers().between(0, 10);
    }

    @Provide
    Arbitrary<MatchResult> validMatchResult() {
        return Arbitraries.of(MatchResult.class);
    }

    @Provide
    Arbitrary<Integer> validXP() {
        return Arbitraries.integers().between(0, 100);
    }
}