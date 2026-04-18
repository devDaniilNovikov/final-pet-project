package dn.shared.outbox;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
public class OutboxRelay {



    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxTransactionService outboxTransactionService;

    private static final int PAGE_LIMIT = 10;

    public OutboxRelay(OutboxRepository outboxRepository,
                       KafkaTemplate<String, String> kafkaTemplate,
                       OutboxTransactionService outboxTransactionService) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.outboxTransactionService = outboxTransactionService;
    }


    @Scheduled(fixedDelay = 1000)
    public void processInPOutboxStatus() {
        List<OutboxEntity> outboxes = outboxRepository.findPendingWithLock(PAGE_LIMIT);
        for (OutboxEntity entity : outboxes) {
            outboxTransactionService.writeInTransaction(entity);
            try {
                kafkaTemplate.send(
                                entity.getTopic(),
                                entity.getAggregateId().toString(),
                                entity.getPayload())
                        .get();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                outboxTransactionService.markAsFailed(entity);
                throw new RuntimeException(ex);
            } catch (ExecutionException ex) {
                log.error("Failed to send outbox entry id={}, topic={}", entity.getId(), entity.getTopic(), ex);
                outboxTransactionService.markAsFailed(entity);
                continue;
            }
            outboxTransactionService.markAsSent(entity);
        }

    }
}
