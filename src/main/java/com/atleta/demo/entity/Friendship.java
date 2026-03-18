package com.atleta.demo.entity;

import com.atleta.demo.enums.RequestStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "friendships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"requester_user_id", "target_user_id"})
})
public class Friendship extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private PlayerProfile requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private PlayerProfile target;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RequestStatus status = RequestStatus.PENDIENTE;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public Friendship() {
    }

    public Friendship(PlayerProfile requester, PlayerProfile target) {
        this.requester = requester;
        this.target = target;
        this.status = RequestStatus.PENDIENTE;
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
