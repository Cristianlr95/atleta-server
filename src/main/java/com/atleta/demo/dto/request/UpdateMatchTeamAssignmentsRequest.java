package com.atleta.demo.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UpdateMatchTeamAssignmentsRequest {

    @NotNull(message = "El actor responsable es obligatorio")
    private UUID actorUuid;

    @NotNull(message = "La lista de jugadores local es obligatoria")
    private List<UUID> homePlayerUuids = new ArrayList<>();

    @NotNull(message = "La lista de jugadores visita es obligatoria")
    private List<UUID> awayPlayerUuids = new ArrayList<>();

    public UUID getActorUuid() {
        return actorUuid;
    }

    public void setActorUuid(UUID actorUuid) {
        this.actorUuid = actorUuid;
    }

    public List<UUID> getHomePlayerUuids() {
        return homePlayerUuids;
    }

    public void setHomePlayerUuids(List<UUID> homePlayerUuids) {
        this.homePlayerUuids = homePlayerUuids;
    }

    public List<UUID> getAwayPlayerUuids() {
        return awayPlayerUuids;
    }

    public void setAwayPlayerUuids(List<UUID> awayPlayerUuids) {
        this.awayPlayerUuids = awayPlayerUuids;
    }
}
