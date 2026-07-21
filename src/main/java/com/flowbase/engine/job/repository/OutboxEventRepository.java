package com.flowbase.engine.job.repository;

import com.flowbase.engine.job.domain.OutboxEvent;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    @Query(value = "SELECT * FROM OUTBOX_EVENT WHERE (STATUS = 'PENDING' OR (STATUS = 'PROCESSING' AND LEASED_UNTIL < NOW()))" +
            " ORDER BY CREATED_AT LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> leasePendingJobs(@Param("limit") int limit);
    List<OutboxEvent> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
