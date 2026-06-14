package com.smartqueue.repository;

import com.smartqueue.domain.ServiceQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceQueueRepository extends JpaRepository<ServiceQueue, UUID> {
    List<ServiceQueue> findByIsActiveTrue();
}
