package com.atleta.demo.integration;

import com.atleta.demo.entity.Athlete;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.enums.MatchMode;
import com.atleta.demo.repository.AthleteRepository;
import com.atleta.demo.repository.MatchRepository;
import com.atleta.demo.repository.PlayerProfileRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.datasource.hikari.maximum-pool-size=2", "spring.datasource.hikari.minimum-idle=0"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MatchLiveConnectionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired HikariDataSource dataSource;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired AthleteRepository athletes;
    @Autowired PlayerProfileRepository profiles;
    @Autowired MatchRepository matches;

    @Test
    void openStreamsReleaseConnectionsAndKeepParticipantAuthorization() throws Exception {
        UUID creatorId = UUID.randomUUID();
        Long matchId = new TransactionTemplate(transactionManager).execute(ignored -> {
            Athlete athlete = new Athlete();
            athlete.setEmail(creatorId + "@live.test");
            athlete.setNombre("Live captain");
            athlete.setPasswordHash("test-password-hash");
            athlete = athletes.save(athlete);
            PlayerProfile profile = new PlayerProfile();
            profile.setAtletaUuid(athlete.getAtletaUuid());
            profile.setAthlete(athlete);
            profile.setAlias("Live captain");
            profile = profiles.save(profile);
            Match match = new Match();
            match.setCreador(profile);
            match.setModalidad(MatchMode.CINCO_VS_CINCO);
            match.setFechaHoraProgramada(LocalDateTime.now().plusDays(1));
            return matches.save(match).getId();
        });
        UUID captain = new TransactionTemplate(transactionManager).execute(ignored ->
                matches.findById(matchId).orElseThrow().getCreador().getAtletaUuid());
        var streams = new ArrayList<MvcResult>();
        try {
            // More simultaneous, still-open streams than available database connections.
            for (int i = 0; i < 4; i++) {
                streams.add(mvc.perform(get("/api/v1/matches/{id}/live", matchId)
                                .with(jwt().jwt(token -> token.subject(captain.toString()))))
                        .andExpect(status().isOk()).andExpect(request().asyncStarted()).andReturn());
                assertEquals(0, dataSource.getHikariPoolMXBean().getActiveConnections(),
                        "An open SSE stream must not reserve a database connection");
            }
            mvc.perform(get("/api/v1/matches/{id}/live", matchId)
                            .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/matches/{id}", matchId)
                            .with(jwt().jwt(token -> token.subject(captain.toString()))))
                    .andExpect(status().isOk());
        } finally {
            streams.forEach(stream -> stream.getRequest().getAsyncContext().complete());
            new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
                matches.deleteById(matchId);
                profiles.deleteById(captain);
                athletes.deleteById(captain);
            });
        }
    }
}
