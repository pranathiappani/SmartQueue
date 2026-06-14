package com.smartqueue.repository;

import com.smartqueue.domain.ServiceQueue;
import com.smartqueue.domain.Token;
import com.smartqueue.domain.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {
    
    long countByStatus(TokenStatus status);
    
    long countByServiceQueueAndStatus(ServiceQueue serviceQueue, TokenStatus status);
    
    long countByServiceQueue(ServiceQueue serviceQueue);
    
    Optional<Token> findTopByOrderByCreatedAtDesc();
    
    java.util.List<Token> findByServiceQueueIdOrderByIsVipDescSortOrderTimeAsc(UUID serviceQueueId);
    
    java.util.List<Token> findByServiceQueueAndStatusOrderByIsVipDescSortOrderTimeAsc(ServiceQueue serviceQueue, TokenStatus status);
    
    boolean existsByServiceQueueAndScheduledTimeAndStatusNot(ServiceQueue serviceQueue, java.time.LocalDateTime scheduledTime, TokenStatus status);
    
    java.util.List<Token> findByUserUsernameOrderByCreatedAtDesc(String username);
    
    java.util.List<Token> findTop50ByServiceQueueAndStatusOrderByCompletedAtDesc(ServiceQueue serviceQueue, TokenStatus status);
}
