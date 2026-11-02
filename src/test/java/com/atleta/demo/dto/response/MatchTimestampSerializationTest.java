package com.atleta.demo.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchTimestampSerializationTest {
    @ParameterizedTest
    @ValueSource(strings = {"UTC", "America/Santiago"})
    void lifecycleTimestampsKeepTheSameInstantInEveryServerZone(String zone) throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            Instant instant = Instant.parse("2026-10-26T18:58:00Z");
            LocalDateTime local = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            MatchResponse response = new MatchResponse();
            response.setStartedAt(local);
            response.setFinalizedAt(local);
            response.setMvpVotingClosedAt(local);
            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            var json = mapper.readTree(mapper.writeValueAsString(response));
            for (String field : new String[]{"startedAt", "finalizedAt", "mvpVotingClosedAt"}) {
                assertEquals(instant, OffsetDateTime.parse(json.get(field).asText()).toInstant());
            }
            MatchMvpResponse voting = new MatchMvpResponse();
            voting.setFinalizedAt(local);
            voting.setClosesAt(local.plusHours(3));
            var votingJson = mapper.readTree(mapper.writeValueAsString(voting));
            assertEquals(instant, OffsetDateTime.parse(votingJson.get("finalizedAt").asText()).toInstant());
            assertEquals(instant.plusSeconds(3 * 3600), OffsetDateTime.parse(votingJson.get("closesAt").asText()).toInstant());
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
