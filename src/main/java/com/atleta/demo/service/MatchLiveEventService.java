package com.atleta.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class MatchLiveEventService {
    private static final long SSE_TIMEOUT_MS = 30L * 60L * 1000L;
    private static final int SSE_DISPATCH_THREADS = 4;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> subscribersByMatch = new ConcurrentHashMap<>();
    private final ExecutorService dispatcher = Executors.newFixedThreadPool(SSE_DISPATCH_THREADS);

    @PreDestroy
    public void shutdown() {
        dispatcher.shutdownNow();
    }

    public SseEmitter subscribe(Long matchId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        subscribersByMatch.computeIfAbsent(matchId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(matchId, emitter));
        emitter.onTimeout(() -> removeEmitter(matchId, emitter));
        emitter.onError(ex -> removeEmitter(matchId, emitter));

        send(emitter, "connected", Map.of(
                "matchId", matchId,
                "connectedAt", LocalDateTime.now().toString()
        ));

        return emitter;
    }

    public void publishInviteCreated(Long matchId, Long inviteId) {
        publish(matchId, "match-invite-created", Map.of(
                "matchId", matchId,
                "inviteId", inviteId,
                "updatedAt", LocalDateTime.now().toString()
        ));
    }

    public void publishInviteDecision(Long matchId, Long inviteId, String status) {
        publish(matchId, "match-invite-updated", Map.of(
                "matchId", matchId,
                "inviteId", inviteId,
                "status", status,
                "updatedAt", LocalDateTime.now().toString()
        ));
    }

    private void publish(Long matchId, String eventName, Object payload) {
        List<SseEmitter> emitters = subscribersByMatch.get(matchId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            dispatcher.execute(() -> send(emitter, eventName, payload));
        }
    }

    private void send(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
        } catch (IOException | IllegalStateException ex) {
            removeEmitter(emitter);
        }
    }

    private void removeEmitter(Long matchId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = subscribersByMatch.get(matchId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            subscribersByMatch.remove(matchId);
        }
    }

    private void removeEmitter(SseEmitter emitter) {
        for (Map.Entry<Long, CopyOnWriteArrayList<SseEmitter>> entry : subscribersByMatch.entrySet()) {
            removeEmitter(entry.getKey(), emitter);
        }
    }
}
