package dn.accountservice.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dn.shared.event.account.*;
import dn.shared.outbox.OutboxEntity;
import dn.shared.outbox.OutboxRepository;
import dn.shared.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountOutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;


    @Value("${custom.kafka.topics.account-created}")
    private String accountCreatedTopic;

    @Value("${custom.kafka.topics.account-updated}")
    private String accountUpdatedTopic;

    @Value("${custom.kafka.topics.account-banned}")
    private String accountBannedTopic;

    @Value("${custom.kafka.topics.account-unbanned}")
    private String accountUnbannedTopic;

    @Value("${custom.kafka.topics.account-deleted}")
    private String accountDeletedTopic;


    private void saveOutbox(UUID id,
                            UUID eventId,
                            String topic,
                            Object payload){
        var outbox = OutboxEntity.builder()
                .id(eventId)
                .aggregateId(id)
                .topic(topic)
                .payload(serialize(payload))
                .outboxStatus(OutboxStatus.PENDING)
                .build();
        outboxRepository.save(outbox);

    }

    private String serialize(Object payload){
        try {
            return objectMapper.writeValueAsString(payload);
        }catch (JsonProcessingException ex){
            log.error("Can't serialize object cause: {}", ex.getMessage());
            throw new RuntimeException(ex);
        }

    }

    public void createOutbox(AccountCreatedEvent event){
        saveOutbox(event.accountId(),event.eventId(),accountCreatedTopic,event);
    }

    public void createOutbox(AccountUpdatedEvent event){
        saveOutbox(event.id(),event.eventId(),accountUpdatedTopic,event);
    }

    public void createOutbox(AccountBannedEvent event){
        saveOutbox(event.id(),event.eventId(),accountBannedTopic,event);
    }

    public void createOutbox(AccountUnbannedEvent event){
        saveOutbox(event.id(),event.eventId(),accountUnbannedTopic,event);
    }

    public void createOutbox(AccountDeletedEvent event){
        saveOutbox(event.id(),event.eventId(),accountDeletedTopic,event);
    }


    
    
    
    
    
}
