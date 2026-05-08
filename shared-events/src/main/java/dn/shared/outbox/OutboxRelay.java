package dn.shared.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {



    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxTransactionService outboxTransactionService;

    private static final int PAGE_LIMIT = 10;




    @Scheduled(fixedDelay=1000)
    public void processInPOutboxStatus() {
        List<OutboxEntity> outboxes = outboxRepository.findPendingWithLock(PAGE_LIMIT);
        for (OutboxEntity entity : outboxes) {
            outboxTransactionService.writeInTransaction(entity);
            try {
                kafkaTemplate.send(
                                entity.getTopic(),
                                entity.getAggregateId().toString(),
                                entity.getPayload())
                        .get(5, TimeUnit.SECONDS);
            } catch (InterruptedException  |TimeoutException ex) {
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
