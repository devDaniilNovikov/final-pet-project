package dn.productservice.service.product;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dn.productservice.exception.SerializeException;
import dn.productservice.service.InventoryBuilder;
import dn.shared.event.order.OrderCreatedEvent;
import dn.shared.outbox.OutboxEntity;
import dn.shared.outbox.OutboxRepository;
import dn.shared.outbox.OutboxStatus;
import dn.shared.event.product.ProductCreateEvent;
import dn.shared.event.product.ProductUpdatedEvent;
import dn.shared.event.product.ProductDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import dn.shared.event.order.OrderCancelledEvent;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import dn.shared.outbox.OutboxableEvent;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductOutboxService {

    @Value("${app.kafka.events.item-reserved}")
    private String itemReservedEvent;

    @Value("${app.kafka.events.item-reserved-failed}")
    private String itemReservedFailedEvent;

    @Value("${app.kafka.events.product-created}")
    private String productCreatedEvent;

    @Value("${app.kafka.events.product-updated}")
    private String productUpdatedEvent;

    @Value("${app.kafka.events.product-deleted}")
    private String productDeletedEvent;


    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectsMapper;
    private final InventoryBuilder inventoryBuilder;

    private void createOutboxEvent(UUID id,
                                   UUID eventId,
                                   String topic,
                                   Object payload){
        var outbox = buildOutbox(id,eventId,topic, payload);
        outboxRepository.save(outbox);
    }


    private OutboxEntity buildOutbox(UUID id,
                                     UUID eventId,
                                     String topic,
                                     Object payload) {
        return OutboxEntity.builder()
                .id(eventId)
                .aggregateId(id)
                .payload(serialize(payload))
                .topic(topic)
                .outboxStatus(OutboxStatus.PENDING)
                .build();
    }


    private String serialize(Object payload){
        try {
            return objectsMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new SerializeException("Could not serialize payload", e);
        }
    }

    private void sendOrderCreatedOutboxEvent(OrderCreatedEvent event){
        var infoForPayload = inventoryBuilder.buildInventoryReservedEvent(
                event.orderId(),
                event.eventId()
        );
        var outbox = buildOutbox(
                event.orderId(),
                event.eventId(),
                itemReservedEvent,
                infoForPayload
        );
        outboxRepository.save(outbox);
    }

    private void sendOrderFailedOutboxEvent(UUID orderId,
                                            UUID eventId,
                                            String reason){
        var infoForPayload = inventoryBuilder.buildInventoryReservationFailedEvent(
                orderId,
                eventId,
                reason
        );
        var outbox = buildOutbox(
                orderId,
                eventId,
                itemReservedFailedEvent,
                infoForPayload
        );
        outboxRepository.save(outbox);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOutbox(OrderCreatedEvent event){
        sendOrderCreatedOutboxEvent(event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOutbox(OrderCreatedEvent event,
                             String reason){
        sendOrderFailedOutboxEvent(event.orderId(),event.eventId(),reason);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOutbox(OrderCancelledEvent event,
                             String reason){
        sendOrderFailedOutboxEvent(event.orderId(),event.eventId(),reason);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOutbox(ProductCreateEvent event){
        createOutboxEvent(event.id(),event.eventId(),productCreatedEvent,event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOutbox(ProductUpdatedEvent event){
        createOutboxEvent(event.id(),event.eventId(),productUpdatedEvent,event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOutbox(ProductDeletedEvent event){
        createOutboxEvent(event.id(),event.eventId(),productDeletedEvent,event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOutbox(List<ProductDeletedEvent> events){
        createBatchOutbox(events,productDeletedEvent);
    }


    private void createBatchOutbox(List<? extends OutboxableEvent> events,
                                   String topic){
        var outbox = events.stream()
                .map(event -> buildOutbox(
                        event.id(),
                        event.eventId(),
                        topic,
                        event))
                .toList();
        outboxRepository.saveAll(outbox);
    }



}
