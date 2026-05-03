package dn.notificationservice.service;

import dn.notificationservice.repository.NotificationRepository;
import dn.shared.event.order.OrderCreatedEvent;
import dn.shared.idempotency.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedEventListener {


    private final NotificationService notificationService;



    @KafkaListener(topics = {"${app.kafka.events.order-created}"})
    public void listenOrderCreatedTopic(OrderCreatedEvent orderCreatedEvent){
        notificationService.createOrderCreatedNotification(
                orderCreatedEvent.eventId(),
                orderCreatedEvent.buyerId(),
                orderCreatedEvent.recipientContact(),
                orderCreatedEvent.sellerId(),
                orderCreatedEvent.sellerContact());
    }
}
