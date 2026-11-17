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
    private Long teamGoals;
    private Long teamMatches;
    public Long getTeamMatches() { return teamMatches; }
    public void setTeamMatches(Long teamMatches) { this.teamMatches = teamMatches; }
    private Long totalGoals;
    private Long teamAssists;
    private Long totalAssists;
    public Long getTeamAssists() { return teamAssists; }
    public void setTeamAssists(Long value) { this.teamAssists = value; }
    public Long getTotalAssists() { return totalAssists; }
    public void setTotalAssists(Long value) { this.totalAssists = value; }

    public Long getTeamGoals() { return teamGoals; }
    public void setTeamGoals(Long teamGoals) { this.teamGoals = teamGoals; }
    public Long getTotalGoals() { return totalGoals; }
    public void setTotalGoals(Long totalGoals) { this.totalGoals = totalGoals; }

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
