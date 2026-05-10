package dn.shared.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {


    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxTransactionService outboxTransactionService;
    private final ExecutorService executorService;
    private static final int TIMEOUT = 5;
    private static final int PAGE_LIMIT = 10;


    @Scheduled(fixedDelayString = "${spring.task.fixedDelay.value}")
    public void processInPOutboxStatus() {
        List<OutboxEntity> outboxes = outboxTransactionService.writeListInTransaction(PAGE_LIMIT);
        executeAsync(outboxes);
    }

    private void executeAsync(List<OutboxEntity> outboxes) {
        List<Future<?>> futures = new ArrayList<>();
        for (OutboxEntity entity : outboxes) {
            futures.add(executorService.submit(() -> {
                try {
                    kafkaTemplate.send(
                                    entity.getTopic(),
                                    entity.getAggregateId().toString(),
                                    entity.getPayload())
                            .get(TIMEOUT, TimeUnit.SECONDS);
                    outboxTransactionService.markAsSent(entity);
                } catch (ExecutionException | TimeoutException e) {
                    outboxTransactionService.markAsFailed(entity);
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }));
        }
        executeAwaitFutures(futures);
    }
    private void executeAwaitFutures(List<Future<?>> futures) {
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ex);
                } catch (ExecutionException ex) {
                    log.error("Execution exception: ",ex);
                }
            }
    }
}
