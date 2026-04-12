package dn.orderservice.service;

import dn.orderservice.entity.OutboxEntity;
import dn.orderservice.enums.OutboxStatus;
import dn.orderservice.mapper.OutboxMapper;
import dn.orderservice.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String,Object> kafkaTemplate;
    private final OutboxMapper outboxMapper;
    private final TransactionTemplate transactionTemplate;

    private static final int PAGE_LIMIT = 10;

    public OutboxRelay(OutboxRepository outboxRepository,
                       KafkaTemplate<String, Object> kafkaTemplate,
                       OutboxMapper outboxMapper,
                       PlatformTransactionManager platformTransactionManager) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.outboxMapper = outboxMapper;
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
    }


    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void process(){
        List<OutboxEntity> outboxes = outboxRepository.findPendingWithLock(PAGE_LIMIT);
        for (OutboxEntity entity:outboxes){
            entity.setOutboxStatus(OutboxStatus.IN_PROGRESS);
            try {
                kafkaTemplate.send(
                        entity.getTopic(),
                        entity.getAggregateId().toString(),
                        outboxMapper.toOrderCreatedEvent(entity))
                        .get();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                transactionTemplate.executeWithoutResult(tx->{
                entity.setOutboxStatus(OutboxStatus.FAILED);
                outboxRepository.save(entity);
                });
                throw new RuntimeException(ex);
            } catch (ExecutionException ex) {
                transactionTemplate.executeWithoutResult(tx-> {
                    entity.setOutboxStatus(OutboxStatus.FAILED);
                    log.error("Failed to send outbox entry id={}, topic={}", entity.getId(), entity.getTopic(), ex);
                    outboxRepository.save(entity);;
                });
                continue;
            }
            entity.setOutboxStatus(OutboxStatus.SENT);
            entity.setProcessedAt(LocalDateTime.now());
        }
        outboxRepository.saveAll(outboxes);
    }


}
