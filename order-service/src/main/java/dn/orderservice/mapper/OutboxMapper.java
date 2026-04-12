package dn.orderservice.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dn.orderservice.entity.OrderEntity;
import dn.orderservice.entity.OrderItemEntity;
import dn.orderservice.entity.OutboxEntity;
import dn.orderservice.enums.OutboxStatus;
import dn.shared.event.order.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

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
                .orderId(order.getId().toString())
                .buyerId(order.getBuyerId().toString())
                .totalPrice(order.getTotalPrice())
                .orderItems(orderItemMapper.toDtoList(items))
                .build();

        return OutboxEntity.builder()
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

