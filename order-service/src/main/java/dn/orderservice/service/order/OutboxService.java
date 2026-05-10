package dn.orderservice.service.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dn.orderservice.entity.OrderEntity;
import dn.orderservice.mapper.OrderItemMapper;
import dn.shared.event.inventory.InventoryReservationFailedEvent;
import dn.shared.event.inventory.InventoryReservedEvent;
import dn.shared.event.order.OrderCancelledEvent;
import dn.shared.event.order.OrderConfirmedEvent;
import dn.shared.event.order.OrderPaidEvent;
import dn.shared.event.payment.PaymentFailedEvent;
import dn.shared.event.payment.PaymentSuccessEvent;
import dn.shared.outbox.OutboxEntity;
import dn.shared.outbox.OutboxRepository;
import dn.shared.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import dn.shared.event.order.OrderCreatedEvent;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectsMapper;
    private final OrderItemMapper orderItemMapper;

    @Value("${app.kafka.events.order-confirmed}")
    private String orderConfirmedTopic;

    @Value("${app.kafka.events.order-created}")
    private String orderCreatedTopic;

    @Value("${app.kafka.events.order-cancelled}")
    private String orderCancelledTopic;

    @Value("${app.kafka.events.order-paid}")
    private String orderPaidTopic;

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOutbox(OrderEntity order, InventoryReservedEvent event) {
        var outboxEvent = OrderConfirmedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(order.getId())
                .buyerId(order.getBuyerId())
                .build();
        persist(outboxEvent, outboxEvent.eventId() , order.getId(), orderConfirmedTopic);
        log.info("Outbox created: OrderConfirmedEvent orderId={}", order.getId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOutbox(OrderEntity order, InventoryReservationFailedEvent event) {
        var outboxEvent = OrderCancelledEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(order.getId())
                .reason(event.reason())
                .orders(orderItemMapper.toDtoList(order.getOrderItemEntities()))
                .build();
        persist(outboxEvent, outboxEvent.eventId(), order.getId(), orderCancelledTopic);
        log.info("Outbox created: OrderCancelledEvent (reservation failed) orderId={}", order.getId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOutbox(OrderEntity order) {
        var outboxEvent = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(order.getId())
                .totalPrice(order.getTotalPrice())
                .buyerId(order.getBuyerId())
                .orderItems(orderItemMapper.toDtoList(order.getOrderItemEntities()))
                .build();
        persist(outboxEvent, outboxEvent.eventId(), order.getId(), orderCreatedTopic);
        log.info("Outbox created: OrderCreatedEvent  orderId={}", order.getId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOutbox(OrderEntity order, PaymentSuccessEvent event) {
        var outboxEvent = OrderPaidEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(order.getId())
                .buyerId(order.getBuyerId())
                .amount(order.getTotalPrice())
                .build();
        persist(outboxEvent,outboxEvent.eventId(), order.getId(),  orderPaidTopic);
        log.info("Outbox created: OrderPaidEvent orderId={}", order.getId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOutbox(OrderEntity order, PaymentFailedEvent event) {
        var outboxEvent = OrderCancelledEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(order.getId())
                .reason(event.reason())
                .orders(orderItemMapper.toDtoList(order.getOrderItemEntities()))
                .build();
        persist(outboxEvent,outboxEvent.eventId(), order.getId(), orderCancelledTopic);
        log.info("Outbox created: OrderCancelledEvent (payment failed) orderId={}", order.getId());
    }

    private void persist(Object event,
                         UUID eventId,
                         UUID aggregateId,
                         String topic) {
        OutboxEntity outbox = OutboxEntity.builder()
                .id(eventId)
                .aggregateId(aggregateId)
                .topic(topic)
                .payload(serialize(event))
                .outboxStatus(OutboxStatus.PENDING)
                .build();
        outboxRepository.save(outbox);
    }

    private String serialize(Object event) {
        try {
            return objectsMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event", e);
        }
    }

}
