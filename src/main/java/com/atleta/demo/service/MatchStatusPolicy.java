package com.atleta.demo.service;

import com.atleta.demo.entity.Match;
import com.atleta.demo.enums.MatchStatus;
import com.atleta.demo.enums.MatchValidationStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MatchStatusPolicy {

    public void validateTransition(MatchStatus currentStatus, MatchStatus newStatus) {
        switch (currentStatus) {
            case CREADO:
                if (newStatus != MatchStatus.INICIADO && newStatus != MatchStatus.INVALIDO) {
                    throw new IllegalArgumentException("Desde CREADO solo se puede pasar a INICIADO o INVALIDO");
                }
                break;
            case INICIADO:
                if (newStatus != MatchStatus.FINALIZADO && newStatus != MatchStatus.INVALIDO) {
                    throw new IllegalArgumentException("Desde INICIADO solo se puede pasar a FINALIZADO o INVALIDO");
                }
                break;
            case FINALIZADO:
            case INVALIDO:
                throw new IllegalArgumentException("No se puede cambiar el estado desde " + currentStatus);
        }
    }

    public void markStarted(Match match, LocalDateTime startedAt) {
        if (match.getStartedAt() == null) {
            match.setStartedAt(startedAt);
        }
        match.setValidationStatus(MatchValidationStatus.PENDING);
        match.setValidationReason(null);
    }

    public void markManualInvalid(Match match) {
        match.setValidationStatus(MatchValidationStatus.INVALID_STATE);
        match.setValidationReason("Partido marcado como invalido manualmente");
    }

    public boolean isClosePending(Match match, long matchPlayWindowHours, LocalDateTime now) {
        if (match == null || match.getEstado() != MatchStatus.INICIADO || match.getStartedAt() == null) {
            return false;
        }

        return now.isAfter(match.getStartedAt().plusHours(matchPlayWindowHours));
    }
}
