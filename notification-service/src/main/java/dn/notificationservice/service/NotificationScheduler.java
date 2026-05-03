package dn.notificationservice.service;

import dn.notificationservice.entity.NotificationEntity;
import dn.notificationservice.enums.NotificationStatus;
import dn.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;


    @Scheduled(fixedDelay = 60000)
    public void scheduleNotification(UUID sourceEventId,UUID recipientAccountId){
        List<NotificationEntity> notifications = notificationRepository.findAllBySourceEventIdAndRecipientAccountId(
                sourceEventId,recipientAccountId
        );
        var notification = notificationService.validNotifications(notifications);
        List<UUID> notificationList = notification.getNotificationStatusMap()
                .keySet()
                .stream()
                .toList();
        notificationRepository.deleteAllByIdInBatch(notificationList);
        log.info("Deleted notifications with ids: {}",notificationList);

    }

}
