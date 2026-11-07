package com.atleta.demo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateMatchOrchestratedRequest {

    @Valid
    @NotNull(message = "match es obligatorio")
    private CreateMatchRequest match;

    @NotNull(message = "teamId es obligatorio")
    private Long teamId;

    @NotNull(message = "targetUuids es obligatorio")
    private List<UUID> targetUuids = new ArrayList<>();

    private String invitationMessage;

    public CreateMatchRequest getMatch() {
        return match;
    }

    public void setMatch(CreateMatchRequest match) {
        this.match = match;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public List<UUID> getTargetUuids() {
        return targetUuids;
    }

    public void setTargetUuids(List<UUID> targetUuids) {
        this.targetUuids = targetUuids;
    }

    public String getInvitationMessage() {
        return invitationMessage;
    }

    public void setInvitationMessage(String invitationMessage) {
        this.invitationMessage = invitationMessage;
    }
}
