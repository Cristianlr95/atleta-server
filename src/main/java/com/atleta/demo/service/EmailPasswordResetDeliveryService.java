package com.atleta.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class EmailPasswordResetDeliveryService implements PasswordResetDeliveryService {
    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final String fromAddress;

    public EmailPasswordResetDeliveryService(
            JavaMailSender mailSender,
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl,
            @Value("${app.mail.from:no-reply@atleta.local}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl.replaceAll("/$", "");
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendResetLink(String email, String rawToken, Instant expiresAt) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Restablece tu contrasena de Atleta");
        message.setText("Abre este enlace para crear una nueva contrasena:\n\n"
                + frontendUrl + "/password-reset?token=" + rawToken
                + "\n\nEl enlace vence en " + expiresAt + ". Si no lo solicitaste, ignora este correo.");
        mailSender.send(message);
    }
}
