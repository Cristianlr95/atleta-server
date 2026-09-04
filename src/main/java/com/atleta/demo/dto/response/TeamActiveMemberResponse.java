package com.atleta.demo.dto.response;

import com.atleta.demo.enums.PlayerRole;

import java.math.BigDecimal;
import java.util.UUID;

public class TeamActiveMemberResponse {
    private UUID playerUuid;
    private String alias;
    private PlayerRole rol;
    private Long primaryPositionId;
    private String primaryPositionName;
    private BigDecimal ovr;

    public TeamActiveMemberResponse() {
    }

    public TeamActiveMemberResponse(UUID playerUuid, String alias, PlayerRole rol, Long primaryPositionId, String primaryPositionName) {
        this(playerUuid, alias, rol, primaryPositionId, primaryPositionName, null);
    }

    public TeamActiveMemberResponse(UUID playerUuid, String alias, PlayerRole rol, Long primaryPositionId,
                                    String primaryPositionName, BigDecimal ovr) {
        this.playerUuid = playerUuid;
        this.alias = alias;
        this.rol = rol;
        this.primaryPositionId = primaryPositionId;
        this.primaryPositionName = primaryPositionName;
        this.ovr = ovr;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public PlayerRole getRol() {
        return rol;
    }

    public void setRol(PlayerRole rol) {
        this.rol = rol;
    }

    public Long getPrimaryPositionId() {
        return primaryPositionId;
    }

    public void setPrimaryPositionId(Long primaryPositionId) {
        this.primaryPositionId = primaryPositionId;
    }

    public String getPrimaryPositionName() {
        return primaryPositionName;
    }

    public void setPrimaryPositionName(String primaryPositionName) {
        this.primaryPositionName = primaryPositionName;
    }

    public BigDecimal getOvr() {
        return ovr;
    }

    public void setOvr(BigDecimal ovr) {
        this.ovr = ovr;
    }
}
