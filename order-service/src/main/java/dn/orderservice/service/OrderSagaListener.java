package dn.orderservice.service;
import dn.orderservice.entity.OrderEntity;
import dn.orderservice.enums.OrderStatus;
import dn.orderservice.exception.OrderNotFoundException;
import dn.orderservice.repository.OrderRepository;
import dn.orderservice.utils.IdMapper;
import dn.shared.event.inventory.InventoryReservationFailedEvent;
import dn.shared.event.order.OrderCancelledEvent;
import dn.shared.event.order.OrderConfirmedEvent;
import dn.shared.event.order.OrderPaidEvent;
import dn.shared.event.payment.PaymentFailedEvent;
import dn.shared.event.payment.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import dn.shared.event.inventory.InventoryReservedEvent;
import org.springframework.transaction.annotation.Transactional;
import java.text.MessageFormat;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderRepository orderRepository;

    @Value("${app.kafka.events.order-confirmed}")
    private String orderConfirmedTopic;

    @Value("${app.kafka.events.order-paid}")
    private String orderPaidTopic;

    @Value("${app.kafka.events.order-cancelled}")
    private String orderCancelledTopic;


    @KafkaListener(topics = "${app.kafka.events.item-reserved}")
    @Transactional
    public void handleItemReserveEvent(InventoryReservedEvent event) {
            UUID mappedOrderId = IdMapper.mapToUUIDFromString(event.orderId());
            OrderEntity order = orderRepository.findById(mappedOrderId)
                .orElseThrow(()->new OrderNotFoundException(
                        MessageFormat.format("Order with id: {0} not found",mappedOrderId)
                ));
            order.setOrderStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            sendEventToKafka(orderConfirmedTopic,event.orderId(), OrderConfirmedEvent.builder()
                    .orderId(event.orderId())
                    .buyerId(order.getBuyerId().toString())
                    .build());
    }



    private void log(String topic,
                     Exception e){
        log.error("Failed sent message to topic={}, cause={0}",topic,e );
    }

    private void sendEventToKafka(String topic,
                                  String key,
                                  Object payload){
        try {
            kafkaTemplate.send(topic, key, payload).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log(topic,e);
            throw new RuntimeException();
        } catch (ExecutionException e) {
            log(topic,e);
            throw new RuntimeException();
        }
    }

    @KafkaListener(topics = "${app.kafka.events.item-reserved-failed}")
    @Transactional
    public void handleItemReserveFailedEvent(InventoryReservationFailedEvent event) {
            UUID mappedOrderId = IdMapper.mapToUUIDFromString(event.orderId());
            OrderEntity order = orderRepository.findById(mappedOrderId)
                    .orElseThrow(()->new OrderNotFoundException(
                            MessageFormat.format("Order with id: {0} not found",mappedOrderId)
                    ));
            order.setOrderStatus(OrderStatus.CANCELED);
            orderRepository.save(order);
            sendEventToKafka(orderCancelledTopic,event.orderId(),
                    OrderCancelledEvent.builder()
                            .orderId(event.orderId())
                            .reason(event.reason())
                            .build());

    }

    @KafkaListener(topics = "${app.kafka.events.payment-success}")
    @Transactional
    public void handlePaymentSuccessEvent(PaymentSuccessEvent event) {
            UUID mappedOrderId = IdMapper.mapToUUIDFromString(event.orderId());
            OrderEntity order = orderRepository.findById(mappedOrderId)
                .orElseThrow(()->new OrderNotFoundException(
                        MessageFormat.format("Order with id: {0} not found",mappedOrderId)
                ));
           order.setOrderStatus(OrderStatus.PAID);
           orderRepository.save(order);
           sendEventToKafka(orderPaidTopic,event.orderId(), OrderPaidEvent.builder()
                   .orderId(event.orderId())
                   .buyerId(event.buyerId())
                   .amount(event.amount())
                   .build());

    }

    @KafkaListener(topics = "${app.kafka.events.payment-failed}")
    @Transactional
    public void handlePaymentFailedEvent(PaymentFailedEvent event){
        UUID mappedOrderId = IdMapper.mapToUUIDFromString(event.orderId());
        OrderEntity order = orderRepository.findById(mappedOrderId)
                .orElseThrow(()->new OrderNotFoundException(
                        MessageFormat.format("Order with id={0} not found",mappedOrderId)
                        ));
        order.setOrderStatus(OrderStatus.CANCELED);
        orderRepository.save(order);
        sendEventToKafka(orderCancelledTopic,event.orderId(), OrderCancelledEvent.builder()
                .orderId(event.orderId())
                .reason(event.reason())
                .build());
        }
    }

