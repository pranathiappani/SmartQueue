package com.smartqueue.api;

import com.smartqueue.application.TokenService;
import com.smartqueue.domain.Token;
import com.smartqueue.domain.TokenStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tokens")
public class TokenController {

    private final TokenService tokenService;

    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public record GenerateTokenRequest(UUID queueId, LocalDateTime scheduledTime) {}

    @PostMapping
    public ResponseEntity<Token> generateToken(Authentication authentication, @RequestBody GenerateTokenRequest request) {
        String username = authentication.getName();
        Token token = tokenService.generateToken(username, request.queueId(), false, request.scheduledTime());
        return ResponseEntity.ok(token);
    }

    public record GenerateVipTokenRequest(UUID queueId, String username, LocalDateTime scheduledTime) {}

    @PostMapping("/vip")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Token> generateVipToken(@RequestBody GenerateVipTokenRequest request) {
        Token token = tokenService.generateToken(request.username(), request.queueId(), true, request.scheduledTime());
        return ResponseEntity.ok(token);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Token> getTokenStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(tokenService.getToken(id));
    }

    @GetMapping("/queue/{queueId}")
    public ResponseEntity<java.util.List<Token>> getTokensByQueue(@PathVariable UUID queueId) {
        return ResponseEntity.ok(tokenService.getTokensByQueueId(queueId));
    }

    @GetMapping("/queue/{queueId}/slots")
    public ResponseEntity<java.util.List<String>> getAvailableSlots(@PathVariable UUID queueId, @RequestParam String date) {
        return ResponseEntity.ok(tokenService.getAvailableSlots(queueId, date));
    }
    
    @GetMapping("/my-tokens")
    public ResponseEntity<java.util.List<Token>> getMyTokens(Authentication authentication) {
        return ResponseEntity.ok(tokenService.getUserTokens(authentication.getName()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<String> cancelToken(@PathVariable UUID id, Authentication authentication) {
        tokenService.cancelToken(id, authentication.getName());
        return ResponseEntity.ok("Token cancelled successfully");
    }

    @PostMapping("/{id}/delay")
    public ResponseEntity<String> delayToken(@PathVariable UUID id, Authentication authentication) {
        tokenService.delayToken(id, authentication.getName());
        return ResponseEntity.ok("Token delayed successfully");
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Token> updateTokenStatus(@PathVariable UUID id, @RequestParam TokenStatus status) {
        Token token = tokenService.updateTokenStatus(id, status);
        return ResponseEntity.ok(token);
    }
}
