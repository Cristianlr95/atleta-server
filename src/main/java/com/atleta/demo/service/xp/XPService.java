package com.atleta.demo.service.xp;

import com.atleta.demo.enums.MatchResult;
import com.atleta.demo.enums.PlayerRole;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Motor de calculo de XP por match.
 * Servicio puro: no accede DB y solo transforma snapshot -> resultados.
 */
@Service
public class XPService {

    private static final int XP_PLAY_MATCH = 10;
    private static final int XP_WIN = 10;
    private static final int XP_LOSS = 5;
    private static final int XP_ASSIST = 8;

    private static final int XP_GOAL_FORWARD = 10;
    private static final int XP_GOAL_MIDFIELD = 12;
    private static final int XP_GOAL_DEFENSE = 15;

    private static final int XP_GK_CLEAN_SHEET = 20;
    private static final int XP_GK_LT_3_CONCEDED = 10;

    public List<XpResult> calculateXp(MatchXpInput input) {
        if (input == null) {
            throw new IllegalArgumentException("El snapshot de XP es obligatorio");
        }

        if (input.players() == null || input.players().isEmpty()) {
            return List.of();
        }

        List<XpResult> results = new ArrayList<>(input.players().size());
        for (PlayerXpSnapshot player : input.players()) {
            if (player == null) {
                continue;
            }

            XpBreakdown breakdown = input.validMatch()
                    ? calculateForValidMatch(player)
                    : new XpBreakdown(0, 0, 0, 0, 0, 0);

            results.add(new XpResult(
                    player.userUuid(),
                    player.positionId(),
                    player.positionName(),
                    breakdown
            ));
        }
        return results;
    }

    private XpBreakdown calculateForValidMatch(PlayerXpSnapshot player) {
        if (!player.confirmedParticipation()) {
            return new XpBreakdown(0, 0, 0, 0, 0, 0);
        }

        int playXp = XP_PLAY_MATCH;
        int resultXp = resultXp(player.result());

        PositionBucket position = resolvePosition(player.positionName(), player.role());
        boolean isDt = player.role() == PlayerRole.DT || position == PositionBucket.DT;

        int goalXp = isDt ? 0 : goalXpByPosition(position, safe(player.goals()));
        int assistXp = isDt ? 0 : safe(player.assists()) * XP_ASSIST;
        int goalkeeperXp = goalkeeperXp(position, safe(player.goalsConceded()));

        int total = playXp + resultXp + goalXp + assistXp + goalkeeperXp;
        return new XpBreakdown(playXp, resultXp, goalXp, assistXp, goalkeeperXp, total);
    }

    private int resultXp(MatchResult result) {
        if (result == null) {
            return 0;
        }
        if (result == MatchResult.VICTORIA) {
            return XP_WIN;
        }
        if (result == MatchResult.DERROTA) {
            return XP_LOSS;
        }
        return 0;
    }

    private int goalXpByPosition(PositionBucket position, int goals) {
        if (goals <= 0) {
            return 0;
        }
        return switch (position) {
            case FORWARD -> goals * XP_GOAL_FORWARD;
            case MIDFIELD -> goals * XP_GOAL_MIDFIELD;
            case DEFENSE -> goals * XP_GOAL_DEFENSE;
            default -> 0;
        };
    }

    private int goalkeeperXp(PositionBucket position, int goalsConceded) {
        if (position != PositionBucket.GOALKEEPER) {
            return 0;
        }
        if (goalsConceded <= 0) {
            return XP_GK_CLEAN_SHEET;
        }
        if (goalsConceded < 3) {
            return XP_GK_LT_3_CONCEDED;
        }
        return 0;
    }

    private PositionBucket resolvePosition(String positionName, PlayerRole role) {
        if (role == PlayerRole.DT) {
            return PositionBucket.DT;
        }

        String normalized = positionName == null
                ? ""
                : positionName.trim().toLowerCase(Locale.ROOT);

        if (normalized.contains("dt")) {
            return PositionBucket.DT;
        }
        if (normalized.contains("port") || normalized.contains("arquer")) {
            return PositionBucket.GOALKEEPER;
        }
        if (normalized.contains("defen")) {
            return PositionBucket.DEFENSE;
        }
        if (normalized.contains("medio")) {
            return PositionBucket.MIDFIELD;
        }
        if (normalized.contains("delanter") || normalized.contains("ataq")) {
            return PositionBucket.FORWARD;
        }

        return PositionBucket.OTHER;
    }

    private int safe(int value) {
        return Math.max(0, value);
    }

    private enum PositionBucket {
        FORWARD,
        MIDFIELD,
        DEFENSE,
        GOALKEEPER,
        DT,
        OTHER
    }
}
