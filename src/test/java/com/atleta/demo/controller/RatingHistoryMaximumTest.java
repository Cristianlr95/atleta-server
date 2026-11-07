package com.atleta.demo.controller;

import com.atleta.demo.dto.response.RatingHistoryResponse;
import com.atleta.demo.entity.Match;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.PlayerRating;
import com.atleta.demo.entity.RatingHistory;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RatingHistoryMaximumTest {
    @Test
    void limitsHistoricResponseWithoutChangingAuditRecord() {
        var profile = new PlayerProfile();
        var rating = new PlayerRating();
        rating.setPlayerProfile(profile);
        var history = new RatingHistory();
        history.setPlayerRating(rating);
        history.setMatch(new Match());
        history.setPreviousRating(new BigDecimal("98.00"));
        history.setNewRating(new BigDecimal("103.00"));
        history.setRatingDelta(new BigDecimal("5.00"));

        RatingHistoryResponse response = ReflectionTestUtils.invokeMethod(
                new RatingController(null, null), "convertToRatingHistoryResponse", history);

        assertThat(response).isNotNull();
        assertThat(response.getPreviousRating()).isEqualByComparingTo("98.00");
        assertThat(response.getNewRating()).isEqualByComparingTo("100.00");
        assertThat(response.getRatingDelta()).isEqualByComparingTo("2.00");
        assertThat(response.isLegacyValuesCapped()).isTrue();
        assertThat(history.getNewRating()).isEqualByComparingTo("103.00");
    }
}
