package com.atleta.demo.dto.response;

import com.atleta.demo.enums.RequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class SocialRequestResponse {
    private Long id;
    private String type;
    private RequestStatus status;
    private UUID requesterUuid;
    private String requesterAlias;
    private UUID targetUuid;
    private String targetAlias;
    private Long teamId;
    private String teamName;
    private Long matchId;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public UUID getRequesterUuid() {
        return requesterUuid;
    }

    public void setRequesterUuid(UUID requesterUuid) {
        this.requesterUuid = requesterUuid;
    }

    public String getRequesterAlias() {
        return requesterAlias;
    }

    public void setRequesterAlias(String requesterAlias) {
        this.requesterAlias = requesterAlias;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public void setTargetUuid(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public String getTargetAlias() {
        return targetAlias;
    }

    public void setTargetAlias(String targetAlias) {
        this.targetAlias = targetAlias;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }
}
