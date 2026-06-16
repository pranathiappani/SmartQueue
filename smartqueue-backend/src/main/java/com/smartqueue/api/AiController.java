package com.smartqueue.api;

import com.smartqueue.api.dto.ChatRequest;
import com.smartqueue.api.dto.ChatResponse;
import com.smartqueue.application.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/chat")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        String responseText = aiService.getChatResponse(request.getMessage(), request.getQueueId(), username);
        return ResponseEntity.ok(new ChatResponse(responseText));
    }
}
