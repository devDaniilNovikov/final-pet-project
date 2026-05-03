package dn.notificationservice.repository;

import dn.notificationservice.entity.NotificationEntity;
import dn.notificationservice.enums.NotificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    boolean existsBySourceEventId(UUID sourceEventId);


    List<NotificationEntity> findAllBySourceEventIdAndRecipientAccountId(UUID sourceEventId,
                                                                         UUID recipientAccountId);

    @Query("""
     select n from NotificationEntity n where n.notificationStatus = :status and n.nextRetryTime <= :nextRetryTime 

           """)
    List<NotificationEntity> findAllByNotificationStatusAndNextRetryTimeLessThanEqual(
            @Param("status") NotificationStatus status,
            @Param("nextRetryTime") Instant nextRetryTime,
            Pageable pageable
    );

    boolean existsByRecipientAccountId(UUID recipientAccountId);

    boolean existsBySourceEventIdAndRecipientAccountId(UUID sourceEventId,
                                                       UUID recipientAccountId);




}
