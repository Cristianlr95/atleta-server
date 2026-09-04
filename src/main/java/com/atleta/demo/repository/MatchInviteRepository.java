package com.atleta.demo.repository;

import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchInvite;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Team;
import com.atleta.demo.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchInviteRepository extends JpaRepository<MatchInvite, Long> {
    @Query("""
        SELECT mi FROM MatchInvite mi
        WHERE (mi.requester = :player OR mi.target = :player)
        ORDER BY mi.createdAt DESC
    """)
    List<MatchInvite> findByPlayer(@Param("player") PlayerProfile player);

    @Query("""
        SELECT mi FROM MatchInvite mi
        LEFT JOIN FETCH mi.requester
        LEFT JOIN FETCH mi.target
        LEFT JOIN FETCH mi.team
        WHERE mi.match.id = :matchId
        ORDER BY mi.createdAt ASC
    """)
    List<MatchInvite> findByMatchId(@Param("matchId") Long matchId);

    Optional<MatchInvite> findTopByMatchAndTargetOrderByCreatedAtDesc(Match match, PlayerProfile target);

    Optional<MatchInvite> findFirstByMatchAndStatusOrderByRespondedAtAscCreatedAtAsc(
            Match match,
            RequestStatus status
    );

    List<MatchInvite> findByMatchAndStatusOrderByRespondedAtAscCreatedAtAsc(
            Match match,
            RequestStatus status
    );

    boolean existsByMatchAndTargetAndStatus(Match match, PlayerProfile target, RequestStatus status);

    boolean existsByTeam(Team team);

    long deleteByTeam(Team team);

    @Modifying
    @Query("""
        DELETE FROM MatchInvite mi
        WHERE mi.status = com.atleta.demo.enums.RequestStatus.PENDIENTE
          AND mi.match.fechaHoraProgramada < :cutoff
    """)
    int deletePendingInvitesForExpiredMatches(@Param("cutoff") java.time.LocalDateTime cutoff);
}
