package com.atleta.demo.service;

import com.atleta.demo.entity.Athlete;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.enums.GenderType;
import com.atleta.demo.enums.MatchGenderCategory;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.enums.MatchStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchRosterPolicyTest {

    private final MatchRosterPolicy policy = new MatchRosterPolicy();

    @Test
    void playersPerTeamByModality_ReturnsConfiguredLimits() {
        assertEquals(5, policy.playersPerTeamByModality(MatchMode.CINCO_VS_CINCO));
        assertEquals(6, policy.playersPerTeamByModality(MatchMode.SEIS_VS_SEIS));
        assertEquals(7, policy.playersPerTeamByModality(MatchMode.SIETE_VS_SIETE));
        assertEquals(5, policy.playersPerTeamByModality(null));
    }

    @Test
    void validateTeamAssignmentWindow_RejectsFinishedAndStartedMatches() {
        Match finished = new Match();
        finished.setEstado(MatchStatus.FINALIZADO);

        IllegalArgumentException finishedException = assertThrows(
                IllegalArgumentException.class,
                () -> policy.validateTeamAssignmentWindow(finished)
        );
        assertEquals("No se puede editar equipos en partido finalizado", finishedException.getMessage());

        Match started = new Match();
        started.setEstado(MatchStatus.INICIADO);
        started.setStartedAt(LocalDateTime.now());

        IllegalArgumentException startedException = assertThrows(
                IllegalArgumentException.class,
                () -> policy.validateTeamAssignmentWindow(started)
        );
        assertEquals("Los equipos se bloquean al comenzar el partido", startedException.getMessage());
    }

    @Test
    void hasMinimumConfirmedPlayers_CountsCreatorWhenMissingFromRows() {
        Match match = new Match();
        match.setModalidad(MatchMode.CINCO_VS_CINCO);
        PlayerProfile creator = player(GenderType.MASCULINO);
        match.setCreador(creator);

        List<MatchPlayer> players = java.util.stream.IntStream.range(0, 9)
                .mapToObj(index -> {
                    MatchPlayer player = matchPlayer(player(index == 0 ? GenderType.FEMENINO : GenderType.MASCULINO));
                    player.setConfirmado(true);
                    return player;
                })
                .toList();

        assertTrue(policy.hasMinimumConfirmedPlayers(match, players));
    }

    @Test
    void validateGenderAssignmentRules_AllowsBalancedMixedTeams() {
        Match match = matchWithCategory(MatchGenderCategory.MIXTO);
        PlayerProfile homeWoman = player(GenderType.FEMENINO);
        PlayerProfile homeMan = player(GenderType.MASCULINO);
        PlayerProfile awayWoman = player(GenderType.FEMENINO);
        PlayerProfile awayMan = player(GenderType.MASCULINO);

        assertDoesNotThrow(() -> policy.validateGenderAssignmentRules(
                match,
                List.of(matchPlayer(homeWoman), matchPlayer(homeMan), matchPlayer(awayWoman), matchPlayer(awayMan)),
                Set.of(homeWoman.getAtletaUuid(), homeMan.getAtletaUuid()),
                Set.of(awayWoman.getAtletaUuid(), awayMan.getAtletaUuid())
        ));
    }

    @Test
    void validateGenderAssignmentRules_RejectsUnbalancedMixedTeams() {
        Match match = matchWithCategory(MatchGenderCategory.MIXTO);
        PlayerProfile homeWoman = player(GenderType.FEMENINO);
        PlayerProfile homeManA = player(GenderType.MASCULINO);
        PlayerProfile homeManB = player(GenderType.MASCULINO);
        PlayerProfile homeManC = player(GenderType.MASCULINO);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> policy.validateGenderAssignmentRules(
                        match,
                        List.of(
                                matchPlayer(homeWoman),
                                matchPlayer(homeManA),
                                matchPlayer(homeManB),
                                matchPlayer(homeManC)
                        ),
                        Set.of(
                                homeWoman.getAtletaUuid(),
                                homeManA.getAtletaUuid(),
                                homeManB.getAtletaUuid(),
                                homeManC.getAtletaUuid()
                        ),
                        Set.of()
                )
        );

        assertEquals(
                "En convocatoria mixta cada equipo debe quedar balanceado por genero (diferencia maxima de 1)",
                exception.getMessage()
        );
    }

    @Test
    void validateGenderAssignmentRules_RejectsGenderSpecificCategoryMismatch() {
        Match womenOnly = matchWithCategory(MatchGenderCategory.SOLO_MUJERES);
        PlayerProfile man = player(GenderType.MASCULINO);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> policy.validateGenderAssignmentRules(
                        womenOnly,
                        List.of(matchPlayer(man)),
                        Set.of(man.getAtletaUuid()),
                        Set.of()
                )
        );

        assertEquals("En convocatoria solo mujeres no se permiten hombres en los equipos", exception.getMessage());
    }

    private Match matchWithCategory(MatchGenderCategory category) {
        Match match = new Match();
        match.setCategoriaGenero(category);
        return match;
    }

    private MatchPlayer matchPlayer(PlayerProfile player) {
        MatchPlayer matchPlayer = new MatchPlayer();
        matchPlayer.setPlayer(player);
        return matchPlayer;
    }

    private PlayerProfile player(GenderType gender) {
        UUID uuid = UUID.randomUUID();

        Athlete athlete = new Athlete();
        athlete.setAtletaUuid(uuid);
        athlete.setGenero(gender);

        PlayerProfile profile = new PlayerProfile();
        profile.setAtletaUuid(uuid);
        profile.setAthlete(athlete);

        return profile;
    }
}
