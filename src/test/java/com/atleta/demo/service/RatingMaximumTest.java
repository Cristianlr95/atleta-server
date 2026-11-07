package com.atleta.demo.service;

import com.atleta.demo.dto.request.RatingCalculationRequest;
import com.atleta.demo.dto.request.RotativeGoalkeeperRequest;
import com.atleta.demo.enums.MatchResultType;
import com.atleta.demo.enums.PriorityLevel;
import com.atleta.demo.enums.RoleType;
import com.atleta.demo.entity.PlayerProfile;
import com.atleta.demo.entity.PlayerRating;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RatingMaximumTest {
    private final RatingCalculationEngine engine = new RatingCalculationEngine();

    @Test
    void normalRatingStopsAtOneHundred() {
        var request = new RatingCalculationRequest(new BigDecimal("99.50"), RoleType.ATAQUE,
                PriorityLevel.PRINCIPAL, MatchResultType.GANADO, 1, 0, null, false);
        assertThat(engine.calculateNewRating(request)).isEqualByComparingTo("100.00");
    }

    @Test
    void rotativeGoalkeeperRatingStopsAtOneHundred() {
        var request = new RotativeGoalkeeperRequest(new BigDecimal("99.50"),
                PriorityLevel.PRINCIPAL, MatchResultType.GANADO);
        assertThat(engine.calculateRotativeGoalkeeperRating(request)).isEqualByComparingTo("100.00");
    }

    @Test
    void ratingCanStillFallAfterReachingOneHundred() {
        var request = new RatingCalculationRequest(new BigDecimal("100.00"), RoleType.ATAQUE,
                PriorityLevel.PRINCIPAL, MatchResultType.PERDIDO, 0, 0, null, false);
        assertThat(engine.calculateNewRating(request)).isEqualByComparingTo("98.50");
    }

    @Test
    void hybridAndWeightedOverallReachButDoNotExceedOneHundred() {
        var service = new RatingService(null, null, null, null, null, null);
        var profile = new PlayerProfile();
        List<PlayerRating> ratings = new ArrayList<>();
        for (RoleType role : RoleType.values()) {
            ratings.add(new PlayerRating(profile, role, PriorityLevel.PRINCIPAL, BigDecimal.valueOf(100)));
        }

        BigDecimal hybrid = ReflectionTestUtils.invokeMethod(service, "calculateHybridOVRFromRatings", ratings);
        BigDecimal weighted = ReflectionTestUtils.invokeMethod(service, "calculateWeightedOVRFromRatings", ratings);
        assertThat(hybrid).isEqualByComparingTo("100.00");
        assertThat(weighted).isEqualByComparingTo("100.00");
    }
}
