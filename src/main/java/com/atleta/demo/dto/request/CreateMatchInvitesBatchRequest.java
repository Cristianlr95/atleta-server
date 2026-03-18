package com.atleta.demo.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class CreateMatchInvitesBatchRequest {

    @NotNull(message = "matchId es obligatorio")
    private Long matchId;

    private Long teamId;

    @NotNull(message = "requesterUuid es obligatorio")
    private UUID requesterUuid;

    @NotEmpty(message = "targetUuids no puede estar vacio")
    private List<UUID> targetUuids;

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

    public List<UUID> getTargetUuids() {
        return targetUuids;
    }

    public void setTargetUuids(List<UUID> targetUuids) {
        this.targetUuids = targetUuids;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
