package dn.notificationservice.service;

import dn.notificationservice.entity.NotificationEntity;
import dn.notificationservice.enums.NotificationStatus;
import dn.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationTransactionService {




    private final TransactionTemplate transactionTemplate;
    private final NotificationRepository notificationRepository;



    public List<NotificationEntity> executeInTransaction(int batchSize) {
        Pageable pageable = PageRequest.of(0, batchSize);
        return transactionTemplate.execute(tx->{
            var notifications = notificationRepository.findAllByNotificationStatus(
                    pageable,
                    NotificationStatus.FAILED
            );
            notifications.forEach(notificationEntity -> notificationEntity.setNotificationStatus(NotificationStatus.IN_PROGRESS));
            return notificationRepository.saveAll(notifications);
        });

    }
}
