package com.smartqueue.application;

import com.smartqueue.domain.ServiceQueue;
import com.smartqueue.domain.QueueType;
import com.smartqueue.repository.ServiceQueueRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ServiceQueueService {

    private final ServiceQueueRepository serviceQueueRepository;

    public ServiceQueueService(ServiceQueueRepository serviceQueueRepository) {
        this.serviceQueueRepository = serviceQueueRepository;
    }

    public ServiceQueue createQueue(String name, String description, Integer averageServiceTimeMinutes, 
                                    String typeStr, Integer slotDuration, Integer slotBuffer, 
                                    String operatingStartStr, String operatingEndStr) {
        ServiceQueue queue = new ServiceQueue(name, description, averageServiceTimeMinutes);
        
        if (typeStr != null && typeStr.equalsIgnoreCase("SCHEDULED")) {
            queue.setType(QueueType.SCHEDULED);
            queue.setSlotDurationMinutes(slotDuration);
            queue.setSlotBufferMinutes(slotBuffer);
            queue.setOperatingHoursStart(java.time.LocalTime.parse(operatingStartStr));
            queue.setOperatingHoursEnd(java.time.LocalTime.parse(operatingEndStr));
        } else {
            queue.setType(QueueType.LIVE);
        }
        
        return serviceQueueRepository.save(queue);
    }

    public List<ServiceQueue> getAllActiveQueues() {
        return serviceQueueRepository.findByIsActiveTrue();
    }

    public ServiceQueue getQueue(UUID id) {
        return serviceQueueRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Queue not found"));
    }

    public ServiceQueue disableQueue(UUID id) {
        ServiceQueue queue = getQueue(id);
        queue.setActive(false);
        return serviceQueueRepository.save(queue);
    }
}
