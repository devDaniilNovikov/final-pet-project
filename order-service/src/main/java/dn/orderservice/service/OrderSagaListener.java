package dn.orderservice.service;
import dn.orderservice.entity.OrderEntity;
import dn.orderservice.entity.OrderStatus;
import dn.orderservice.exception.OrderNotFoundException;
import dn.orderservice.mapper.OrderItemMapper;
import dn.orderservice.repository.OrderRepository;
import dn.shared.event.inventory.InventoryReservationFailedEvent;
import dn.shared.event.order.OrderCancelledEvent;
import dn.shared.event.order.OrderConfirmedEvent;
import dn.shared.event.order.OrderPaidEvent;
import dn.shared.event.payment.PaymentFailedEvent;
import dn.shared.event.payment.PaymentSuccessEvent;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import dn.shared.event.inventory.InventoryReservedEvent;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderSagaListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderRepository orderRepository;
    private final OrderItemMapper orderItemMapper;
    private final IdMapper idMapper;
    private final ReentrantLock reentrantLock = new ReentrantLock(false);

    @Value("${app.kafka.events.order-confirmed}")
    private String orderConfirmedTopic;

    @Value("${app.kafka.events.order-paid}")
    private String orderPaidTopic;

    @Value("${app.kafka.events.order-cancelled}")
    private String orderCancelledTopic;




    @KafkaListener(topics = "${app.kafka.events.item-reserved}")
    public void handleItemReserveEvent(InventoryReservedEvent event) {
            UUID mappedOrderId = idMapper.mapToUUIDFromString(event.orderId());
            OrderEntity order = orderRepository.findById(mappedOrderId)
                .orElseThrow(()->new OrderNotFoundException(
                        MessageFormat.format("Order with id: {0} not found",mappedOrderId)
                ));
            order.setOrderStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            kafkaTemplate.send(orderConfirmedTopic,
                        mappedOrderId.toString(),
                        OrderConfirmedEvent.builder()
                                .orderId(order.getId().toString())
                                .buyerId(order.getBuyerId().toString())
                                .build());
    }

    @KafkaListener(topics = "${app.kafka.events.item-reserved-failed}")
    public void handleItemReserveFailedEvent(InventoryReservationFailedEvent event) {
            UUID mappedOrderId = idMapper.mapToUUIDFromString(event.orderId());
            OrderEntity order = orderRepository.findById(mappedOrderId)
                    .orElseThrow(()->new OrderNotFoundException(
                            MessageFormat.format("Order with id: {0} not found",mappedOrderId)
                    ));
            order.setOrderStatus(OrderStatus.CANCELED);
            orderRepository.save(order);

    }

    @KafkaListener(topics = "${app.kafka.events.payment-success}")
    public void handlePaymentSuccessEvent(PaymentSuccessEvent event) {
            UUID mappedOrderId = idMapper.mapToUUIDFromString(event.orderId());
            OrderEntity order = orderRepository.findById(mappedOrderId)
                .orElseThrow(()->new OrderNotFoundException(
                        MessageFormat.format("Order with id: {0} not found",mappedOrderId)
                ));
           order.setOrderStatus(OrderStatus.PAID);
           orderRepository.save(order);
           kafkaTemplate.send(orderPaidTopic, mappedOrderId.toString(),
                    OrderPaidEvent.builder()
                            .orderId(order.getId().toString())
                            .buyerId(order.getBuyerId().toString())
                            .build());

    }

    @KafkaListener(topics = "${app.kafka.events.payment-failed}")
    public void handlePaymentFailedEvent(PaymentFailedEvent event){
        UUID mappedOrderId = idMapper.mapToUUIDFromString(event.orderId());
        OrderEntity order = orderRepository.findById(mappedOrderId)
                .orElseThrow(()->new OrderNotFoundException(
                        MessageFormat.format("Order with id: {0} not found",mappedOrderId)
                        ));
        order.setOrderStatus(OrderStatus.CANCELED);
        orderRepository.save(order);
        kafkaTemplate.send(orderCancelledTopic,order.getId().toString(),
                OrderCancelledEvent.builder()
                        .orders(orderItemMapper.toDtoList(order.getOrderItemEntities()))
                        .orderId(order.getId().toString())
                        .reason(event.reason())
                        .build());
        }
    }

