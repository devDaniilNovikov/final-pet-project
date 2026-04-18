package dn.productservice.service;


import dn.productservice.event.ProductCreateEvent;
import dn.productservice.event.ProductDeletedEvent;
import dn.productservice.event.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    @Value("${app.kafka.events.product-created}")
    private String productCreatedEvent;

    @Value("${app.kafka.events.product-updated}")
    private String productUpdatedEvent;

    @Value("${app.kafka.events.product-deleted}")
    private String productDeletedEvent;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendProductCreatedEvent(ProductCreateEvent event) {
        String key = event.id() != null ? event.id().toString() : null;
        kafkaTemplate.send(productCreatedEvent, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send ProductCreatedEvent for productId={}: {}", event.id(), ex.getMessage());
                    }
                });
    }

    public void sendProductUpdatedEvent(ProductUpdatedEvent event) {
        kafkaTemplate.send(productUpdatedEvent, event.id(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send ProductUpdatedEvent for productId={}: {}", event.id(), ex.getMessage());
                    }
                });
    }

    public void sendProductDeletedEvent(ProductDeletedEvent event) {
        String key = event.productId() != null ? event.productId()
                : (event.ids() != null && !event.ids().isEmpty() ? event.ids().get(0) : null);
        kafkaTemplate.send(productDeletedEvent, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send ProductDeletedEvent for key={}: {}", key, ex.getMessage());
                    }
                });
    }
}
