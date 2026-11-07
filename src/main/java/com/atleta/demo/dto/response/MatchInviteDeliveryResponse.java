package com.atleta.demo.dto.response;

import java.util.UUID;

public class MatchInviteDeliveryResponse {

    public enum DeliveryStatus {
        SENT,
        ALREADY_SENT,
        ALREADY_ACCEPTED,
        FAILED
    }

    private final UUID targetUuid;
    private final DeliveryStatus status;
    private final SocialRequestResponse invitation;
    private final String message;

    public MatchInviteDeliveryResponse(
            UUID targetUuid,
            DeliveryStatus status,
            SocialRequestResponse invitation,
            String message
    ) {
        this.targetUuid = targetUuid;
        this.status = status;
        this.invitation = invitation;
        this.message = message;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public SocialRequestResponse getInvitation() {
        return invitation;
    }

    public String getMessage() {
        return message;
    }
}
