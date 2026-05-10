package dn.orderservice.service.order;
import dn.orderservice.entity.OrderEntity;
import dn.orderservice.enums.OrderStatus;
import dn.orderservice.exception.OrderNotFoundException;
import dn.orderservice.repository.OrderRepository;

import dn.shared.idempotency.EventProcessor;

import dn.shared.event.inventory.InventoryReservationFailedEvent;
import dn.shared.event.payment.PaymentFailedEvent;
import dn.shared.event.payment.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import dn.shared.event.inventory.InventoryReservedEvent;

import org.springframework.transaction.annotation.Transactional;
import java.text.MessageFormat;
import java.util.UUID;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaListener {

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    private final EventProcessor eventProcessor;



    @KafkaListener(topics = "${app.kafka.events.item-reserved}")
    @Transactional
    public void handleItemReservedEvent(InventoryReservedEvent event) {
        processHandledEvent(
                event.eventId(),
                event.orderId(),
                OrderStatus.CONFIRMED,
                order -> outboxService.createOutbox(order,event));
    }





    @KafkaListener(topics = "${app.kafka.events.item-reserved-failed}")
    @Transactional
    public void handleItemReserveFailedEvent(InventoryReservationFailedEvent event) {
        processHandledEvent(
                event.eventId(),
                event.orderId(),
                OrderStatus.CANCELED,
                order -> outboxService.createOutbox(order,event));
        }


    private void processHandledEvent(UUID eventId,
                                     UUID orderId,
                                     OrderStatus orderStatus,
                                     Consumer<OrderEntity> consumer){
        try {
            eventProcessor.processEvent(eventId);
            OrderEntity order = orderRepository.findByIdWithLock(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(
                            MessageFormat.format("Order with id: {0} not found", orderId)
                    ));
            order.setOrderStatus(orderStatus);
            orderRepository.save(order);
            consumer.accept(order);
        }catch (DataIntegrityViolationException e){
            log.warn("Order with id: {} already exists", orderId);
        }

    }

    @KafkaListener(topics = "${app.kafka.events.payment-success}")
    @Transactional
    public void handlePaymentSuccessEvent(PaymentSuccessEvent event) {
        processHandledEvent(
                event.eventId(),
                event.orderId(),
                OrderStatus.PAID,
                order -> outboxService.createOutbox(order, event)
        );
        }


    @KafkaListener(topics = "${app.kafka.events.payment-failed}")
    @Transactional
    public void handlePaymentFailedEvent(PaymentFailedEvent event) {
        processHandledEvent(
                event.eventId(),
                event.orderId(),
                OrderStatus.CANCELED,
                order -> outboxService.createOutbox(order, event)
        );
    }

}

