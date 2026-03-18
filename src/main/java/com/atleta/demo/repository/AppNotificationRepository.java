package com.atleta.demo.repository;

import com.atleta.demo.entity.AppNotification;
import com.atleta.demo.entity.PlayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    @Query("""
        SELECT n FROM AppNotification n
        WHERE n.recipient = :recipient
        ORDER BY n.createdAt DESC
    """)
    List<AppNotification> findByRecipient(@Param("recipient") PlayerProfile recipient);

    @Query("""
        SELECT COUNT(n) FROM AppNotification n
        WHERE n.recipient = :recipient AND n.isRead = false
    """)
    long countUnreadByRecipient(@Param("recipient") PlayerProfile recipient);
}
