package dn.orderservice.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dn.orderservice.entity.OrderEntity;
import dn.orderservice.entity.OutboxEntity;
import dn.orderservice.enums.OutboxStatus;
import dn.orderservice.mapper.OrderItemMapper;
import dn.orderservice.repository.OrderRepository;
import dn.orderservice.repository.OutboxRepository;
import dn.orderservice.repository.ProcessedEventRepository;
import dn.shared.event.inventory.InventoryReservationFailedEvent;
import dn.shared.event.inventory.InventoryReservedEvent;
import dn.shared.event.order.OrderCancelledEvent;
import dn.shared.event.order.OrderConfirmedEvent;
import dn.shared.event.order.OrderItemDto;
import dn.shared.event.order.OrderPaidEvent;
import dn.shared.event.payment.PaymentFailedEvent;
import dn.shared.event.payment.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectsMapper;




    @Value("${app.kafka.events.order-confirmed}")
    private String orderConfirmedTopic;

    @Value("${app.kafka.events.order-paid}")
    private String orderPaidTopic;

    @Value("${app.kafka.events.order-cancelled}")
    private String orderCancelledTopic;

    public void createOutbox(OrderEntity order,
                              InventoryReservedEvent event) {
        try {
            var orderPayload = OrderConfirmedEvent.builder()
                    .orderId(order.getId())
                    .eventId(event.eventId())
                    .buyerId(order.getBuyerId())
                    .build();
            String outboxPayload = objectsMapper.writeValueAsString(orderPayload);
            OutboxEntity outbox = OutboxEntity.builder()
                    .id(event.eventId())
                    .topic(orderConfirmedTopic)
                    .outboxStatus(OutboxStatus.PENDING)
                    .payload(outboxPayload)
                    .build();
            outboxRepository.save(outbox);
        }catch (JsonProcessingException ex){
            logException(ex);
        }
    }

    public void createOutbox(OrderEntity order,
                              InventoryReservationFailedEvent event){
        try {
            var orderPayload = OrderCancelledEvent.builder()
                    .orderId(order.getId())
                    .eventId(event.eventId())
                    .orders(Collections.emptyList()) //TODO:
                    .build();
            var payload = objectsMapper.writeValueAsString(orderPayload);
            var outbox = OutboxEntity.builder()
                    .id(event.eventId())
                    .topic(orderCancelledTopic)
                    .outboxStatus(OutboxStatus.PENDING)
                    .payload(payload)
                    .build();
            outboxRepository.save(outbox);
        }catch (JsonProcessingException ex){
            logException(ex);
        }
    }

    private static void logException(JsonProcessingException ex){
        log.error("Can't serialize future cause: {}",ex.getMessage());
    }

    public void createOutbox(OrderEntity order,
                              PaymentSuccessEvent event) {
        try {
            var orderPayload = OrderPaidEvent.builder()
                    .orderId(order.getId())
                    .eventId(event.eventId())
                    .buyerId(order.getBuyerId())
                    .amount(order.getTotalPrice())
                    .build();
            var payload = objectsMapper.writeValueAsString(orderPayload);
            var outbox = OutboxEntity.builder()
                    .id(event.eventId())
                    .topic(orderPaidTopic)
                    .outboxStatus(OutboxStatus.PENDING)
                    .payload(payload)
                    .build();
            outboxRepository.save(outbox);
        }catch (JsonProcessingException ex){
            logException(ex);
        }
    }

    public void createOutbox(OrderEntity order,
                              PaymentFailedEvent event)  {
        try {
            var orderPayload = OrderCancelledEvent.builder()
                    .orderId(order.getId())
                    .eventId(event.eventId())
                    .orders(Collections.emptyList()) //TODO:
                    .build();
            var payload = objectsMapper.writeValueAsString(orderPayload);
            var outbox = OutboxEntity.builder()
                    .id(event.eventId())
                    .topic(orderCancelledTopic)
                    .outboxStatus(OutboxStatus.PENDING)
                    .payload(payload)
                    .build();
            outboxRepository.save(outbox);
        }catch (JsonProcessingException ex){
            logException(ex);
        }
    }
}
