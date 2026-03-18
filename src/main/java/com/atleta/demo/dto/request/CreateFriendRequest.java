package com.atleta.demo.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreateFriendRequest {
    @NotNull
    private UUID requesterUuid;
    @NotNull
    private UUID targetUuid;

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
}
