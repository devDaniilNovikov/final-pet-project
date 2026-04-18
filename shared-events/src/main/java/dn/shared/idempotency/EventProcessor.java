package dn.shared.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventProcessor {

    private final ProcessedEventRepository processedEventRepository;


    public void processEvent(UUID eventId){
        processedEventRepository.save(ProcessedEventEntity.builder()
                .id(eventId)
                .build());
    }
}
