package com.atleta.demo.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class RespondRequestDecision {
    private UUID actorUuid;
    @NotNull
    private Boolean accept;

    public UUID getActorUuid() {
        return actorUuid;
    }

    public void setActorUuid(UUID actorUuid) {
        this.actorUuid = actorUuid;
    }

    public Boolean getAccept() {
        return accept;
    }

    public void setAccept(Boolean accept) {
        this.accept = accept;
    }
}
