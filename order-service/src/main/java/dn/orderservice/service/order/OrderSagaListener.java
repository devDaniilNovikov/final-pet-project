package dn.orderservice.service.order;
import dn.orderservice.entity.OrderEntity;
import dn.orderservice.enums.OrderStatus;
import dn.orderservice.exception.OrderNotFoundException;
import dn.orderservice.repository.OrderRepository;

import dn.shared.idempotency.ProcessedEventEntity;
import dn.shared.idempotency.ProcessedEventRepository;
import dn.shared.event.inventory.InventoryReservationFailedEvent;
import dn.shared.event.payment.PaymentFailedEvent;
import dn.shared.event.payment.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final ProcessedEventRepository processedEventRepository;
    private final OrderRepository orderRepository;
    private final OutboxService outboxService;

    private void processEvent(UUID eventId){
        processedEventRepository.save(ProcessedEventEntity.builder()
                .id(eventId)
                .build());
    }




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
                                     UUID mappedOrderId,
                                     OrderStatus orderStatus,
                                     Consumer<OrderEntity> consumer){
        if (!processedEventRepository.existsById(eventId)) {
            OrderEntity order = orderRepository.findByIdWithLock(mappedOrderId)
                    .orElseThrow(() -> new OrderNotFoundException(
                            MessageFormat.format("Order with id: {0} not found", mappedOrderId)
                    ));
            order.setOrderStatus(orderStatus);
            orderRepository.save(order);
            processEvent(eventId);
            consumer.accept(order);
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

