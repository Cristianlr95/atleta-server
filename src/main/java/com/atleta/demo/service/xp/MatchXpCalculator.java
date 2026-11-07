package com.atleta.demo.service.xp;

import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.enums.MatchTeamSide;
import java.util.Locale;

public final class MatchXpCalculator {
    private MatchXpCalculator() {}

    public static int calculate(MatchPlayer player, int goals, int local, int away, MatchTeamSide side) {
        int xp = 5;
        if (local != away) {
            boolean won = (side == MatchTeamSide.LOCAL && local > away)
                    || (side == MatchTeamSide.VISITA && away > local);
            xp += won ? 5 : 2;
        }
        String position = player.getPosition() != null && player.getPosition().getNombre() != null
                ? player.getPosition().getNombre().toUpperCase(Locale.ROOT) : "";
        int goalXp = position.contains("DEF") ? 6 : position.contains("MED") ? 5 : 4;
        return Math.min(25, xp + Math.min(25, Math.max(0, goals)) * goalXp);
    }
}
