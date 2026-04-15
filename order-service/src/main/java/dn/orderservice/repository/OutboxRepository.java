package dn.orderservice.repository;

import dn.orderservice.entity.OutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEntity, UUID> {

    @Query(value = """
              SELECT * FROM marketplace.outbox                                                
              WHERE outbox_status = 'PENDING'                 
              ORDER BY created_at                                                    
              LIMIT :limit                                                                                                                                                   \s
              FOR UPDATE SKIP LOCKED  
            """, nativeQuery = true)
    List<OutboxEntity> findPendingWithLock(@Param("limit") int limit);
}
