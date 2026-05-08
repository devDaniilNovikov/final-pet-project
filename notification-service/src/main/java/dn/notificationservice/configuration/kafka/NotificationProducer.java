package dn.notificationservice.configuration.kafka;

import dn.notificationservice.entity.NotificationEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final KafkaTemplate <String,NotificationEntity> kafkaTemplate;

    @Value("${app.kafka.custom-topics.notification-service-dlt}")
    private String notificationServiceDltTopic;

    @Transactional
    public void sendMessageToDeadLetterTopic(NotificationEntity notificationEntity) {
        kafkaTemplate.send(
                notificationServiceDltTopic,
                notificationEntity.getId().toString(),
                notificationEntity
        );
    }
}
