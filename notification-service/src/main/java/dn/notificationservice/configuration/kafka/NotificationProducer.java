package dn.notificationservice.configuration.kafka;
import dn.notificationservice.entity.NotificationEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private static final int TIMEOUT = 5;

    private final KafkaTemplate <String,NotificationEntity> kafkaTemplate;

    @Value("${app.kafka.custom-topics.notification-service-dlt}")
    private String notificationServiceDltTopic;


    public void sendMessageToDeadLetterTopic(NotificationEntity notificationEntity) {
        try {
            kafkaTemplate.send(
                    notificationServiceDltTopic,
                    notificationEntity.getId().toString(),
                    notificationEntity
            ).get(TIMEOUT, TimeUnit.SECONDS);
        }catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted",ex);
            throw new RuntimeException(ex);
        }catch (TimeoutException | ExecutionException ex){
            log.error("Notification service failed",ex);
            throw new RuntimeException(ex);
        }
    }
}
