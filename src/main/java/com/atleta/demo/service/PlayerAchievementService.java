package com.atleta.demo.service;

import com.atleta.demo.dto.response.PlayerAchievementResponse;
import com.atleta.demo.entity.MatchEvent;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.enums.AchievementTier;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.enums.MatchTeamSide;
import com.atleta.demo.repository.MatchEventRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchRepository;
import com.atleta.demo.repository.PlayerProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Calculates competitive badges from finalized, auditable career data. */
@Service
public class PlayerAchievementService {
    private static final int[] GOALS = {5, 15, 30};
    private static final int[] ASSISTS = {5, 12, 25};
    private static final int[] MATCHES = {10, 30, 75};
    private static final int[] CREATED_MATCHES = {3, 10, 25};
    private static final int[] WINS = {5, 15, 40};

    private final PlayerProfileRepository playerProfileRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;

    public PlayerAchievementService(PlayerProfileRepository playerProfileRepository,
                                    MatchEventRepository matchEventRepository,
                                    MatchPlayerRepository matchPlayerRepository,
                                    MatchRepository matchRepository) {
        this.playerProfileRepository = playerProfileRepository;
        this.matchEventRepository = matchEventRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchRepository = matchRepository;
    }

    @Transactional(readOnly = true)
    public List<PlayerAchievementResponse> getAchievements(UUID playerUuid) {
        PlayerProfile player = playerProfileRepository.findById(playerUuid)
                .orElseThrow(() -> new EntityNotFoundException("Perfil de jugador no encontrado"));

        long goals = confirmedFinalized(matchEventRepository.findGoalsByPlayer(player));
        long assists = confirmedFinalized(matchEventRepository.findAssistsByPlayer(player));
        List<MatchPlayer> finalizedAppearances = matchPlayerRepository.findByPlayer(player).stream()
                .filter(appearance -> Boolean.TRUE.equals(appearance.getConfirmado()))
                .filter(appearance -> appearance.getMatch().getEstado() == MatchStatus.FINALIZADO)
                .toList();
        long wins = finalizedAppearances.stream().filter(this::isWin).count();
        long created = matchRepository.findByCreador(player).stream()
                .filter(match -> match.getEstado() != MatchStatus.INVALIDO)
                .count();

        return List.of(
                achievement("TOP_SCORER", "Goleador", "Marca goles en partidos finalizados.", "goles", goals, GOALS),
                achievement("TOP_ASSIST", "Asistente máximo", "Genera goles para tu equipo.", "asistencias", assists, ASSISTS),
                achievement("MATCH_VETERAN", "Partidos jugados", "Compite y confirma presencia en partidos finalizados.", "partidos", finalizedAppearances.size(), MATCHES),
                achievement("MATCH_CREATOR", "Creador de partidos", "Organiza encuentros válidos para la comunidad.", "partidos creados", created, CREATED_MATCHES),
                achievement("MATCH_WINNER", "Partidos ganados", "Acumula victorias en partidos finalizados.", "victorias", wins, WINS)
        );
    }

    private long confirmedFinalized(List<MatchEvent> events) {
        return events.stream()
                .filter(event -> event.getMatch().getEstado() == MatchStatus.FINALIZADO)
                .filter(event -> Boolean.TRUE.equals(event.getConfirmedByHome()) && Boolean.TRUE.equals(event.getConfirmedByAway()))
                .count();
    }

    private boolean isWin(MatchPlayer appearance) {
        int local = appearance.getMatch().getFinalScoreLocal();
        int away = appearance.getMatch().getFinalScoreAway();
        return (appearance.getTeamSide() == MatchTeamSide.LOCAL && local > away)
                || (appearance.getTeamSide() == MatchTeamSide.VISITA && away > local);
    }

    private PlayerAchievementResponse achievement(String code, String title, String description, String metric,
                                                   long value, int[] thresholds) {
        AchievementTier tier = null;
        for (int index = thresholds.length - 1; index >= 0; index--) {
            if (value >= thresholds[index]) {
                tier = AchievementTier.values()[index];
                break;
            }
        }
        int next = tier == AchievementTier.GOLD ? thresholds[2]
                : tier == AchievementTier.SILVER ? thresholds[2]
                : tier == AchievementTier.BRONZE ? thresholds[1]
                : thresholds[0];
        int progress = Math.min(100, (int) Math.round((value * 100d) / next));
        return new PlayerAchievementResponse(code, title, description, metric, value, next, progress, tier, tier != null);
    }
}
