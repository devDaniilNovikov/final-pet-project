package dn.shared.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventProcessor {

    private final ProcessedEventRepository processedEventRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEvent(UUID eventId){
        processedEventRepository.save(
                ProcessedEventEntity.builder()
                .id(eventId)
                .build()
        );
    }
}
