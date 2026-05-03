package com.atleta.demo.controller;

import com.atleta.demo.dto.request.CreateFriendRequest;
import com.atleta.demo.dto.request.CreateMatchInviteRequest;
import com.atleta.demo.dto.request.CreateMatchInvitesBatchRequest;
import com.atleta.demo.dto.request.CreateTeamInviteRequest;
import com.atleta.demo.dto.request.RegisterPushTokenRequest;
import com.atleta.demo.dto.request.RespondRequestDecision;
import com.atleta.demo.dto.response.AppNotificationResponse;
import com.atleta.demo.dto.response.PushTokenResponse;
import com.atleta.demo.dto.response.SocialPlayerLookupResponse;
import com.atleta.demo.dto.response.SocialRequestResponse;
import com.atleta.demo.dto.response.UnreadNotificationCountResponse;
import com.atleta.demo.service.SocialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social")
@Tag(name = "Social", description = "Amistades, invitaciones y notificaciones")
public class SocialController {
    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @PostMapping("/friendships/requests")
    @Operation(summary = "Enviar solicitud de amistad")
    public ResponseEntity<SocialRequestResponse> createFriendRequest(@Valid @RequestBody CreateFriendRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(socialService.createFriendRequest(request));
    }

    @PutMapping("/friendships/requests/{requestId}/decision")
    @Operation(summary = "Responder solicitud de amistad")
    public ResponseEntity<SocialRequestResponse> respondFriendRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody RespondRequestDecision decision
    ) {
        return ResponseEntity.ok(socialService.respondFriendRequest(requestId, decision));
    }

    @GetMapping("/friendships/{playerUuid}")
    @Operation(summary = "Listar amistades y solicitudes")
    public ResponseEntity<List<SocialRequestResponse>> getFriendships(@PathVariable UUID playerUuid) {
        return ResponseEntity.ok(socialService.getFriendships(playerUuid));
    }

    @PostMapping("/team-invites")
    @Operation(summary = "Enviar invitacion a equipo")
    public ResponseEntity<SocialRequestResponse> createTeamInvite(@Valid @RequestBody CreateTeamInviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(socialService.createTeamInvite(request));
    }

    @PutMapping("/team-invites/{inviteId}/decision")
    @Operation(summary = "Responder invitacion de equipo")
    public ResponseEntity<SocialRequestResponse> respondTeamInvite(
            @PathVariable Long inviteId,
            @Valid @RequestBody RespondRequestDecision decision
    ) {
        return ResponseEntity.ok(socialService.respondTeamInvite(inviteId, decision));
    }

    @GetMapping("/team-invites/{playerUuid}")
    @Operation(summary = "Listar invitaciones de equipo")
    public ResponseEntity<List<SocialRequestResponse>> getTeamInvites(@PathVariable UUID playerUuid) {
        return ResponseEntity.ok(socialService.getTeamInvites(playerUuid));
    }

    @PostMapping("/match-invites")
    @Operation(summary = "Enviar invitacion de partido")
    public ResponseEntity<SocialRequestResponse> createMatchInvite(@Valid @RequestBody CreateMatchInviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(socialService.createMatchInvite(request));
    }

    @PostMapping("/match-invites/batch")
    @Operation(summary = "Enviar invitaciones de partido en lote")
    public ResponseEntity<List<SocialRequestResponse>> createMatchInvitesBatch(
            @Valid @RequestBody CreateMatchInvitesBatchRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(socialService.createMatchInvitesBatch(request));
    }

    @PutMapping("/match-invites/{inviteId}/decision")
    @Operation(summary = "Responder invitacion de partido")
    public ResponseEntity<SocialRequestResponse> respondMatchInvite(
            @PathVariable Long inviteId,
            @Valid @RequestBody RespondRequestDecision decision
    ) {
        return ResponseEntity.ok(socialService.respondMatchInvite(inviteId, decision));
    }

    @GetMapping("/match-invites/{playerUuid}")
    @Operation(summary = "Listar invitaciones de partido")
    public ResponseEntity<List<SocialRequestResponse>> getMatchInvites(@PathVariable UUID playerUuid) {
        return ResponseEntity.ok(socialService.getMatchInvites(playerUuid));
    }

    @GetMapping("/match-invites/by-match/{matchId}")
    @Operation(summary = "Listar invitaciones de partido por matchId")
    public ResponseEntity<List<SocialRequestResponse>> getMatchInvitesByMatch(@PathVariable Long matchId) {
        return ResponseEntity.ok(socialService.getMatchInvitesByMatch(matchId));
    }

    @GetMapping("/notifications/{playerUuid}")
    @Operation(summary = "Listar notificaciones de usuario")
    public ResponseEntity<List<AppNotificationResponse>> getNotifications(@PathVariable UUID playerUuid) {
        return ResponseEntity.ok(socialService.getNotifications(playerUuid));
    }

    @PutMapping("/notifications/{notificationId}/read")
    @Operation(summary = "Marcar notificacion como leida")
    public ResponseEntity<AppNotificationResponse> markNotificationAsRead(
            @PathVariable Long notificationId,
            @RequestParam UUID playerUuid
    ) {
        return ResponseEntity.ok(socialService.markNotificationAsRead(notificationId, playerUuid));
    }

    @PostMapping("/notifications/reminders/forms/{playerUuid}")
    @Operation(summary = "Emitir recordatorio de formulario incompleto")
    public ResponseEntity<AppNotificationResponse> sendIncompleteFormReminder(@PathVariable UUID playerUuid) {
        return ResponseEntity.status(HttpStatus.CREATED).body(socialService.sendIncompleteFormReminder(playerUuid));
    }

    @PostMapping("/notifications/push-tokens")
    @Operation(summary = "Registrar o refrescar push token del usuario autenticado")
    public ResponseEntity<PushTokenResponse> registerPushToken(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RegisterPushTokenRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(socialService.registerPushToken(currentUserUuid(jwt), request));
    }

    @GetMapping("/notifications/unread-count")
    @Operation(summary = "Obtener contador de notificaciones no leidas del usuario autenticado")
    public ResponseEntity<UnreadNotificationCountResponse> getUnreadNotificationCount(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(socialService.getUnreadNotificationCount(currentUserUuid(jwt)));
    }

    @GetMapping("/players/search")
    @Operation(summary = "Buscar jugadores para invitaciones")
    public ResponseEntity<List<SocialPlayerLookupResponse>> searchPlayers(@RequestParam String q) {
        return ResponseEntity.ok(socialService.searchPlayers(q));
    }

    private UUID currentUserUuid(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new IllegalArgumentException("No se pudo identificar el usuario autenticado");
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El usuario autenticado no tiene un UUID valido");
        }
    }
}
