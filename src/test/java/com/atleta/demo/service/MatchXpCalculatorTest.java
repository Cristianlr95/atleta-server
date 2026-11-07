package com.atleta.demo.service;

import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.enums.MatchTeamSide;
import com.atleta.demo.service.xp.MatchXpCalculator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchXpCalculatorTest {
    @Test void balancesParticipationAndLimitsExceptionalGames() {
        MatchPlayer player = new MatchPlayer();
        assertEquals(5, MatchXpCalculator.calculate(player, 0, 1, 1, MatchTeamSide.LOCAL));
        assertEquals(10, MatchXpCalculator.calculate(player, 0, 2, 1, MatchTeamSide.LOCAL));
        assertEquals(7, MatchXpCalculator.calculate(player, 0, 1, 2, MatchTeamSide.LOCAL));
        assertEquals(25, MatchXpCalculator.calculate(player, Integer.MAX_VALUE, 2, 1, MatchTeamSide.LOCAL));
    }
}
