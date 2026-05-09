package com.atleta.demo.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MatchStatusSchedulerTest {

    @Test
    void refreshAutomatedMatchStates_DelegatesToMatchService() {
        MatchService matchService = mock(MatchService.class);
        MatchStatusScheduler scheduler = new MatchStatusScheduler(matchService);

        scheduler.refreshAutomatedMatchStates();

        verify(matchService).refreshAutomatedMatchStates();
    }
}
