package com.atleta.demo.entity;

import com.atleta.demo.enums.EventType;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchStatus;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de propiedades para la entidad MatchEvent.
 * 
 * Feature: api-foundation, Property 8: Validación de eventos
 * Valida: Requisitos 8.1, 8.2, 8.4
 */
class MatchEventPropertyTest {

    /**
     * Property 8: Validación de eventos
     * Para cualquier evento de partido registrado, debe tener tipo válido, jugador existente, 
     * y requerir confirmación de ambos equipos
     * 
     * Validates: Requirements 8.1, 8.2, 8.4
     */
    @Property(tries = 100)
    @Label("Feature: api-foundation, Property 8: Validación de eventos")
    void eventValidationProperty(
            @ForAll("validEventType") EventType eventType,
            @ForAll("validPlayerProfile") PlayerProfile player,
            @ForAll("validTeam") Team team,
            @ForAll("validMatch") Match match,
            @ForAll("validPlayerProfile") PlayerProfile registeredBy,
            @ForAll("validPlayerProfile") PlayerProfile assistPlayer) {
        
        // Crear evento con datos válidos
        MatchEvent event = new MatchEvent(match, eventType, player, team, registeredBy);
        
        // Si es un gol, puede tener asistente
        if (eventType == EventType.GOL) {
            event.setAssistPlayer(assistPlayer);
        }
        
        // Verificar que el evento tiene tipo válido
        assertThat(event.getTipoEvento()).isNotNull();
        assertThat(event.getTipoEvento()).isIn(EventType.GOL, EventType.ASISTENCIA);
        
        // Verificar que el jugador existe
        assertThat(event.getPlayer()).isNotNull();
        assertThat(event.getPlayer().getAtletaUuid()).isNotNull();
        
        // Verificar que el equipo existe
        assertThat(event.getTeam()).isNotNull();
        
        // Verificar que el partido existe
        assertThat(event.getMatch()).isNotNull();
        
        // Verificar que el registrador existe
        assertThat(event.getRegisteredBy()).isNotNull();
        
        // Verificar estado inicial de confirmaciones (ambos equipos deben confirmar)
        assertThat(event.getConfirmedByHome()).isFalse();
        assertThat(event.getConfirmedByAway()).isFalse();
        assertThat(event.isFullyConfirmed()).isFalse();
        
        // Verificar que se requiere confirmación de ambos equipos
        event.confirmByHome();
        assertThat(event.getConfirmedByHome()).isTrue();
        assertThat(event.isFullyConfirmed()).isFalse(); // Aún falta el visitante
        
        event.confirmByAway();
        assertThat(event.getConfirmedByAway()).isTrue();
        assertThat(event.isFullyConfirmed()).isTrue(); // Ahora está completamente confirmado
        
        // Verificar que se establece timestamp de confirmación
        assertThat(event.getConfirmedAt()).isNotNull();
        
        // Verificar métodos utilitarios
        if (eventType == EventType.GOL) {
            assertThat(event.isGol()).isTrue();
            assertThat(event.isAsistencia()).isFalse();
        } else {
            assertThat(event.isGol()).isFalse();
            assertThat(event.isAsistencia()).isTrue();
        }
    }

    /**
     * Propiedad que verifica que los eventos de gol pueden tener asistente opcional
     */
    @Property(tries = 100)
    @Label("Feature: api-foundation, Property 8: Goles pueden tener asistente opcional")
    void goalEventsCanHaveOptionalAssist(
            @ForAll("validPlayerProfile") PlayerProfile player,
            @ForAll("validTeam") Team team,
            @ForAll("validMatch") Match match,
            @ForAll("validPlayerProfile") PlayerProfile registeredBy,
            @ForAll("validPlayerProfile") PlayerProfile assistPlayer) {
        
        // Crear evento de gol
        MatchEvent golEvent = new MatchEvent(match, EventType.GOL, player, team, registeredBy);
        
        // Inicialmente sin asistente
        assertThat(golEvent.hasAssist()).isFalse();
        assertThat(golEvent.getAssistPlayer()).isNull();
        
        // Agregar asistente
        golEvent.setAssistPlayer(assistPlayer);
        assertThat(golEvent.hasAssist()).isTrue();
        assertThat(golEvent.getAssistPlayer()).isEqualTo(assistPlayer);
        
        // Crear evento de asistencia (no debería tener asistente)
        MatchEvent assistEvent = new MatchEvent(match, EventType.ASISTENCIA, player, team, registeredBy);
        assertThat(assistEvent.hasAssist()).isFalse();
    }

    /**
     * Propiedad que verifica la trazabilidad de quién registró el evento
     */
    @Property(tries = 100)
    @Label("Feature: api-foundation, Property 8: Trazabilidad de registro de eventos")
    void eventRegistrationTraceability(
            @ForAll("validEventType") EventType eventType,
            @ForAll("validPlayerProfile") PlayerProfile player,
            @ForAll("validTeam") Team team,
            @ForAll("validMatch") Match match,
            @ForAll("validPlayerProfile") PlayerProfile registeredBy) {
        
        MatchEvent event = new MatchEvent(match, eventType, player, team, registeredBy);
        
        // Verificar trazabilidad del registro
        assertThat(event.getRegisteredBy()).isEqualTo(registeredBy);
        assertThat(event.getRegisteredAt()).isNotNull();
        assertThat(event.getRegisteredAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    // Generators
    @Provide
    Arbitrary<EventType> validEventType() {
        return Arbitraries.of(EventType.class);
    }

    @Provide
    Arbitrary<PlayerProfile> validPlayerProfile() {
        return Arbitraries.create(() -> {
            Athlete athlete = new Athlete();
            athlete.setAtletaUuid(UUID.randomUUID());
            athlete.setEmail("test" + System.nanoTime() + "@example.com");
            athlete.setNombre("Test Player");
            athlete.setPasswordHash("hashedPassword");
            
            PlayerProfile profile = new PlayerProfile(athlete);
            profile.setAlias("TestAlias" + System.nanoTime());
            return profile;
        });
    }

    @Provide
    Arbitrary<Team> validTeam() {
        return Arbitraries.create(() -> {
            Athlete creatorAthlete = new Athlete();
            creatorAthlete.setAtletaUuid(UUID.randomUUID());
            creatorAthlete.setEmail("creator" + System.nanoTime() + "@example.com");
            creatorAthlete.setNombre("Creator");
            creatorAthlete.setPasswordHash("hashedPassword");
            
            PlayerProfile creator = new PlayerProfile(creatorAthlete);
            
            Team team = new Team();
            team.setNombre("Team " + System.nanoTime());
            team.setCreador(creator);
            return team;
        });
    }

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
}