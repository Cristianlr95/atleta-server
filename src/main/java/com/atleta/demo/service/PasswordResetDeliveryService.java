package com.atleta.demo.service;

import java.time.Instant;

public interface PasswordResetDeliveryService {
    void sendResetLink(String email, String rawToken, Instant expiresAt);
}
