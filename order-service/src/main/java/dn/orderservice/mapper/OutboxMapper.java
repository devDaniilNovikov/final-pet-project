package dn.orderservice.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dn.orderservice.entity.OrderEntity;
import dn.orderservice.entity.OrderItemEntity;
import dn.shared.event.order.OrderCreatedEvent;
import dn.shared.outbox.OutboxEntity;
import dn.shared.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxMapper {

    private final ObjectMapper objectMapper;
    private final OrderItemMapper orderItemMapper;

    /**
     * OrderEntity + items → OutboxEntity (payload = serialized OrderCreatedEvent)
     */
    public OutboxEntity toOutboxEntity(OrderEntity order,
                                       List<OrderItemEntity> items,
                                       String topic) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(order.getId())
                .buyerId(order.getBuyerId())
                .totalPrice(order.getTotalPrice())
                .orderItems(orderItemMapper.toDtoList(items))
                .build();

        return OutboxEntity.builder()
                .id(event.eventId())
                .aggregateId(order.getId())
                .topic(topic)
                .payload(serialize(event))
                .outboxStatus(OutboxStatus.PENDING)
                .build();
    }

    /**
     * OutboxEntity → OrderCreatedEvent (десериализация payload для Relay)
     */
    public OrderCreatedEvent toOrderCreatedEvent(OutboxEntity outboxEntity) {
        try {
            return objectMapper.readValue(outboxEntity.getPayload(), OrderCreatedEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to deserialize outbox payload for id: " + outboxEntity.getId(), e);
        }
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event", e);
        }
    }
}

