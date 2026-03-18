package com.atleta.demo.service;

import com.atleta.demo.dto.response.MatchMvpResponse;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchMvpVote;
import com.atleta.demo.entity.MatchPlayer;
import com.atleta.demo.entity.PlayerHistory;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.PlayerPosition;
import com.atleta.demo.repository.MatchMvpVoteRepository;
import com.atleta.demo.repository.MatchPlayerRepository;
import com.atleta.demo.repository.MatchRepository;
import com.atleta.demo.repository.PlayerHistoryRepository;
import com.atleta.demo.repository.PlayerPositionRepository;
import com.atleta.demo.repository.PlayerProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MatchMvpService {

    private static final long MVP_VOTING_WINDOW_HOURS = 3;
    private static final int MVP_BONUS_XP = 10;

    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchMvpVoteRepository matchMvpVoteRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerHistoryRepository playerHistoryRepository;
    private final PlayerPositionRepository playerPositionRepository;
    private final boolean mvpXpBonusEnabled;

    public MatchMvpService(
            MatchRepository matchRepository,
            MatchPlayerRepository matchPlayerRepository,
            MatchMvpVoteRepository matchMvpVoteRepository,
            PlayerProfileRepository playerProfileRepository,
            PlayerHistoryRepository playerHistoryRepository,
            PlayerPositionRepository playerPositionRepository,
            @Value("${features.mvp-xp-bonus-enabled:false}") boolean mvpXpBonusEnabled
    ) {
        this.matchRepository = matchRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchMvpVoteRepository = matchMvpVoteRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.playerHistoryRepository = playerHistoryRepository;
        this.playerPositionRepository = playerPositionRepository;
        this.mvpXpBonusEnabled = mvpXpBonusEnabled;
    }

    @Transactional(readOnly = false)
    public MatchMvpResponse getMvpState(Long matchId, UUID voterUserId) {
        Match match = getMatchOrThrow(matchId);
        ensureFinalized(match);

        LocalDateTime closesAt = resolveVotingCloseAt(match);
        maybeCloseVoting(match, closesAt);

        Map<UUID, PlayerProfile> candidatesByUuid = resolveEligibleParticipants(match);

        List<MatchMvpVote> votes = matchMvpVoteRepository.findByMatch(match);
        return buildResponse(match, closesAt, voterUserId, candidatesByUuid, votes);
    }

    public MatchMvpResponse vote(Long matchId, UUID voterUserId, UUID votedUserId) {
        if (voterUserId == null) {
            throw new IllegalArgumentException("Se requiere voterUserId");
        }
        if (votedUserId == null) {
            throw new IllegalArgumentException("Se requiere votedUserId");
        }

        Match match = getMatchOrThrow(matchId);
        ensureFinalized(match);
        LocalDateTime closesAt = resolveVotingCloseAt(match);
        if (LocalDateTime.now().isAfter(closesAt) || LocalDateTime.now().isEqual(closesAt)) {
            maybeCloseVoting(match, closesAt);
            throw new IllegalArgumentException("La votacion MVP ya cerro (ventana de 3 horas vencida)");
        }

        PlayerProfile voter = playerProfileRepository.findById(voterUserId)
                .orElseThrow(() -> new IllegalArgumentException("Votante no encontrado: " + voterUserId));
        PlayerProfile votedUser = playerProfileRepository.findById(votedUserId)
                .orElseThrow(() -> new IllegalArgumentException("Jugador votado no encontrado: " + votedUserId));

        Map<UUID, PlayerProfile> candidatesByUuid = resolveEligibleParticipants(match);

        if (!candidatesByUuid.containsKey(voterUserId)) {
            throw new IllegalArgumentException("Solo participantes confirmados pueden votar MVP");
        }
        if (!candidatesByUuid.containsKey(votedUserId)) {
            throw new IllegalArgumentException("Solo se puede votar por participantes confirmados del partido");
        }

        MatchMvpVote vote = matchMvpVoteRepository.findByMatchAndVoter(match, voter)
                .orElseGet(() -> new MatchMvpVote(match, voter, votedUser));
        vote.setVotedUser(votedUser);
        matchMvpVoteRepository.save(vote);

        List<MatchMvpVote> votes = matchMvpVoteRepository.findByMatch(match);

        // Cierre anticipado: si todos los participantes confirmados ya votaron, se define MVP sin esperar 3 horas.
        maybeCloseVoting(match, closesAt, votes, candidatesByUuid.size());

        return buildResponse(match, closesAt, voterUserId, candidatesByUuid, votes);
    }

    private MatchMvpResponse buildResponse(
            Match match,
            LocalDateTime closesAt,
            UUID voterUserId,
            Map<UUID, PlayerProfile> candidatesByUuid,
            List<MatchMvpVote> votes
    ) {
        MatchMvpResponse response = new MatchMvpResponse();
        response.setMatchId(match.getId());
        response.setFinalizedAt(match.getFinalizedAt());
        response.setClosesAt(closesAt);
        response.setOpen(LocalDateTime.now().isBefore(closesAt) && match.getMvpUser() == null);

        List<MatchMvpResponse.MvpCandidateResponse> candidates = new ArrayList<>();
        for (PlayerProfile profile : candidatesByUuid.values()) {
            MatchMvpResponse.MvpCandidateResponse candidate = new MatchMvpResponse.MvpCandidateResponse();
            candidate.setUserId(profile.getAtletaUuid());
            candidate.setAlias(profile.getAlias());
            candidates.add(candidate);
        }
        response.setCandidates(candidates);

        Map<UUID, Long> tally = new HashMap<>();
        for (MatchMvpVote vote : votes) {
            UUID votedUuid = vote.getVotedUser().getAtletaUuid();
            tally.put(votedUuid, tally.getOrDefault(votedUuid, 0L) + 1L);
            if (voterUserId != null && vote.getVoter().getAtletaUuid().equals(voterUserId)) {
                response.setMyVote(votedUuid);
            }
        }

        List<MatchMvpResponse.MvpTallyItemResponse> tallyItems = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : tally.entrySet()) {
            MatchMvpResponse.MvpTallyItemResponse item = new MatchMvpResponse.MvpTallyItemResponse();
            item.setUserId(entry.getKey());
            item.setVotes(entry.getValue());
            PlayerProfile profile = candidatesByUuid.get(entry.getKey());
            item.setAlias(profile != null ? profile.getAlias() : entry.getKey().toString());
            tallyItems.add(item);
        }
        tallyItems.sort(Comparator.comparingLong(MatchMvpResponse.MvpTallyItemResponse::getVotes).reversed());
        response.setTally(tallyItems);

        if (match.getMvpUser() != null) {
            response.setWinnerUserId(match.getMvpUser().getAtletaUuid());
            response.setWinnerAlias(match.getMvpUser().getAlias());
        }

        return response;
    }

    private Match getMatchOrThrow(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));
    }

    private void ensureFinalized(Match match) {
        if (match.getFinalizedAt() == null) {
            throw new IllegalArgumentException("La votacion MVP solo esta disponible para partidos finalizados");
        }
    }

    private LocalDateTime resolveVotingCloseAt(Match match) {
        if (match.getMvpVotingClosedAt() != null) {
            return match.getMvpVotingClosedAt();
        }
        LocalDateTime closesAt = match.getFinalizedAt().plusHours(MVP_VOTING_WINDOW_HOURS);
        match.setMvpVotingClosedAt(closesAt);
        matchRepository.save(match);
        return closesAt;
    }

    private void maybeCloseVoting(Match match, LocalDateTime closesAt) {
        int confirmedCount = resolveEligibleParticipants(match).size();
        List<MatchMvpVote> votes = matchMvpVoteRepository.findByMatch(match);
        maybeCloseVoting(match, closesAt, votes, confirmedCount);
    }

    private Map<UUID, PlayerProfile> resolveEligibleParticipants(Match match) {
        Map<UUID, PlayerProfile> participants = matchPlayerRepository.findConfirmedPlayersByMatch(match).stream()
                .map(MatchPlayer::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(PlayerProfile::getAtletaUuid, p -> p, (a, b) -> a, HashMap::new));

        if (match.getCreador() != null && match.getCreador().getAtletaUuid() != null) {
            participants.putIfAbsent(match.getCreador().getAtletaUuid(), match.getCreador());
        }

        return participants;
    }

    private void maybeCloseVoting(
            Match match,
            LocalDateTime closesAt,
            List<MatchMvpVote> votes,
            int confirmedParticipants
    ) {
        if (match.getMvpUser() != null) {
            return;
        }

        boolean closedByTime = !LocalDateTime.now().isBefore(closesAt);
        boolean closedByFullParticipation = confirmedParticipants > 0 && hasEveryoneVoted(votes, confirmedParticipants);
        if (!closedByTime && !closedByFullParticipation) {
            return;
        }

        if (votes.isEmpty()) {
            return;
        }

        Map<UUID, Long> counts = votes.stream()
                .collect(Collectors.groupingBy(v -> v.getVotedUser().getAtletaUuid(), Collectors.counting()));
        long maxVotes = counts.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        List<UUID> tied = counts.entrySet().stream()
                .filter(e -> e.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        UUID winnerUuid = resolveTieBreak(match, tied);
        if (winnerUuid == null) {
            return;
        }

        PlayerProfile winner = playerProfileRepository.findById(winnerUuid).orElse(null);
        if (winner != null) {
            match.setMvpUser(winner);
            matchRepository.save(match);
            applyMvpBonusXpIfEnabled(match, winner);
        }
    }

    private boolean hasEveryoneVoted(List<MatchMvpVote> votes, int confirmedParticipants) {
        long uniqueVoters = votes.stream()
                .map(v -> v.getVoter().getAtletaUuid())
                .distinct()
                .count();
        return uniqueVoters >= confirmedParticipants;
    }

    private UUID resolveTieBreak(Match match, List<UUID> tied) {
        if (tied.isEmpty()) {
            return null;
        }

        return tied.stream()
                .max((a, b) -> {
                    PlayerProfile playerA = playerProfileRepository.findById(a).orElse(null);
                    PlayerProfile playerB = playerProfileRepository.findById(b).orElse(null);
                    int xpA = getMatchXp(match, playerA);
                    int xpB = getMatchXp(match, playerB);
                    if (xpA != xpB) {
                        return Integer.compare(xpA, xpB);
                    }

                    int goalsA = getMatchGoals(match, playerA);
                    int goalsB = getMatchGoals(match, playerB);
                    if (goalsA != goalsB) {
                        return Integer.compare(goalsA, goalsB);
                    }

                    return b.toString().compareTo(a.toString());
                })
                .orElse(null);
    }

    private int getMatchXp(Match match, PlayerProfile player) {
        if (player == null) {
            return 0;
        }
        return playerHistoryRepository.findByMatchAndPlayer(match, player)
                .map(PlayerHistory::getXpGanada)
                .orElse(0);
    }

    private int getMatchGoals(Match match, PlayerProfile player) {
        if (player == null) {
            return 0;
        }
        return playerHistoryRepository.findByMatchAndPlayer(match, player)
                .map(PlayerHistory::getGoles)
                .orElse(0);
    }

    private void applyMvpBonusXpIfEnabled(Match match, PlayerProfile winner) {
        if (!mvpXpBonusEnabled || match == null || winner == null) {
            return;
        }

        PlayerHistory winnerHistory = playerHistoryRepository.findByMatchAndPlayer(match, winner).orElse(null);
        if (winnerHistory == null || winnerHistory.getPosition() == null) {
            return;
        }

        int updatedRows = playerHistoryRepository.applyMvpBonusXpIfNotApplied(
                match.getId(),
                winner.getAtletaUuid(),
                MVP_BONUS_XP
        );
        if (updatedRows <= 0) {
            return;
        }

        PlayerPosition winnerPosition = playerPositionRepository
                .findByPlayerAndPosition(winner, winnerHistory.getPosition())
                .orElse(null);
        if (winnerPosition != null) {
            winnerPosition.addXp(MVP_BONUS_XP);
            playerPositionRepository.save(winnerPosition);
        }
    }
}
