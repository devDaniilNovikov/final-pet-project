package dn.notificationservice.service;

import dn.notificationservice.configuration.kafka.NotificationProducer;
import dn.notificationservice.entity.NotificationEntity;
import dn.notificationservice.enums.NotificationStatus;
import dn.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;


@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    @Value("${app.schedule.notification.batch-size}")
    private int batchSize;

    private final NotificationRepository notificationRepository;
    private final NotificationProducer notificationProducer;
    private final ExecutorService executorService;
    private final NotificationService notificationService;


    @Scheduled(fixedDelayString = "${spring.scheduling.task.fixedDelay.value}")
    public void scheduleNotifications() {
        var pageable = PageRequest.of(0, batchSize);
        List<NotificationEntity> notifications = notificationRepository.findAllByNotificationStatus(
                pageable,
                NotificationStatus.FAILED
        );
        completeAsync(notifications);
        log.info("Deleted notifications: {}", notifications);

    }

    private void completeAsync(List<NotificationEntity> notifications) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (NotificationEntity notification : notifications) {
            CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                notificationProducer.sendMessageToDeadLetterTopic(notification);
                return notification;
            }, executorService).thenAccept(it -> {
                notification.setNotificationStatus(NotificationStatus.SENT);
            }).exceptionally(r->{
                if (r!=null) {
                    log.error(r.getMessage(), r.getCause());
                    throw new RuntimeException(r.getMessage(), r.getCause());

                }
                return null;
            });
            futures.add(future);
        }
        var futuresArray = futures.toArray(new CompletableFuture[0]);
        CompletableFuture.allOf(futuresArray).join();
        notificationRepository.saveAll(notifications);
        futures.forEach(future -> {log.info("Completed notifications: {}", future);});
    }
}
