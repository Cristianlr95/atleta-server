package com.atleta.demo.service;

import com.atleta.demo.dto.request.CreateMatchInvitesBatchRequest;
import com.atleta.demo.dto.request.CreateMatchOrchestratedRequest;
import com.atleta.demo.dto.response.MatchResponse;
import com.atleta.demo.dto.response.OrchestratedMatchCreationResponse;
import com.atleta.demo.dto.response.SocialRequestResponse;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Team;
import com.atleta.demo.repository.MatchRepository;
import com.atleta.demo.repository.PlayerProfileRepository;
import com.atleta.demo.repository.TeamMemberRepository;
import com.atleta.demo.repository.TeamRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
public class OrchestratedMatchCreationService {

    private final MatchRepository matchRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MatchService matchService;
    private final SocialService socialService;

    public OrchestratedMatchCreationService(
            MatchRepository matchRepository,
            PlayerProfileRepository playerProfileRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            MatchService matchService,
            SocialService socialService
    ) {
        this.matchRepository = matchRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.matchService = matchService;
        this.socialService = socialService;
    }

    /**
     * Serializes commands in this node while the database uniqueness constraint
     * protects the same creator/key pair across nodes.
     */
    @Transactional
    public synchronized OrchestratedMatchCreationResponse create(
            CreateMatchOrchestratedRequest request,
            UUID actorUuid,
            String rawIdempotencyKey
    ) {
        String idempotencyKey = normalizeIdempotencyKey(rawIdempotencyKey);
        var existing = matchRepository.findByCreadorAtletaUuidAndCreationIdempotencyKey(actorUuid, idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), actorUuid);
        }

        PlayerProfile actor = playerProfileRepository.findById(actorUuid)
                .orElseThrow(() -> new IllegalArgumentException("Creador no encontrado"));
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));
        boolean canUseTeam = actor.equals(team.getCreador()) || teamMemberRepository.isActiveMember(team, actor);
        if (!canUseTeam || Boolean.TRUE.equals(team.getArchived())) {
            throw new AccessDeniedException("No puedes crear un partido con este equipo");
        }

        LinkedHashSet<UUID> distinctTargets = new LinkedHashSet<>(request.getTargetUuids());
        distinctTargets.remove(null);
        distinctTargets.remove(actorUuid);
        long existingTargets = playerProfileRepository.findAllById(distinctTargets).size();
        if (existingTargets != distinctTargets.size()) {
            throw new IllegalArgumentException("Uno o mas jugadores invitados no existen");
        }

        request.getMatch().setCreadorUuid(actorUuid);
        MatchResponse created = matchService.createMatch(request.getMatch());
        Match persisted = matchRepository.findById(created.getId())
                .orElseThrow(() -> new IllegalStateException("El partido creado no pudo recuperarse"));
        persisted.setCreationIdempotencyKey(idempotencyKey);
        matchRepository.saveAndFlush(persisted);

        matchService.addTeamToMatch(created.getId(), team.getId(), true, actorUuid);

        CreateMatchInvitesBatchRequest invitationsRequest = new CreateMatchInvitesBatchRequest();
        invitationsRequest.setMatchId(created.getId());
        invitationsRequest.setTeamId(team.getId());
        invitationsRequest.setRequesterUuid(actorUuid);
        invitationsRequest.setTargetUuids(List.copyOf(distinctTargets));
        invitationsRequest.setMessage(request.getInvitationMessage());
        List<SocialRequestResponse> invitations = distinctTargets.isEmpty()
                ? List.of()
                : socialService.createMatchInvitesBatch(invitationsRequest);

        return new OrchestratedMatchCreationResponse(
                matchService.getMatchById(created.getId()),
                invitations,
                false
        );
    }

    private OrchestratedMatchCreationResponse replay(Match match, UUID actorUuid) {
        return new OrchestratedMatchCreationResponse(
                matchService.getMatchById(match.getId()),
                socialService.getMatchInvitesByMatch(match.getId(), actorUuid),
                true
        );
    }

    private String normalizeIdempotencyKey(String rawKey) {
        if (rawKey == null) {
            throw new IllegalArgumentException("Idempotency-Key es obligatorio");
        }
        String normalized = rawKey.trim();
        if (normalized.length() < 8 || normalized.length() > 100) {
            throw new IllegalArgumentException("Idempotency-Key debe tener entre 8 y 100 caracteres");
        }
        return normalized;
    }
}
