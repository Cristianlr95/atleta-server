package com.atleta.demo.dto.response;

import java.util.List;

public class OrchestratedMatchCreationResponse {

    private MatchResponse match;
    private List<SocialRequestResponse> invitations;
    private boolean replayed;

    public OrchestratedMatchCreationResponse() {
    }

    public OrchestratedMatchCreationResponse(
            MatchResponse match,
            List<SocialRequestResponse> invitations,
            boolean replayed
    ) {
        this.match = match;
        this.invitations = invitations;
        this.replayed = replayed;
    }

    public MatchResponse getMatch() {
        return match;
    }

    public void setMatch(MatchResponse match) {
        this.match = match;
    }

    public List<SocialRequestResponse> getInvitations() {
        return invitations;
    }

    public void setInvitations(List<SocialRequestResponse> invitations) {
        this.invitations = invitations;
    }

    public boolean isReplayed() {
        return replayed;
    }

    public void setReplayed(boolean replayed) {
        this.replayed = replayed;
    }
}
