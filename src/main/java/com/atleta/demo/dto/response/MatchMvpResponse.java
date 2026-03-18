package com.atleta.demo.dto.response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MatchMvpResponse {

    private Long matchId;
    private LocalDateTime finalizedAt;
    private LocalDateTime closesAt;
    private boolean open;
    private UUID myVote;
    private UUID winnerUserId;
    private String winnerAlias;
    private List<MvpCandidateResponse> candidates = new ArrayList<>();
    private List<MvpTallyItemResponse> tally = new ArrayList<>();

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public LocalDateTime getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(LocalDateTime finalizedAt) {
        this.finalizedAt = finalizedAt;
    }

    public LocalDateTime getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(LocalDateTime closesAt) {
        this.closesAt = closesAt;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public UUID getMyVote() {
        return myVote;
    }

    public void setMyVote(UUID myVote) {
        this.myVote = myVote;
    }

    public UUID getWinnerUserId() {
        return winnerUserId;
    }

    public void setWinnerUserId(UUID winnerUserId) {
        this.winnerUserId = winnerUserId;
    }

    public String getWinnerAlias() {
        return winnerAlias;
    }

    public void setWinnerAlias(String winnerAlias) {
        this.winnerAlias = winnerAlias;
    }

    public List<MvpCandidateResponse> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<MvpCandidateResponse> candidates) {
        this.candidates = candidates;
    }

    public List<MvpTallyItemResponse> getTally() {
        return tally;
    }

    public void setTally(List<MvpTallyItemResponse> tally) {
        this.tally = tally;
    }

    public static class MvpCandidateResponse {
        private UUID userId;
        private String alias;

        public UUID getUserId() {
            return userId;
        }

        public void setUserId(UUID userId) {
            this.userId = userId;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }
    }

    public static class MvpTallyItemResponse {
        private UUID userId;
        private String alias;
        private long votes;

        public UUID getUserId() {
            return userId;
        }

        public void setUserId(UUID userId) {
            this.userId = userId;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public long getVotes() {
            return votes;
        }

        public void setVotes(long votes) {
            this.votes = votes;
        }
    }
}
