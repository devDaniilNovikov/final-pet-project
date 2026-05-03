package dn.accountservice.config.kafka;

import dn.shared.event.account.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${custom.kafka.topics.account-created}")
    private String accountCreatedTopic;

    @Value("${custom.kafka.topics.account-banned}")
    private String accountBannedTopic;

    @Value("${custom.kafka.topics.account-unbanned}")
    private String accountUnbannedTopic;

    @Value("${custom.kafka.topics.account-deleted}")
    private String accountDeletedTopic;

    @Value("${custom.kafka.topics.account-updated}")
    private String accountUpdatedTopic;


    @Transactional
    public void send(String topic,
                     String key,
                     Object payload) {

        try {
            kafkaTemplate.send(topic, key, payload).get();
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            log.error("Failed to send message to kafka topic={}",topic,e);
            throw new RuntimeException("Kafka send interrupted", e);
        }
        catch (Exception e){
            log.error("Failed to send message to kafka topic={}",topic,e);
            throw new RuntimeException("Kafka send interrupted",e);
        }
    }



    public void sendAccountCreatedEvent(AccountCreatedEvent event) {
        send(accountCreatedTopic,event.accountId().toString(),event);
    }

    public void sendAccountUpdatedEvent(AccountUpdatedEvent event){
        send(accountUpdatedTopic,event.id().toString(),event);
    }

    public void sendAccountBannedEvent(AccountBannedEvent event) {
        send(accountBannedTopic, event.id().toString(),event);
    }

    public void sendAccountDeletedEvent(AccountDeletedEvent event) {
        send(accountDeletedTopic, event.id().toString(),event);
    }

    public void sendAccountUnbannedEvent(AccountUnbannedEvent event) {
        send(accountUnbannedTopic, event.id().toString(),event);

    }

}
