package dn.accountservice.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dn.accountservice.exception.SerializeException;
import dn.shared.event.account.*;
import dn.shared.outbox.OutboxEntity;
import dn.shared.outbox.OutboxRepository;
import dn.shared.outbox.OutboxStatus;
import dn.shared.outbox.OutboxableEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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


    private OutboxEntity buildOutbox(UUID aggregateId,
                                     UUID eventId,
                                     Object payload,
                                     String topic){
        return OutboxEntity.builder()
                .id(eventId)
                .aggregateId(aggregateId)
                .topic(topic)
                .payload(serialize(payload))
                .outboxStatus(OutboxStatus.PENDING)
                .build();

    }





    private String serialize(Object payload){
        try {
            return objectMapper.writeValueAsString(payload);
        }catch (JsonProcessingException ex){
            log.error("Can't serialize object cause: ", ex);
            throw new SerializeException(ex);
        }

    }


    private void createOutboxEvent(UUID aggregateId,
                                   UUID eventId,
                                   Object event,
                                   String topic){
        var outbox = buildOutbox(aggregateId,eventId, event, topic);
        outboxRepository.save(outbox);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOutbox(AccountCreatedEvent event){
        createOutboxEvent(event.id(),event.eventId(),event,accountCreatedTopic);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOutbox(AccountUpdatedEvent event){
        createOutboxEvent(event.id(),event.eventId(),event,accountUpdatedTopic);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOutbox(AccountBannedEvent event){
        createOutboxEvent(event.id(),event.eventId(),event,accountBannedTopic);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOutbox(AccountUnbannedEvent event){
        createOutboxEvent(event.id(),event.eventId(),event,accountUnbannedTopic);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOutbox(AccountDeletedEvent event){
        createOutboxEvent(event.id(),event.eventId(),event,accountDeletedTopic);
    }



    private void createOutboxBatch(List<? extends OutboxableEvent> events,
                             String topic){
        var outboxForSave = events.stream()
                        .map(event -> buildOutbox(
                        event.id(),
                        event.eventId(),
                        event,
                        topic))
                        .toList();
        outboxRepository.saveAll(outboxForSave);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createBannedOutbox(List<AccountBannedEvent> events){
        createOutboxBatch(events,accountBannedTopic);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createDeletedOutbox(List<AccountDeletedEvent> events){
        createOutboxBatch(events,accountDeletedTopic);
    }



    
    
    
    
    
}
