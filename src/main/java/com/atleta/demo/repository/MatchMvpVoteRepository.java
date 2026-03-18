package com.atleta.demo.repository;

import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.MatchMvpVote;
import com.atleta.demo.entity.PlayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchMvpVoteRepository extends JpaRepository<MatchMvpVote, Long> {

    List<MatchMvpVote> findByMatch(Match match);

    Optional<MatchMvpVote> findByMatchAndVoter(Match match, PlayerProfile voter);

    long countByMatch(Match match);

    @Query("SELECT vote.votedUser.atletaUuid, COUNT(vote) " +
           "FROM MatchMvpVote vote " +
           "WHERE vote.match = :match " +
           "GROUP BY vote.votedUser.atletaUuid")
    List<Object[]> countVotesGroupedByVotedUser(@Param("match") Match match);
}
