package com.atleta.demo.repository;

import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Team;
import com.atleta.demo.entity.TeamInvite;
import com.atleta.demo.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamInviteRepository extends JpaRepository<TeamInvite, Long> {
    @Query("""
        SELECT ti FROM TeamInvite ti
        WHERE (ti.requester = :player OR ti.target = :player)
        ORDER BY ti.createdAt DESC
    """)
    List<TeamInvite> findByPlayer(@Param("player") PlayerProfile player);

    boolean existsByTeamAndTargetAndStatus(Team team, PlayerProfile target, RequestStatus status);

    long deleteByTeam(Team team);
}
