package com.smartqueue.application;

import com.smartqueue.api.dto.NotificationPayload;
import com.smartqueue.domain.Token;
import com.smartqueue.domain.TokenStatus;
import com.smartqueue.domain.User;
import com.smartqueue.domain.ServiceQueue;
import com.smartqueue.domain.QueueType;
import com.smartqueue.repository.ServiceQueueRepository;
import com.smartqueue.repository.TokenRepository;
import com.smartqueue.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TokenService {

    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ServiceQueueRepository serviceQueueRepository;
    private final KafkaProducerService kafkaProducerService;

    public TokenService(TokenRepository tokenRepository, UserRepository userRepository, ServiceQueueRepository serviceQueueRepository, KafkaProducerService kafkaProducerService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.serviceQueueRepository = serviceQueueRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    public Token generateToken(String username, UUID queueId, boolean isVip, LocalDateTime scheduledTime) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ServiceQueue queue = serviceQueueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found"));

        long totalTokens = tokenRepository.count();
        String tokenNumber = (isVip ? "VIP-" : "T-") + (1000 + totalTokens + 1);

        Token token = new Token(user, queue, tokenNumber, TokenStatus.WAITING);
        token.setVip(isVip);

        if (queue.getType() == QueueType.SCHEDULED) {
            if (scheduledTime == null) throw new IllegalArgumentException("Scheduled time required");
            boolean isBooked = tokenRepository.existsByServiceQueueAndScheduledTimeAndStatusNot(queue, scheduledTime, TokenStatus.CANCELLED);
            if (isBooked) throw new IllegalStateException("Slot already booked");
            
            token.setScheduledTime(scheduledTime);
            token.setScheduledEndTime(scheduledTime.plusMinutes(queue.getSlotDurationMinutes()));
            token.setExpectedTime(scheduledTime);
            token.setSortOrderTime(scheduledTime);
        } else {
            long waitingCount = tokenRepository.countByServiceQueueAndStatus(queue, TokenStatus.WAITING);
            int dynamicAverageTime = calculateDynamicAverageServiceTime(queue);
            LocalDateTime expectedTime = LocalDateTime.now().plusMinutes((long) dynamicAverageTime * waitingCount);

            token.setExpectedTime(expectedTime);
            if (isVip) {
                token.setSortOrderTime(LocalDateTime.now().minusYears(1));
            }
        }
        
        tokenRepository.save(token);

        sendNotification(user, token, "TOKEN_CREATED");
        
        if (queue.getType() == QueueType.LIVE) {
            recalculateExpectedTimes(queue, calculateDynamicAverageServiceTime(queue));
        }

        return token;
    }

    public Token getToken(UUID id) {
        return tokenRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Token not found"));
    }

    public java.util.List<String> getAvailableSlots(UUID queueId, String dateStr) {
        ServiceQueue queue = serviceQueueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found"));
        
        if (queue.getType() != QueueType.SCHEDULED) {
            throw new IllegalArgumentException("Queue is not a scheduled queue");
        }
        
        java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
        java.time.LocalTime start = queue.getOperatingHoursStart();
        java.time.LocalTime end = queue.getOperatingHoursEnd();
        
        int duration = queue.getSlotDurationMinutes();
        int buffer = queue.getSlotBufferMinutes() != null ? queue.getSlotBufferMinutes() : 0;
        
        java.util.List<String> slots = new java.util.ArrayList<>();
        
        java.time.LocalTime current = start;
        while (current.plusMinutes(duration).isBefore(end) || current.plusMinutes(duration).equals(end)) {
            LocalDateTime slotTime = LocalDateTime.of(date, current);
            
            if (slotTime.isAfter(LocalDateTime.now())) {
                boolean isBooked = tokenRepository.existsByServiceQueueAndScheduledTimeAndStatusNot(queue, slotTime, TokenStatus.CANCELLED);
                if (!isBooked) {
                    slots.add(current.toString());
                }
            }
            
            current = current.plusMinutes(duration + buffer);
        }
        
        return slots;
    }

    public java.util.List<Token> getTokensByQueueId(UUID queueId) {
        return tokenRepository.findByServiceQueueIdOrderByIsVipDescSortOrderTimeAsc(queueId);
    }
    
    public java.util.List<Token> getUserTokens(String username) {
        return tokenRepository.findByUserUsernameOrderByCreatedAtDesc(username);
    }

    public void cancelToken(UUID id, String username) {
        Token token = getToken(id);
        if (!token.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Not authorized to cancel this token");
        }
        token.setStatus(TokenStatus.CANCELLED);
        tokenRepository.save(token);
        
        sendNotification(token.getUser(), token, "TOKEN_CANCELLED");
    }

    public void delayToken(UUID id, String username) {
        Token token = getToken(id);
        if (!token.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Not authorized");
        }
        if (token.getStatus() != TokenStatus.WAITING) {
            throw new IllegalStateException("Only waiting tokens can be delayed");
        }
        
        ServiceQueue queue = token.getServiceQueue();
        java.util.List<Token> waitingTokens = tokenRepository.findByServiceQueueAndStatusOrderByIsVipDescSortOrderTimeAsc(queue, TokenStatus.WAITING);
        
        int currentIndex = waitingTokens.indexOf(token);
        if (currentIndex == -1 || currentIndex == waitingTokens.size() - 1) {
            return;
        }
        
        int targetIndex = Math.min(currentIndex + 3, waitingTokens.size() - 1);
        Token targetToken = waitingTokens.get(targetIndex);
        
        token.setSortOrderTime(targetToken.getSortOrderTime().plusSeconds(1));
        tokenRepository.save(token);
        
        int dynamicAverageTime = calculateDynamicAverageServiceTime(queue);
        recalculateExpectedTimes(queue, dynamicAverageTime);
    }

    public Token updateTokenStatus(UUID id, TokenStatus status) {
        Token token = getToken(id);
        token.setStatus(status);
        
        if (status == TokenStatus.ACTIVE) {
            token.setActivatedAt(LocalDateTime.now());
            sendNotification(token.getUser(), token, "TOKEN_CALLED");
        } else if (status == TokenStatus.COMPLETED) {
            token.setCompletedAt(LocalDateTime.now());
            sendNotification(token.getUser(), token, "TOKEN_COMPLETED");
        }
        
        tokenRepository.save(token);
        
        if (status == TokenStatus.ACTIVE || status == TokenStatus.COMPLETED || status == TokenStatus.CANCELLED) {
            ServiceQueue queue = token.getServiceQueue();
            if (queue.getType() == QueueType.LIVE) {
                int dynamicAverageTime = calculateDynamicAverageServiceTime(queue);
                recalculateExpectedTimes(queue, dynamicAverageTime);
            }
        }
        
        return token;
    }

    private void recalculateExpectedTimes(ServiceQueue queue, int dynamicAverageTime) {
        java.util.List<Token> activeTokens = tokenRepository.findByServiceQueueAndStatusOrderByIsVipDescSortOrderTimeAsc(queue, TokenStatus.ACTIVE);
        
        LocalDateTime baseline = LocalDateTime.now();
        if (!activeTokens.isEmpty()) {
            Token active = activeTokens.get(0);
            if (active.getActivatedAt() != null) {
                LocalDateTime projectedCompletion = active.getActivatedAt().plusMinutes(dynamicAverageTime);
                if (projectedCompletion.isAfter(LocalDateTime.now())) {
                    baseline = projectedCompletion;
                }
            }
        }
        
        java.util.List<Token> waitingTokens = tokenRepository.findByServiceQueueAndStatusOrderByIsVipDescSortOrderTimeAsc(queue, TokenStatus.WAITING);
        for (int i = 0; i < waitingTokens.size(); i++) {
            Token waitingToken = waitingTokens.get(i);
            
            long additionalWait = (long) dynamicAverageTime * i;
            LocalDateTime newExpectedTime = baseline.plusMinutes(additionalWait);
            
            waitingToken.setExpectedTime(newExpectedTime);
            
            long minutesUntil = java.time.Duration.between(LocalDateTime.now(), newExpectedTime).toMinutes();
            if (minutesUntil <= 5 && !waitingToken.isAlertSent()) {
                waitingToken.setAlertSent(true);
                sendNotification(waitingToken.getUser(), waitingToken, "BE_READY");
            }
            tokenRepository.save(waitingToken);
        }
    }

    private int calculateDynamicAverageServiceTime(ServiceQueue queue) {
        java.util.List<Token> completedTokens = tokenRepository.findTop50ByServiceQueueAndStatusOrderByCompletedAtDesc(queue, TokenStatus.COMPLETED);
        if (completedTokens.isEmpty()) {
            return queue.getAverageServiceTimeMinutes();
        }
        
        long totalDurationMinutes = 0;
        int validTokens = 0;
        for (Token t : completedTokens) {
            if (t.getActivatedAt() != null && t.getCompletedAt() != null) {
                long duration = java.time.Duration.between(t.getActivatedAt(), t.getCompletedAt()).toMinutes();
                totalDurationMinutes += duration;
                validTokens++;
            }
        }
        
        if (validTokens == 0) {
            return queue.getAverageServiceTimeMinutes();
        }
        
        int average = (int) (totalDurationMinutes / validTokens);
        return average > 0 ? average : 1; // Ensure at least 1 min average
    }

    private void sendNotification(User user, Token token, String eventType) {
        NotificationPayload payload = new NotificationPayload();
        payload.setUserId(user.getId().toString());
        payload.setEventType(eventType);
        payload.setChannels(Collections.singletonList("EMAIL"));
        
        payload.setRecipient(new NotificationPayload.Recipient(user.getEmail(), user.getPhone()));
        
        Map<String, String> templateData = new HashMap<>();
        templateData.put("customerName", user.getUsername());
        templateData.put("tokenNumber", token.getTokenNumber());
        payload.setTemplateData(templateData);

        kafkaProducerService.sendNotificationEvent(payload);
    }

    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void periodicallyRecalculateETAs() {
        java.util.List<ServiceQueue> activeQueues = serviceQueueRepository.findAll();
        for (ServiceQueue queue : activeQueues) {
            if (queue.getType() == QueueType.LIVE) {
                int dynamicAverageTime = calculateDynamicAverageServiceTime(queue);
                recalculateExpectedTimes(queue, dynamicAverageTime);
            }
        }
    }
}
