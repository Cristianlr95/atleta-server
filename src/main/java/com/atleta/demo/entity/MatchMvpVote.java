package com.atleta.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * Voto MVP por partido.
 * Un votante puede tener un solo voto por partido (upsert).
 */
@Entity
@Table(
        name = "match_mvp_votes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_match_mvp_votes_match_voter",
                        columnNames = {"match_id", "voter_user_id"}
                )
        }
)
public class MatchMvpVote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    @NotNull(message = "El partido es obligatorio")
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_user_id", nullable = false)
    @NotNull(message = "El votante es obligatorio")
    private PlayerProfile voter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voted_user_id", nullable = false)
    @NotNull(message = "El jugador votado es obligatorio")
    private PlayerProfile votedUser;

    public MatchMvpVote() {
        // JPA
    }

    public MatchMvpVote(Match match, PlayerProfile voter, PlayerProfile votedUser) {
        this.match = match;
        this.voter = voter;
        this.votedUser = votedUser;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public PlayerProfile getVoter() {
        return voter;
    }

    public void setVoter(PlayerProfile voter) {
        this.voter = voter;
    }

    public PlayerProfile getVotedUser() {
        return votedUser;
    }

    public void setVotedUser(PlayerProfile votedUser) {
        this.votedUser = votedUser;
    }
}
