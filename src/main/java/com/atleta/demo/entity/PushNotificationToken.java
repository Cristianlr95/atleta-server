package com.atleta.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "push_notification_tokens")
public class PushNotificationToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private PlayerProfile recipient;

    @Column(name = "token", nullable = false, length = 512, unique = true)
    private String token;

    @Column(name = "platform", nullable = false, length = 32)
    private String platform;

    @Column(name = "device_id", length = 191)
    private String deviceId;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    public PushNotificationToken() {
    }

    public PushNotificationToken(PlayerProfile recipient, String token, String platform, String deviceId) {
        this.recipient = recipient;
        this.token = token;
        this.platform = platform;
        this.deviceId = deviceId;
        this.active = true;
        this.lastSeenAt = LocalDateTime.now();
    }

    public PlayerProfile getRecipient() {
        return recipient;
    }

    public void setRecipient(PlayerProfile recipient) {
        this.recipient = recipient;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
