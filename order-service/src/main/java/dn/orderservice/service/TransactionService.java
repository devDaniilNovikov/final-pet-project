package dn.orderservice.service;


import dn.orderservice.entity.OutboxEntity;
import dn.orderservice.enums.OutboxStatus;
import dn.orderservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Service
@Slf4j
public class TransactionService {

    private final TransactionTemplate transactionTemplate;
    private final OutboxRepository outboxRepository;


    public TransactionService(final PlatformTransactionManager platformTransactionManager,
                              OutboxRepository outboxRepository) {
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.outboxRepository = outboxRepository;
    }


    public void writeInTransaction(OutboxEntity outboxEntity){
        transactionTemplate.executeWithoutResult(tx->{
            outboxEntity.setOutboxStatus(OutboxStatus.IN_PROGRESS);
            outboxRepository.save(outboxEntity);
        });
    }

    public void markAsSent(OutboxEntity outboxEntity){
        transactionTemplate.executeWithoutResult(tx->{
            outboxEntity.setOutboxStatus(OutboxStatus.SENT);
            outboxEntity.setProcessedAt(LocalDateTime.now());
            outboxRepository.save(outboxEntity);
        });
    }

    public void markAsFailed(OutboxEntity outboxEntity){
        transactionTemplate.executeWithoutResult(tx->{
            outboxEntity.setOutboxStatus(OutboxStatus.FAILED);
            outboxRepository.save(outboxEntity);
        });
    }
}
