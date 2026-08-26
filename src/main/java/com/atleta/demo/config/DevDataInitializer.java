package com.atleta.demo.config;

import com.atleta.demo.entity.Athlete;
import com.atleta.demo.entity.PlayerPosition;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.Position;
import com.atleta.demo.entity.Team;
import com.atleta.demo.entity.TeamMember;
import com.atleta.demo.entity.TeamStats;
import com.atleta.demo.enums.GenderType;
import com.atleta.demo.enums.PlayerRole;
import com.atleta.demo.repository.AthleteRepository;
import com.atleta.demo.repository.PlayerPositionRepository;
import com.atleta.demo.repository.PlayerProfileRepository;
import com.atleta.demo.repository.PositionRepository;
import com.atleta.demo.repository.TeamMemberRepository;
import com.atleta.demo.repository.TeamRepository;
import com.atleta.demo.repository.TeamStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Crea cuentas reproducibles exclusivamente para el entorno de desarrollo.
 *
 * <p>La siembra es idempotente: nunca modifica usuarios que ya existan. No se
 * carga en testing, staging ni produccion.</p>
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev-seed", name = "enabled", havingValue = "true")
public class DevDataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DevDataInitializer.class);

    private final AthleteRepository athleteRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerPositionRepository playerPositionRepository;
    private final PositionRepository positionRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamStatsRepository teamStatsRepository;
    private final PasswordEncoder passwordEncoder;
    private final String password;

    public DevDataInitializer(
            AthleteRepository athleteRepository,
            PlayerProfileRepository playerProfileRepository,
            PlayerPositionRepository playerPositionRepository,
            PositionRepository positionRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            TeamStatsRepository teamStatsRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.dev-seed.password}") String password
    ) {
        this.athleteRepository = athleteRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.playerPositionRepository = playerPositionRepository;
        this.positionRepository = positionRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamStatsRepository = teamStatsRepository;
        this.passwordEncoder = passwordEncoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, PlayerProfile> players = new LinkedHashMap<>();
        for (DevPlayer player : devPlayers()) {
            players.put(player.email(), seedPlayer(player));
        }
        seedTeam(players);
        seedOnboardingUser();

        logger.info("Plantilla de desarrollo verificada: 14 jugadores en Atleta Dev FC. Usa DEV_SEED_ENABLED=false para omitirla.");
    }

    private PlayerProfile seedPlayer(DevPlayer player) {
        Athlete athlete = athleteRepository.findByEmailIgnoreCase(player.email())
                .orElseGet(() -> athleteRepository.save(new Athlete(
                        player.email(), passwordEncoder.encode(password), player.name(), player.gender())));

        PlayerProfile profile = playerProfileRepository.findById(athlete.getAtletaUuid())
                .orElseGet(() -> playerProfileRepository.save(new PlayerProfile(athlete, player.alias())));

        if (playerPositionRepository.findByPlayerOrderByPrioridad(profile).isEmpty()) {
            for (int index = 0; index < player.positions().size(); index++) {
                String positionName = player.positions().get(index);
                Position position = positionRepository.findByNombre(positionName)
                        .orElseThrow(() -> new IllegalStateException("No se encontro la posicion base: " + positionName));
                playerPositionRepository.save(new PlayerPosition(profile, position, index + 1, 0));
            }
        }
        return profile;
    }

    private void seedTeam(Map<String, PlayerProfile> players) {
        PlayerProfile captain = players.get("capitan.dev@atleta.test");
        Team team = teamRepository.findByNombre("Atleta Dev FC").orElseGet(() -> {
            Team createdTeam = teamRepository.saveAndFlush(new Team("Atleta Dev FC", captain, null, 2026));
            TeamStats stats = teamStatsRepository.save(new TeamStats(createdTeam));
            createdTeam.setStats(stats);
            return createdTeam;
        });

        team.setArchived(false);
        for (Map.Entry<String, PlayerProfile> entry : players.entrySet()) {
            PlayerRole role = roleFor(entry.getKey());
            TeamMember member = teamMemberRepository.findByTeamAndPlayer(team, entry.getValue())
                    .orElseGet(() -> new TeamMember(team, entry.getValue(), role));
            member.setRol(role);
            member.setActivo(true);
            teamMemberRepository.save(member);
        }
    }

    private PlayerRole roleFor(String email) {
        if ("capitan.dev@atleta.test".equals(email)) {
            return PlayerRole.CAPITAN;
        }
        if ("dt.dev@atleta.test".equals(email)) {
            return PlayerRole.DT;
        }
        return PlayerRole.JUGADOR;
    }

    private void seedOnboardingUser() {
        String email = "onboarding.dev@atleta.test";
        if (!athleteRepository.existsByEmail(email)) {
            athleteRepository.save(new Athlete(email, passwordEncoder.encode(password), "Nuevo Jugador", GenderType.MASCULINO));
        }
    }

    private List<DevPlayer> devPlayers() {
        return List.of(
                new DevPlayer("capitan.dev@atleta.test", "Capitan Local", "CapitanDev", GenderType.MASCULINO, List.of("Mediocampo", "Defensa", "Delantero")),
                new DevPlayer("jugador.dev@atleta.test", "Jugador Visita", "JugadorDev", GenderType.MASCULINO, List.of("Delantero", "Mediocampo", "Defensa")),
                new DevPlayer("jugadora.dev@atleta.test", "Jugadora Local", "JugadoraDev", GenderType.FEMENINO, List.of("Defensa", "Mediocampo", "Delantero")),
                new DevPlayer("arquero.dev@atleta.test", "Alex Arco", "ArqueroDev", GenderType.MASCULINO, List.of("Portero", "Defensa", "Mediocampo")),
                new DevPlayer("defensa.norte@atleta.test", "Valentina Norte", "ValeNorte", GenderType.FEMENINO, List.of("Defensa", "Mediocampo", "Lateral Izquierdo")),
                new DevPlayer("defensa.sur@atleta.test", "Tomas Sur", "TomasSur", GenderType.MASCULINO, List.of("Defensa", "Lateral Derecho", "Mediocampo")),
                new DevPlayer("volante.uno@atleta.test", "Camila Volante", "CamiVolante", GenderType.FEMENINO, List.of("Mediocampo", "Defensa", "Delantero")),
                new DevPlayer("volante.dos@atleta.test", "Mateo Volante", "MateoVolante", GenderType.MASCULINO, List.of("Mediocampo", "Delantero", "Defensa")),
                new DevPlayer("extrema.izquierda@atleta.test", "Sofia Izquierda", "SofiIzq", GenderType.FEMENINO, List.of("Lateral Izquierdo", "Mediocampo", "Delantero")),
                new DevPlayer("extremo.derecho@atleta.test", "Nicolas Derecho", "NicoDer", GenderType.MASCULINO, List.of("Lateral Derecho", "Mediocampo", "Defensa")),
                new DevPlayer("delantera.dev@atleta.test", "Antonia Gol", "AntoGol", GenderType.FEMENINO, List.of("Delantero", "Mediocampo", "Defensa")),
                new DevPlayer("delantero.dev@atleta.test", "Diego Gol", "DiegoGol", GenderType.MASCULINO, List.of("Delantero", "Mediocampo", "Defensa")),
                new DevPlayer("dt.dev@atleta.test", "Daniela Tecnica", "DTDev", GenderType.FEMENINO, List.of("Mediocampo", "Defensa", "Delantero")),
                new DevPlayer("versatil.dev@atleta.test", "Martin Versatil", "MartinDev", GenderType.MASCULINO, List.of("Defensa", "Mediocampo", "Delantero"))
        );
    }

    private record DevPlayer(String email, String name, String alias, GenderType gender, List<String> positions) {
    }
}
