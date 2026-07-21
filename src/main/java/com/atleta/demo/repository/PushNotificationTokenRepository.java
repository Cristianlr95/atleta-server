package com.atleta.demo.repository;

import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.PushNotificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface PushNotificationTokenRepository extends JpaRepository<PushNotificationToken, Long> {
    Optional<PushNotificationToken> findByToken(String token);

    Optional<PushNotificationToken> findByRecipientAndDeviceId(PlayerProfile recipient, String deviceId);

    List<PushNotificationToken> findByRecipientAndActiveTrue(PlayerProfile recipient);
}
