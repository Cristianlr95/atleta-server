package com.atleta.demo.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateMatchInviteRequest {
    @NotNull
    private Long matchId;
    private Long teamId;
    @NotNull
    private UUID requesterUuid;
    @NotNull
    private UUID targetUuid;
    @Size(max = 255)
    private String message;

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public UUID getRequesterUuid() {
        return requesterUuid;
    }

    public void setRequesterUuid(UUID requesterUuid) {
        this.requesterUuid = requesterUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public void setTargetUuid(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
