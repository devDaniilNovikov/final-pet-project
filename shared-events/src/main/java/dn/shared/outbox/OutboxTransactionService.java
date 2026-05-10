package dn.shared.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutboxTransactionService {



    private final TransactionTemplate transactionTemplate;
    private final OutboxRepository outboxRepository;


    public OutboxTransactionService(PlatformTransactionManager platformTransactionManager,
                                    OutboxRepository outboxRepository) {
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.outboxRepository = outboxRepository;
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

    public List<OutboxEntity> writeListInTransaction(int pageLimit){
        return transactionTemplate.execute(tx->{
            List<OutboxEntity> outboxEntities  = outboxRepository.findPendingWithLock(pageLimit);
            outboxEntities.forEach(outboxEntity -> outboxEntity.setOutboxStatus(OutboxStatus.IN_PROGRESS));
            return outboxRepository.saveAll(outboxEntities);
        });
    }
}
