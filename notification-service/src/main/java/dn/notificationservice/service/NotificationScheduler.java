package dn.notificationservice.service;
import dn.notificationservice.configuration.kafka.NotificationProducer;
import dn.notificationservice.entity.NotificationEntity;
import dn.notificationservice.enums.NotificationStatus;
import dn.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {


    @Value("${app.schedule.concurrency.timeout}")
    private int concurrencyTimeout;

    @Value("${app.schedule.notification.batch-size}")
    private int batchSize;

    private final NotificationRepository notificationRepository;
    private final NotificationProducer notificationProducer;
    private final NotificationTransactionService notificationTransactionService;
    private final ExecutorService executorService;


    @Scheduled(fixedDelayString = "${app.schedule.fixedDelay.value}")
    public void scheduleNotifications() {
        List<NotificationEntity> notifications = notificationTransactionService.executeInTransaction(batchSize);
        executeAsync(notifications);
    }



    private void executeAsync(List<NotificationEntity> notifications) {
        List<Future<NotificationEntity>> futures = new ArrayList<>();
        for (NotificationEntity notification : notifications) {
            futures.add(executorService.submit(() -> {
                try {
                    notificationProducer.sendMessageToDeadLetterTopic(notification);
                    notification.setNotificationStatus(NotificationStatus.SENT);
                }catch (Exception e) {
                    log.error("Failed to send notification {}: ", notification.getId(), e);
                    notification.setNotificationStatus(NotificationStatus.FAILED);
                }
                return notification;
            }));
        }
        var processedNotifications = executeAwaitFutures(futures);
        notificationRepository.saveAll(processedNotifications);
    }

    private List<NotificationEntity> executeAwaitFutures(List<Future<NotificationEntity>> futures) {
        List<NotificationEntity> notifications = new ArrayList<>();
        for (Future<NotificationEntity> future : futures) {
            try {
                var successNotification = future.get(concurrencyTimeout,TimeUnit.SECONDS);
                notifications.add(successNotification);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while sending notifications: ", e);
                break;
            } catch (TimeoutException | ExecutionException e) {
                log.error("Timeout occurred while sending notifications: ",e);

            }
        }
        return notifications;
    }
}
