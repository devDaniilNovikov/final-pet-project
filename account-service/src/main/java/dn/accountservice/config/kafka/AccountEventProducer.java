package dn.accountservice.config.kafka;


import dn.accountservice.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${custom.kafka.topics.account-created}")
    private String accountCreatedTopic;

    @Value("${custom.kafka.topics.account-banned}")
    private String accountBannedTopic;

    @Value("${custom.kafka.topics.account-deleted}")
    private String accountDeletedTopic;


    public void sendAccountCreatedEvent(AccountCreatedEvent event) {
        kafkaTemplate.send(accountCreatedTopic, event.id(),event);
    }

    public void sendAccountBannedEvent(AccountBannedEvent event) {
        kafkaTemplate.send(accountBannedTopic, event.id(),event);
    }

    public void sendAccountDeletedEvent(AccountDeletedEvent event) {
        kafkaTemplate.send(accountDeletedTopic, event.id(),event);

    }

}
