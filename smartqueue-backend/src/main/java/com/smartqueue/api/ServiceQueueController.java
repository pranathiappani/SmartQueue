package com.smartqueue.api;

import com.smartqueue.application.ServiceQueueService;
import com.smartqueue.domain.ServiceQueue;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/queues")
public class ServiceQueueController {

    private final ServiceQueueService serviceQueueService;

    public ServiceQueueController(ServiceQueueService serviceQueueService) {
        this.serviceQueueService = serviceQueueService;
    }

    public record CreateQueueRequest(String name, String description, Integer averageServiceTimeMinutes, 
                                     String type, Integer slotDurationMinutes, Integer slotBufferMinutes, 
                                     String operatingHoursStart, String operatingHoursEnd) {}

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceQueue> createQueue(@RequestBody CreateQueueRequest request) {
        ServiceQueue queue = serviceQueueService.createQueue(
                request.name(), request.description(), request.averageServiceTimeMinutes(),
                request.type(), request.slotDurationMinutes(), request.slotBufferMinutes(),
                request.operatingHoursStart(), request.operatingHoursEnd()
        );
        return ResponseEntity.ok(queue);
    }

    @GetMapping
    public ResponseEntity<List<ServiceQueue>> getActiveQueues() {
        return ResponseEntity.ok(serviceQueueService.getAllActiveQueues());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceQueue> getQueue(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceQueueService.getQueue(id));
    }

    @PutMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceQueue> disableQueue(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceQueueService.disableQueue(id));
    }
}
