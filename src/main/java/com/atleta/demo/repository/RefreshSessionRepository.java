package com.atleta.demo.repository;

import com.atleta.demo.entity.RefreshSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshSession> findByTokenHash(String tokenHash);

    boolean existsByIdAndRevokedAtIsNullAndExpiresAtAfter(UUID id, Instant now);

    @Modifying
    @Query("update RefreshSession session set session.revokedAt = :now where session.athlete.atletaUuid = :athleteUuid and session.revokedAt is null")
    int revokeAllByAthleteUuid(@Param("athleteUuid") UUID athleteUuid, @Param("now") Instant now);
}
