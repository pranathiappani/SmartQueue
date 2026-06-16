package com.smartqueue.application;

import com.smartqueue.domain.ServiceQueue;
import com.smartqueue.domain.Token;
import com.smartqueue.domain.TokenStatus;
import com.smartqueue.repository.ServiceQueueRepository;
import com.smartqueue.repository.TokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AiService {

    @Value("${ai.gemini.api-key}")
    private String apiKey;

    @Value("${ai.gemini.url}")
    private String apiUrl;

    private final ServiceQueueRepository queueRepository;
    private final TokenRepository tokenRepository;
    private final TokenService tokenService;
    private final RestTemplate restTemplate;

    public AiService(ServiceQueueRepository queueRepository, TokenRepository tokenRepository, TokenService tokenService) {
        this.queueRepository = queueRepository;
        this.tokenRepository = tokenRepository;
        this.tokenService = tokenService;
        
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String getChatResponse(String userMessage, UUID queueId, String username) {
        String systemInfo = buildContext(queueId);
        StringBuilder fullPromptBuilder = new StringBuilder();
        fullPromptBuilder.append("System Context:\n");
        fullPromptBuilder.append("You are the SmartQueue AI assistant. Your goal is to be helpful and concise.\n");
        fullPromptBuilder.append("Below is the current real-time status of all active queues:\n");
        fullPromptBuilder.append(systemInfo).append("\n\n");
        fullPromptBuilder.append("Instructions:\n");
        fullPromptBuilder.append("1. Answer the user's question using the queue status provided above.\n");
        fullPromptBuilder.append("2. If the user explicitly asks to join a queue, generate a token, or book a spot, you MUST use the `generate_token` tool.\n");
        fullPromptBuilder.append("3. If the user is just asking a question (e.g. 'how long is the wait?'), answer directly and DO NOT use the tool.");

        String systemPrompt = fullPromptBuilder.toString();

        Map<String, Object> requestBody = new HashMap<>();
        
        Map<String, Object> systemInstruction = new HashMap<>();
        Map<String, Object> sysPart = new HashMap<>();
        sysPart.put("text", systemPrompt);
        systemInstruction.put("parts", List.of(sysPart));
        requestBody.put("systemInstruction", systemInstruction);

        Map<String, Object> contentPart = new HashMap<>();
        contentPart.put("text", userMessage);
        Map<String, Object> contentBody = new HashMap<>();
        contentBody.put("parts", List.of(contentPart));
        requestBody.put("contents", List.of(contentBody));

        // Add Tools (Function Declarations)
        Map<String, Object> functionDecl = new HashMap<>();
        functionDecl.put("name", "generate_token");
        functionDecl.put("description", "Generates a new token for the user, putting them in the specified queue.");
        
        Map<String, Object> params = new HashMap<>();
        params.put("type", "OBJECT");
        Map<String, Object> props = new HashMap<>();
        Map<String, Object> queueNameProp = new HashMap<>();
        queueNameProp.put("type", "STRING");
        queueNameProp.put("description", "The exact name of the queue the user wants to join.");
        props.put("queue_name", queueNameProp);
        params.put("properties", props);
        params.put("required", List.of("queue_name"));
        
        functionDecl.put("parameters", params);

        Map<String, Object> tool = new HashMap<>();
        tool.put("functionDeclarations", List.of(functionDecl));
        // **IMPORTANT**: Gemini requires `tools` to be an array of tools!
        requestBody.put("tools", List.of(tool));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            System.out.println("SENDING REQUEST TO GEMINI...");
            try {
                System.out.println(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(requestBody));
            } catch (Exception ex) {}
            long start = System.currentTimeMillis();
            Map<String, Object> response = restTemplate.postForObject(apiUrl + "?key=" + apiKey, entity, Map.class);
            System.out.println("RECEIVED RESPONSE IN " + (System.currentTimeMillis() - start) + "ms");
            
            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> resParts = (List<Map<String, Object>>) content.get("parts");
                    
                    Map<String, Object> firstPart = resParts.get(0);
                    
                    // Check if AI decided to call a function
                    if (firstPart.containsKey("functionCall")) {
                        Map<String, Object> functionCall = (Map<String, Object>) firstPart.get("functionCall");
                        if ("generate_token".equals(functionCall.get("name"))) {
                            Map<String, Object> args = (Map<String, Object>) functionCall.get("args");
                            String requestedQueueName = (String) args.get("queue_name");
                            
                            return executeGenerateToken(requestedQueueName, username);
                        }
                    }
                    
                    // Otherwise return normal text response
                    return (String) firstPart.get("text");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "I'm sorry, but I am currently having trouble connecting to my brain. Please try again later!";
        }
        
        return "Sorry, I couldn't understand that.";
    }

    private String executeGenerateToken(String queueName, String username) {
        if (username == null) {
            return "You must be logged in to generate a token via chat.";
        }
        
        Optional<ServiceQueue> queueOpt = queueRepository.findByNameIgnoreCase(queueName);
        if (queueOpt.isEmpty()) {
            return "I couldn't find a queue named '" + queueName + "'. Please check the name and try again.";
        }
        
        try {
            Token generatedToken = tokenService.generateToken(username, queueOpt.get().getId(), false, null);
            int dynamicAverageTime = tokenService.calculateDynamicAverageServiceTime(queueOpt.get());
            long waitingCount = tokenRepository.countByServiceQueueAndStatus(queueOpt.get(), TokenStatus.WAITING);
            long etaMinutes = waitingCount * dynamicAverageTime;
            
            return "Success! I have generated a token for you in the " + queueOpt.get().getName() + " queue. Your token number is **" + generatedToken.getTokenNumber() + "**. Your estimated wait time is approximately " + etaMinutes + " minutes.";
        } catch (Exception e) {
            return "I tried to generate a token for you, but an error occurred: " + e.getMessage();
        }
    }

    private String buildContext(UUID queueId) {
        java.time.LocalDateTime startOfDay = java.time.LocalDateTime.now().toLocalDate().atStartOfDay();

        if (queueId == null) {
            List<ServiceQueue> queues = queueRepository.findAll();
            if (queues.isEmpty()) {
                return "General System Info: We currently have no active queues in the system.";
            }
            StringBuilder ctx = new StringBuilder("General System Info: Here is the status of all current queues:\n");
            for (ServiceQueue q : queues) {
                long waitingCount = tokenRepository.countByServiceQueueAndStatus(q, TokenStatus.WAITING);
                int dynamicAvg = tokenService.calculateDynamicAverageServiceTime(q);
                long completedToday = tokenRepository.countByServiceQueueAndStatusAndCreatedAtAfter(q, TokenStatus.COMPLETED, startOfDay);
                
                ctx.append("- ").append(q.getName()).append(" (Type: ").append(q.getType()).append("): ")
                   .append(waitingCount).append(" people waiting. ")
                   .append("Moving at ").append(dynamicAvg).append(" mins/person. ")
                   .append(completedToday).append(" completed today.\n");
            }
            return ctx.toString();
        }

        return queueRepository.findById(queueId).map(queue -> {
            long waitingCount = tokenRepository.countByServiceQueueAndStatus(queue, TokenStatus.WAITING);
            int dynamicAverageTime = tokenService.calculateDynamicAverageServiceTime(queue);
            long completedToday = tokenRepository.countByServiceQueueAndStatusAndCreatedAtAfter(queue, TokenStatus.COMPLETED, startOfDay);
            long cancelledToday = tokenRepository.countByServiceQueueAndStatusAndCreatedAtAfter(queue, TokenStatus.CANCELLED, startOfDay);
            
            StringBuilder ctx = new StringBuilder();
            ctx.append("=== Core Queue Info ===\n");
            ctx.append("Queue Name: ").append(queue.getName()).append("\n");
            ctx.append("Type: ").append(queue.getType()).append("\n");
            ctx.append("Total People Currently Waiting: ").append(waitingCount).append("\n\n");
            
            ctx.append("=== Deep Predictive Analytics ===\n");
            ctx.append("Official Static Baseline Service Time: ").append(queue.getAverageServiceTimeMinutes()).append(" minutes per person.\n");
            ctx.append("Dynamic Real-Time Service Rate (based on recent actual speed): ").append(dynamicAverageTime).append(" minutes per person.\n");
            
            if (waitingCount > 0) {
                ctx.append("Predictive ETA for a new person joining now: ")
                   .append(waitingCount * dynamicAverageTime).append(" minutes.\n");
            }
            
            ctx.append("Total Tokens Completed Today: ").append(completedToday).append("\n");
            ctx.append("Total Tokens Cancelled/Abandoned Today: ").append(cancelledToday).append("\n");

            return ctx.toString();
        }).orElse("The requested queue ID could not be found.");
    }
}
