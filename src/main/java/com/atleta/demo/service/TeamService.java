package com.atleta.demo.service;

import com.atleta.demo.dto.request.CreateTeamRequest;
import com.atleta.demo.dto.response.PlayerProfileResponse;
import com.atleta.demo.dto.response.TeamActiveMemberResponse;
import com.atleta.demo.dto.response.TeamResponse;
import com.atleta.demo.dto.response.TeamStatsResponse;
import com.atleta.demo.entity.PlayerPosition;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Team;
import com.atleta.demo.entity.TeamMember;
import com.atleta.demo.entity.TeamStats;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.repository.PlayerProfileRepository;
import com.atleta.demo.repository.PlayerPositionRepository;
import com.atleta.demo.repository.MatchInviteRepository;
import com.atleta.demo.repository.TeamMemberRepository;
import com.atleta.demo.repository.TeamRepository;
import com.atleta.demo.repository.TeamInviteRepository;
import com.atleta.demo.repository.TeamStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class TeamService {
    private static final Logger logger = LoggerFactory.getLogger(TeamService.class);
    private static final long MAX_LOGO_SIZE_BYTES = 3 * 1024 * 1024;
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/png", "image/jpeg", "image/webp");
    private static final Path TEAM_LOGOS_DIR = Path.of("uploads", "team-logos");

    private final TeamRepository teamRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamStatsRepository teamStatsRepository;
    private final PlayerPositionRepository playerPositionRepository;
    private final TeamInviteRepository teamInviteRepository;
    private final MatchInviteRepository matchInviteRepository;

    public TeamService(TeamRepository teamRepository,
                       PlayerProfileRepository playerProfileRepository,
                       TeamMemberRepository teamMemberRepository,
                       TeamStatsRepository teamStatsRepository,
                       PlayerPositionRepository playerPositionRepository,
                       TeamInviteRepository teamInviteRepository,
                       MatchInviteRepository matchInviteRepository) {
        this.teamRepository = teamRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamStatsRepository = teamStatsRepository;
        this.playerPositionRepository = playerPositionRepository;
        this.teamInviteRepository = teamInviteRepository;
        this.matchInviteRepository = matchInviteRepository;
    }

    public TeamResponse createTeam(CreateTeamRequest request) {
        String teamName = request.getNombre() == null ? "" : request.getNombre().trim();
        if (teamName.isBlank()) {
            throw new IllegalArgumentException("El nombre del equipo es obligatorio");
        }

        if (teamRepository.existsByNombre(teamName)) {
            throw new IllegalArgumentException("Ya existe un equipo con ese nombre");
        }

        PlayerProfile creator = playerProfileRepository.findById(request.getCreadorUuid())
                .orElseThrow(() -> new IllegalArgumentException("Creador no encontrado: " + request.getCreadorUuid()));

        Team team = new Team();
        team.setNombre(teamName);
        team.setCreador(creator);
        team.setLogoUrl(normalizeBlank(request.getLogoUrl()));
        team.setAnioFundacion(request.getAnioFundacion());
        team = teamRepository.saveAndFlush(team);

        TeamStats teamStats = new TeamStats(team);
        teamStatsRepository.save(teamStats);
        team.setStats(teamStats);

        TeamMember creatorMembership = new TeamMember(team, creator, PlayerRole.CAPITAN);
        creatorMembership.setActivo(true);
        teamMemberRepository.save(creatorMembership);
        team.addMember(creatorMembership);

        return toTeamResponse(team);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getTeamsByPlayer(UUID playerUuid) {
        PlayerProfile player = playerProfileRepository.findById(playerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + playerUuid));

        List<Team> activeMemberTeams = teamRepository.findTeamsByActiveMember(player);
        List<Team> createdTeams = teamRepository.findByCreadorAndArchivedFalse(player);

        Map<Long, Team> uniqueTeams = new LinkedHashMap<>();
        for (Team team : activeMemberTeams) {
            uniqueTeams.put(team.getId(), team);
        }
        for (Team team : createdTeams) {
            uniqueTeams.put(team.getId(), team);
        }

        List<TeamResponse> response = new ArrayList<>();
        for (Team team : uniqueTeams.values()) {
            response.add(toTeamResponse(team));
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getTeamsByCreator(UUID creatorUuid) {
        PlayerProfile creator = playerProfileRepository.findById(creatorUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + creatorUuid));

        return teamRepository.findByCreadorAndArchivedFalse(creator).stream()
                .map(this::toTeamResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamActiveMemberResponse> getActiveMembersByTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado: " + teamId));

        if (Boolean.TRUE.equals(team.getArchived())) {
            throw new IllegalArgumentException("Equipo no encontrado: " + teamId);
        }

        List<TeamMember> activeMembers = teamMemberRepository.findActiveByTeam(team);
        List<TeamActiveMemberResponse> response = new ArrayList<>();

        for (TeamMember member : activeMembers) {
            PlayerPosition primaryPosition = playerPositionRepository
                    .findPrimaryPositionByPlayer(member.getPlayer())
                    .orElse(null);

            response.add(new TeamActiveMemberResponse(
                    member.getPlayer().getAtletaUuid(),
                    member.getPlayer().getAlias(),
                    member.getRol(),
                    primaryPosition != null && primaryPosition.getPosition() != null ? primaryPosition.getPosition().getId() : null,
                    primaryPosition != null && primaryPosition.getPosition() != null ? primaryPosition.getPosition().getNombre() : null
            ));
        }

        return response;
    }

    public String storeTeamLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar una imagen");
        }

        if (file.getSize() > MAX_LOGO_SIZE_BYTES) {
            throw new IllegalArgumentException("El logo excede el tamano maximo permitido (3 MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Formato de imagen no permitido");
        }

        String extension = resolveExtension(contentType);
        String fileName = UUID.randomUUID() + "." + extension;

        try {
            Files.createDirectories(TEAM_LOGOS_DIR);
            Path target = TEAM_LOGOS_DIR.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo guardar el logo");
        }

        return "/uploads/team-logos/" + fileName;
    }

    public void deleteTeam(Long teamId, UUID actorUuid) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado: " + teamId));

        playerProfileRepository.findById(actorUuid)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + actorUuid));

        if (team.getCreador() == null || !team.getCreador().getAtletaUuid().equals(actorUuid)) {
            throw new SecurityException("Solo el creador del equipo puede eliminarlo");
        }

        if (Boolean.TRUE.equals(team.getArchived())) {
            return;
        }

        long deletedTeamInvites = teamInviteRepository.deleteByTeam(team);
        long deletedMatchInvites = matchInviteRepository.deleteByTeam(team);

        team.setArchived(true);
        team.setArchivedAt(LocalDateTime.now());
        teamRepository.save(team);

        logger.info("Equipo {} archivado por {}. Invitaciones eliminadas: teamInvites={}, matchInvites={}",
                teamId, actorUuid, deletedTeamInvites, deletedMatchInvites);
    }

    private TeamResponse toTeamResponse(Team team) {
        TeamResponse response = new TeamResponse();
        response.setId(team.getId());
        response.setNombre(team.getNombre());
        response.setLogoUrl(team.getLogoUrl());
        response.setAnioFundacion(team.getAnioFundacion());
        response.setCreatedAt(team.getCreatedAt());

        if (team.getCreador() != null) {
            response.setCreador(new PlayerProfileResponse(
                    team.getCreador().getAtletaUuid(),
                    team.getCreador().getAlias(),
                    team.getCreador().getAthlete() != null ? team.getCreador().getAthlete().getGenero() : null,
                    team.getCreador().getTrustScore(),
                    team.getCreador().getCreatedAt()
            ));
        }

        if (team.getStats() != null) {
            response.setStats(toTeamStatsResponse(team.getStats()));
        }

        return response;
    }

    private TeamStatsResponse toTeamStatsResponse(TeamStats stats) {
        return new TeamStatsResponse(
                stats.getId(),
                stats.getPartidosJugados(),
                stats.getPartidosGanados(),
                stats.getPartidosEmpatados(),
                stats.getPartidosPerdidos(),
                stats.getGolesFavor(),
                stats.getGolesContra(),
                stats.getDiferenciaGoles(),
                stats.getPuntos()
        );
    }

    private String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String resolveExtension(String contentType) {
        if ("image/png".equals(contentType)) {
            return "png";
        }
        if ("image/webp".equals(contentType)) {
            return "webp";
        }
        return "jpg";
    }
}
