package com.atleta.demo.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class VoteMatchMvpRequest {

    @NotNull(message = "El jugador votado es obligatorio")
    private UUID votedUserId;

    public UUID getVotedUserId() {
        return votedUserId;
    }

    public void setVotedUserId(UUID votedUserId) {
        this.votedUserId = votedUserId;
    }
}
