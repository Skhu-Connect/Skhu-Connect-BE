package org.skhuconnect.notification.controller;

import jakarta.validation.Valid;
import org.skhuconnect.notification.dto.request.FcmTokenRequest;
import org.skhuconnect.notification.fcm.FcmTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/connect/notifications/fcm-tokens")
public class FcmTokenController {
    private final FcmTokenService service;
    public FcmTokenController(FcmTokenService service) { this.service=service; }
    @PostMapping public ResponseEntity<Void> register(@RequestAttribute("userId") Long userId, @Valid @RequestBody FcmTokenRequest request) { service.register(userId, request); return ResponseEntity.noContent().build(); }
    @DeleteMapping public ResponseEntity<Void> delete(@RequestAttribute("userId") Long userId, @Valid @RequestBody FcmTokenRequest request) { service.delete(userId, request.token()); return ResponseEntity.noContent().build(); }
}
