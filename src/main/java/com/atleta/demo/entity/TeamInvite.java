package com.atleta.demo.entity;

import com.atleta.demo.enums.RequestStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_invites")
public class TeamInvite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private PlayerProfile requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private PlayerProfile target;

    @Column(name = "message")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RequestStatus status = RequestStatus.PENDIENTE;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public TeamInvite() {
    }

    public TeamInvite(Team team, PlayerProfile requester, PlayerProfile target, String message) {
        this.team = team;
        this.requester = requester;
        this.target = target;
        this.message = message;
        this.status = RequestStatus.PENDIENTE;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public PlayerProfile getRequester() {
        return requester;
    }

    public void setRequester(PlayerProfile requester) {
        this.requester = requester;
    }

    public PlayerProfile getTarget() {
        return target;
    }

    public void setTarget(PlayerProfile target) {
        this.target = target;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }
}
