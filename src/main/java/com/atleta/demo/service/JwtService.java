package com.atleta.demo.service;

import com.atleta.demo.config.JwtProperties;
import com.atleta.demo.entity.Athlete;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;

    public JwtService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.jwtProperties = jwtProperties;
    }

    public String generateToken(Athlete athlete) {
        return generateToken(athlete, null);
    }

    public String generateToken(Athlete athlete, UUID sessionId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtProperties.getExpiration());

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
            .issuer(jwtProperties.getIssuer())
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(athlete.getAtletaUuid().toString())
            .claim("uuid", athlete.getAtletaUuid().toString())
            .claim("email", athlete.getEmail())
            .claim("auth_provider", athlete.getAuthProvider());

        if (sessionId != null) {
            claims.claim("sid", sessionId.toString());
        }

        return jwtEncoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(),
            claims.build()
        )).getTokenValue();
    }

    public UUID validateToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return UUID.fromString(jwt.getSubject());
        } catch (Exception e) {
            throw new IllegalArgumentException("Token invalido", e);
        }
    }
}
