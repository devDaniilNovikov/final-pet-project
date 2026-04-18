package dn.productservice.service.product;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dn.shared.event.inventory.InventoryReservationFailedEvent;
import dn.shared.event.inventory.InventoryReservedEvent;
import dn.shared.event.order.OrderCreatedEvent;
import dn.shared.outbox.OutboxEntity;
import dn.shared.outbox.OutboxRepository;
import dn.shared.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductOutboxService {

    @Value("${app.kafka.events.item-reserved}")
    private String itemReservedEvent;

    @Value("${app.kafka.events.item-reserved-failed}")
    private String itemReservedFailedEvent;


    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectsMapper;


    public OutboxEntity createOutbox(OrderCreatedEvent orderCreatedEvent){
        try {
            var payload = InventoryReservedEvent.builder()
                    .orderId(orderCreatedEvent.orderId())
                    .eventId(orderCreatedEvent.eventId())
                    .build();
            OutboxEntity outbox = OutboxEntity.builder()
                    .id(payload.eventId())
                    .aggregateId(payload.orderId())
                    .payload(objectsMapper.writeValueAsString(payload))
                    .topic(itemReservedEvent)
                    .outboxStatus(OutboxStatus.PENDING)
                    .build();
            outboxRepository.save(outbox);
            log.info("Successful sending order event, payload: {}",outbox.getPayload());
            return outbox;
        }catch (JsonProcessingException ex){
            logException(ex);
            return null;
        }
    }

    public OutboxEntity createOutbox(OrderCreatedEvent orderCancelledEvent,
                                     String reason){
        try {
            var payload = InventoryReservationFailedEvent.builder()
                    .orderId(orderCancelledEvent.orderId())
                    .eventId(orderCancelledEvent.eventId())
                    .reason(reason)
                    .build();
            OutboxEntity outbox = OutboxEntity.builder()
                    .id(orderCancelledEvent.eventId())
                    .aggregateId(orderCancelledEvent.orderId())
                    .payload(objectsMapper.writeValueAsString(payload))
                    .topic(itemReservedFailedEvent)
                    .outboxStatus(OutboxStatus.PENDING)
                    .build();
            outboxRepository.save(outbox);
            log.error("Error while sending order event");
            return outbox;
        }catch (JsonProcessingException ex){
            logException(ex);
            return null;
        }
    }


    private static void logException(JsonProcessingException ex){
        log.error("Can't serialize value cause: {}",ex.getMessage());
    }
}
