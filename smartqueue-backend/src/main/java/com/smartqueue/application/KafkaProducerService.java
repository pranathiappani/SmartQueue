package com.smartqueue.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartqueue.api.dto.NotificationPayload;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TOPIC = "notification_events"; // Topic expected by existing service
    
    private static final String NOTIFICATION_URL = "http://host.docker.internal:8081/api/v1/notifications";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendNotificationEvent(NotificationPayload payload) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(TOPIC, payload.getUserId(), jsonMessage);
            
            // Direct HTTP Call to local notification service
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(NOTIFICATION_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonMessage))
                    .build();
                    
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        System.out.println("Direct Notification sent to " + NOTIFICATION_URL + " - Status: " + response.statusCode());
                    })
                    .exceptionally(ex -> {
                        System.err.println("Failed to send direct HTTP notification: " + ex.getMessage());
                        return null;
                    });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize NotificationPayload to JSON", e);
        }
    }
}
