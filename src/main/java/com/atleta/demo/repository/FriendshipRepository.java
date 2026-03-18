package com.atleta.demo.repository;

import com.atleta.demo.entity.Friendship;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    Optional<Friendship> findByRequesterAndTarget(PlayerProfile requester, PlayerProfile target);

    @Query("""
        SELECT f FROM Friendship f
        WHERE ((f.requester = :a AND f.target = :b) OR (f.requester = :b AND f.target = :a))
    """)
    List<Friendship> findPair(@Param("a") PlayerProfile a, @Param("b") PlayerProfile b);

    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.requester = :player OR f.target = :player)
        ORDER BY f.createdAt DESC
    """)
    List<Friendship> findByPlayer(@Param("player") PlayerProfile player);

    @Query("""
        SELECT COUNT(f) > 0 FROM Friendship f
        WHERE ((f.requester = :a AND f.target = :b) OR (f.requester = :b AND f.target = :a))
          AND f.status = :status
    """)
    boolean existsPairWithStatus(@Param("a") PlayerProfile a, @Param("b") PlayerProfile b, @Param("status") RequestStatus status);
}
