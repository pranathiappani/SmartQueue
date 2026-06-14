package com.smartqueue.api;

import com.smartqueue.api.dto.AnalyticsSummary;
import com.smartqueue.api.dto.QueueStat;
import com.smartqueue.domain.ServiceQueue;
import com.smartqueue.domain.TokenStatus;
import com.smartqueue.repository.ServiceQueueRepository;
import com.smartqueue.repository.TokenRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final ServiceQueueRepository serviceQueueRepository;
    private final TokenRepository tokenRepository;

    public AnalyticsController(ServiceQueueRepository serviceQueueRepository, TokenRepository tokenRepository) {
        this.serviceQueueRepository = serviceQueueRepository;
        this.tokenRepository = tokenRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummary> getSummary() {
        AnalyticsSummary summary = new AnalyticsSummary();
        summary.setTotalQueues(serviceQueueRepository.count());
        summary.setTotalTokensWaiting(tokenRepository.countByStatus(TokenStatus.WAITING));
        summary.setTotalTokensCompleted(tokenRepository.countByStatus(TokenStatus.COMPLETED));

        List<QueueStat> queueStats = new ArrayList<>();
        List<ServiceQueue> queues = serviceQueueRepository.findAll();
        for (ServiceQueue queue : queues) {
            QueueStat stat = new QueueStat();
            stat.setQueueName(queue.getName());
            stat.setTotalTokens(tokenRepository.countByServiceQueue(queue));
            stat.setAvgWaitTime(queue.getAverageServiceTimeMinutes());
            queueStats.add(stat);
        }
        summary.setQueueStats(queueStats);

        return ResponseEntity.ok(summary);
    }
}
