package com.atleta.demo.dto.response;

import java.util.UUID;

public class SocialPlayerLookupResponse {
    private UUID atletaUuid;
    private String alias;
    private String athleteName;
    private String athleteEmail;

    public SocialPlayerLookupResponse() {
    }

    public SocialPlayerLookupResponse(UUID atletaUuid, String alias, String athleteName, String athleteEmail) {
        this.atletaUuid = atletaUuid;
        this.alias = alias;
        this.athleteName = athleteName;
        this.athleteEmail = athleteEmail;
    }

    public UUID getAtletaUuid() {
        return atletaUuid;
    }

    public void setAtletaUuid(UUID atletaUuid) {
        this.atletaUuid = atletaUuid;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAthleteName() {
        return athleteName;
    }

    public void setAthleteName(String athleteName) {
        this.athleteName = athleteName;
    }

    public String getAthleteEmail() {
        return athleteEmail;
    }

    public void setAthleteEmail(String athleteEmail) {
        this.athleteEmail = athleteEmail;
    }
}
