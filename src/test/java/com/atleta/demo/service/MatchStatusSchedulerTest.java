package com.atleta.demo.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MatchStatusSchedulerTest {

    @Test
    void refreshAutomatedMatchStates_DelegatesToAutomatedStatusService() {
        MatchAutomatedStatusService service = mock(MatchAutomatedStatusService.class);
        MatchStatusScheduler scheduler = new MatchStatusScheduler(service);

        scheduler.refreshAutomatedMatchStates();

        verify(service).refreshAutomatedMatchStates();
    }
}
