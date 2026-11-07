package com.atleta.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "atleta.matches.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MatchStatusScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MatchStatusScheduler.class);

    private final MatchAutomatedStatusService matchAutomatedStatusService;

    public MatchStatusScheduler(MatchAutomatedStatusService matchAutomatedStatusService) {
        this.matchAutomatedStatusService = matchAutomatedStatusService;
    }

    @Scheduled(
            initialDelayString = "${atleta.matches.scheduler.initial-delay:PT1M}",
            fixedDelayString = "${atleta.matches.scheduler.fixed-delay:PT5M}"
    )
    public void refreshAutomatedMatchStates() {
        logger.debug("Refrescando estados automaticos de partidos por scheduler");
        matchAutomatedStatusService.refreshAutomatedMatchStates();
    }
}
