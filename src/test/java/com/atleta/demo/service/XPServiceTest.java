package com.atleta.demo.service;

import com.atleta.demo.enums.MatchResult;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.service.xp.MatchXpInput;
import com.atleta.demo.service.xp.PlayerXpSnapshot;
import com.atleta.demo.service.xp.XPService;
import com.atleta.demo.service.xp.XpResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XPServiceTest {

    private XPService xpService;

    @BeforeEach
    void setUp() {
        xpService = new XPService();
    }

    @Test
    void calculateXp_DelanteroVictoriaGolAsistencia_AplicaReglas() {
        PlayerXpSnapshot snapshot = player(
                "Delantero",
                PlayerRole.JUGADOR,
                MatchResult.VICTORIA,
                1,
                1,
                0
        );

        XpResult result = xpService.calculateXp(new MatchXpInput(true, List.of(snapshot))).getFirst();
        assertEquals(10, result.breakdown().playXp());
        assertEquals(10, result.breakdown().resultXp());
        assertEquals(10, result.breakdown().goalXp());
        assertEquals(8, result.breakdown().assistXp());
        assertEquals(0, result.breakdown().goalkeeperXp());
        assertEquals(38, result.breakdown().totalXp());
    }

    @Test
    void calculateXp_MediocampoConDosGolesYDerrota_AplicaEscalaPorPosicion() {
        PlayerXpSnapshot snapshot = player(
                "Mediocampo",
                PlayerRole.JUGADOR,
                MatchResult.DERROTA,
                2,
                1,
                0
        );

        XpResult result = xpService.calculateXp(new MatchXpInput(true, List.of(snapshot))).getFirst();
        assertEquals(24, result.breakdown().goalXp());
        assertEquals(8, result.breakdown().assistXp());
        assertEquals(5, result.breakdown().resultXp());
        assertEquals(47, result.breakdown().totalXp());
    }

    @Test
    void calculateXp_DefensaConGol_AplicaBonoDefensivoDeGol() {
        PlayerXpSnapshot snapshot = player(
                "Defensa",
                PlayerRole.CAPITAN,
                MatchResult.VICTORIA,
                1,
                0,
                0
        );

        XpResult result = xpService.calculateXp(new MatchXpInput(true, List.of(snapshot))).getFirst();
        assertEquals(15, result.breakdown().goalXp());
        assertEquals(35, result.breakdown().totalXp());
    }

    @Test
    void calculateXp_PorteroVallaInvicta_AplicaBonoPorteroAlto() {
        PlayerXpSnapshot snapshot = player(
                "Portero",
                PlayerRole.JUGADOR,
                MatchResult.VICTORIA,
                0,
                0,
                0
        );

        XpResult result = xpService.calculateXp(new MatchXpInput(true, List.of(snapshot))).getFirst();
        assertEquals(20, result.breakdown().goalkeeperXp());
        assertEquals(40, result.breakdown().totalXp());
    }

    @Test
    void calculateXp_PorteroConDosRecibidos_AplicaBonoIntermedio() {
        PlayerXpSnapshot snapshot = player(
                "Arquero",
                PlayerRole.JUGADOR,
                MatchResult.DERROTA,
                0,
                0,
                2
        );

        XpResult result = xpService.calculateXp(new MatchXpInput(true, List.of(snapshot))).getFirst();
        assertEquals(10, result.breakdown().goalkeeperXp());
        assertEquals(25, result.breakdown().totalXp());
    }

    @Test
    void calculateXp_DTRolIgnoraGolesYAsistencias() {
        PlayerXpSnapshot snapshot = player(
                "DT",
                PlayerRole.DT,
                MatchResult.VICTORIA,
                3,
                4,
                0
        );

        XpResult result = xpService.calculateXp(new MatchXpInput(true, List.of(snapshot))).getFirst();
        assertEquals(0, result.breakdown().goalXp());
        assertEquals(0, result.breakdown().assistXp());
        assertEquals(20, result.breakdown().totalXp());
    }

    @Test
    void calculateXp_MatchInvalido_NoEntregaXp() {
        PlayerXpSnapshot snapshot = player(
                "Delantero",
                PlayerRole.JUGADOR,
                MatchResult.VICTORIA,
                5,
                3,
                0
        );

        XpResult result = xpService.calculateXp(new MatchXpInput(false, List.of(snapshot))).getFirst();
        assertEquals(0, result.breakdown().totalXp());
        assertEquals(0, result.breakdown().playXp());
        assertEquals(0, result.breakdown().resultXp());
        assertEquals(0, result.breakdown().goalXp());
        assertEquals(0, result.breakdown().assistXp());
        assertEquals(0, result.breakdown().goalkeeperXp());
    }

    @Test
    void calculateXp_SnapshotNulo_LanzaError() {
        assertThrows(IllegalArgumentException.class, () -> xpService.calculateXp(null));
    }

    private PlayerXpSnapshot player(
            String positionName,
            PlayerRole role,
            MatchResult result,
            int goals,
            int assists,
            int goalsConceded
    ) {
        return new PlayerXpSnapshot(
                UUID.randomUUID(),
                1L,
                positionName,
                role,
                result,
                goals,
                assists,
                goalsConceded,
                true
        );
    }
}
