package com.atleta.demo.entity;

import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchStatus;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de propiedades para la entidad TrustLog.
 * 
 * Feature: api-foundation, Property 10: Trazabilidad de confianza
 * Valida: Requisitos 10.1, 10.3, 10.4
 */
class TrustLogPropertyTest {

    /**
     * Property 10: Trazabilidad de confianza
     * Para cualquier cambio en el trust_score, debe registrarse en trust_logs con motivo, 
     * fecha y referencia al partido si aplica
     * 
     * Validates: Requirements 10.1, 10.3, 10.4
     */
    @Property(tries = 100)
    @Label("Feature: api-foundation, Property 10: Trazabilidad de confianza")
    void trustScoreTraceabilityProperty(
            @ForAll("validPlayerProfile") PlayerProfile player,
            @ForAll("validTrustChange") Integer cambio,
            @ForAll("validTrustScore") Integer trustScoreAnterior,
            @ForAll("validMotivo") String motivo,
            @ForAll("validPlayerProfile") PlayerProfile changedBy) {
        
        // Calcular nuevo trust score
        Integer trustScoreNuevo = trustScoreAnterior + cambio;
        
        // Crear log de confianza sin partido (cambio manual)
        TrustLog trustLog = new TrustLog(player, cambio, trustScoreAnterior, 
                                        trustScoreNuevo, motivo, changedBy);
        
        // Verificar trazabilidad completa
        assertThat(trustLog.getPlayer()).isEqualTo(player);
        assertThat(trustLog.getCambio()).isEqualTo(cambio);
        assertThat(trustLog.getTrustScoreAnterior()).isEqualTo(trustScoreAnterior);
        assertThat(trustLog.getTrustScoreNuevo()).isEqualTo(trustScoreNuevo);
        assertThat(trustLog.getMotivo()).isEqualTo(motivo);
        assertThat(trustLog.getChangedBy()).isEqualTo(changedBy);
        
        // Verificar que no está relacionado con partido
        assertThat(trustLog.getMatch()).isNull();
        assertThat(trustLog.isRelatedToMatch()).isFalse();
        
        // Verificar que es un cambio manual
        assertThat(trustLog.isManualChange()).isTrue();
        assertThat(trustLog.isSystemChange()).isFalse();
        
        // Verificar métodos utilitarios
        if (cambio > 0) {
            assertThat(trustLog.isIncremento()).isTrue();
            assertThat(trustLog.isDecremento()).isFalse();
        } else if (cambio < 0) {
            assertThat(trustLog.isIncremento()).isFalse();
            assertThat(trustLog.isDecremento()).isTrue();
        }
        
        assertThat(trustLog.getAbsoluteCambio()).isEqualTo(Math.abs(cambio));
    }

    /**
     * Propiedad que verifica la trazabilidad de cambios relacionados con partidos
     */
    @Property(tries = 100)
    @Label("Feature: api-foundation, Property 10: Trazabilidad de cambios relacionados con partidos")
    void matchRelatedTrustChangeTraceability(
            @ForAll("validPlayerProfile") PlayerProfile player,
            @ForAll("validMatch") Match match,
            @ForAll("validTrustChange") Integer cambio,
            @ForAll("validTrustScore") Integer trustScoreAnterior,
            @ForAll("validMotivo") String motivo) {
        
        Integer trustScoreNuevo = trustScoreAnterior + cambio;
        
        // Crear log de confianza relacionado con partido (cambio automático del sistema)
        TrustLog trustLog = new TrustLog(player, match, cambio, trustScoreAnterior, 
                                        trustScoreNuevo, motivo, null);
        
        // Verificar trazabilidad con partido
        assertThat(trustLog.getPlayer()).isEqualTo(player);
        assertThat(trustLog.getMatch()).isEqualTo(match);
        assertThat(trustLog.getCambio()).isEqualTo(cambio);
        assertThat(trustLog.getTrustScoreAnterior()).isEqualTo(trustScoreAnterior);
        assertThat(trustLog.getTrustScoreNuevo()).isEqualTo(trustScoreNuevo);
        assertThat(trustLog.getMotivo()).isEqualTo(motivo);
        
        // Verificar que está relacionado con partido
        assertThat(trustLog.isRelatedToMatch()).isTrue();
        
        // Verificar que es un cambio del sistema (sin changedBy)
        assertThat(trustLog.getChangedBy()).isNull();
        assertThat(trustLog.isSystemChange()).isTrue();
        assertThat(trustLog.isManualChange()).isFalse();
    }

    /**
     * Propiedad que verifica la consistencia del historial completo de cambios
     */
    @Property(tries = 100)
    @Label("Feature: api-foundation, Property 10: Consistencia del historial de cambios")
    void trustChangeHistoryConsistency(
            @ForAll("validPlayerProfile") PlayerProfile player,
            @ForAll("validTrustChange") Integer cambio,
            @ForAll("validTrustScore") Integer trustScoreAnterior,
            @ForAll("validMotivo") String motivo) {
        
        Integer trustScoreNuevo = trustScoreAnterior + cambio;
        
        // Crear log de confianza automático del sistema
        TrustLog trustLog = new TrustLog(player, cambio, trustScoreAnterior, 
                                        trustScoreNuevo, motivo);
        
        // Verificar que mantiene historial completo
        assertThat(trustLog.getPlayer()).isEqualTo(player);
        assertThat(trustLog.getCambio()).isEqualTo(cambio);
        assertThat(trustLog.getTrustScoreAnterior()).isEqualTo(trustScoreAnterior);
        assertThat(trustLog.getTrustScoreNuevo()).isEqualTo(trustScoreNuevo);
        assertThat(trustLog.getMotivo()).isEqualTo(motivo);
        
        // Verificar que es cambio del sistema
        assertThat(trustLog.getChangedBy()).isNull();
        assertThat(trustLog.getMatch()).isNull();
        assertThat(trustLog.isSystemChange()).isTrue();
        assertThat(trustLog.isManualChange()).isFalse();
        assertThat(trustLog.isRelatedToMatch()).isFalse();
        
        // Verificar que el cálculo del nuevo score es correcto
        assertThat(trustLog.getTrustScoreNuevo())
            .isEqualTo(trustLog.getTrustScoreAnterior() + trustLog.getCambio());
    }

    /**
     * Propiedad que verifica que el motivo siempre está presente
     */
    @Property(tries = 100)
    @Label("Feature: api-foundation, Property 10: Motivo siempre presente")
    void trustChangeAlwaysHasReason(
            @ForAll("validPlayerProfile") PlayerProfile player,
            @ForAll("validTrustChange") Integer cambio,
            @ForAll("validTrustScore") Integer trustScoreAnterior,
            @ForAll("validMotivo") String motivo) {
        
        Integer trustScoreNuevo = trustScoreAnterior + cambio;
        
        TrustLog trustLog = new TrustLog(player, cambio, trustScoreAnterior, 
                                        trustScoreNuevo, motivo);
        
        // Verificar que el motivo nunca está vacío o null
        assertThat(trustLog.getMotivo()).isNotNull();
        assertThat(trustLog.getMotivo()).isNotBlank();
        assertThat(trustLog.getMotivo().length()).isLessThanOrEqualTo(255);
    }

    // Generators
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
            profile.setTrustScore(100); // Score inicial
            return profile;
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
            match.setEstado(MatchStatus.FINALIZADO);
            match.setLatitud(new BigDecimal("40.7128"));
            match.setLongitud(new BigDecimal("-74.0060"));
            match.setCuota(new BigDecimal("10.00"));
            return match;
        });
    }

    @Provide
    Arbitrary<Integer> validTrustChange() {
        return Arbitraries.integers().between(-50, 50).filter(i -> i != 0);
    }

    @Provide
    Arbitrary<Integer> validTrustScore() {
        return Arbitraries.integers().between(0, 1000);
    }

    @Provide
    Arbitrary<String> validMotivo() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .withCharRange('A', 'Z')
            .withCharRange('0', '9')
            .withChars(' ', '.', ',', '!', '?')
            .ofMinLength(5)
            .ofMaxLength(100)
            .map(s -> "Motivo: " + s);
    }
}