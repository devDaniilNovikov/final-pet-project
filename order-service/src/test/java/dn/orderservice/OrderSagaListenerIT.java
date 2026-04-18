package dn.orderservice;

import dn.orderservice.entity.OrderEntity;
import dn.orderservice.enums.OrderStatus;
import dn.orderservice.repository.OrderRepository;
import dn.shared.event.inventory.InventoryReservationFailedEvent;
import dn.shared.event.inventory.InventoryReservedEvent;
import dn.shared.event.payment.PaymentFailedEvent;
import dn.shared.event.payment.PaymentSuccessEvent;
import dn.shared.idempotency.ProcessedEventRepository;
import dn.shared.outbox.OutboxEntity;
import dn.shared.outbox.OutboxRelay;
import dn.shared.outbox.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class OrderSagaListenerIT extends AbstractOrderIT {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    // Prevent the relay scheduler from processing outbox entries during assertions
    @MockitoBean
    private OutboxRelay outboxRelay;

    @Value("${app.kafka.events.item-reserved}")
    private String itemReservedTopic;

    @Value("${app.kafka.events.item-reserved-failed}")
    private String itemReservedFailedTopic;

    @Value("${app.kafka.events.payment-success}")
    private String paymentSuccessTopic;

    @Value("${app.kafka.events.payment-failed}")
    private String paymentFailedTopic;

    private OrderEntity savedOrder;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        processedEventRepository.deleteAll();
        orderRepository.deleteAll();

        savedOrder = orderRepository.save(OrderEntity.builder()
                .id(UUID.randomUUID())
                .buyerId(UUID.randomUUID())
                .totalPrice(new BigDecimal("250.00"))
                .orderStatus(OrderStatus.PENDING)
                .orderItemEntities(List.of())
                .build());

        eventId = UUID.randomUUID();
    }

    // --- handleItemReserveEvent ---

    @Test
    void handleItemReserveEvent_setsOrderStatusToConfirmed() {
        kafkaTemplate.send(itemReservedTopic, savedOrder.getId().toString(),
                new InventoryReservedEvent(eventId, savedOrder.getId()));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            OrderEntity updated = orderRepository.findById(savedOrder.getId()).orElseThrow();
            assertThat(updated.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        });
    }

    @Test
    void handleItemReserveEvent_createsOutboxEntry() {
        kafkaTemplate.send(itemReservedTopic, savedOrder.getId().toString(),
                new InventoryReservedEvent(eventId, savedOrder.getId()));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<OutboxEntity> outboxEntries = outboxRepository.findAll();
            assertThat(outboxEntries).hasSize(1);
            assertThat(outboxEntries.get(0).getAggregateId()).isEqualTo(savedOrder.getId());
        });
    }

    @Test
    void handleItemReserveEvent_duplicateEvent_processedOnlyOnce() {
        InventoryReservedEvent event = new InventoryReservedEvent(eventId, savedOrder.getId());

        kafkaTemplate.send(itemReservedTopic, savedOrder.getId().toString(), event);
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(orderRepository.findById(savedOrder.getId()).orElseThrow().getOrderStatus())
                        .isEqualTo(OrderStatus.CONFIRMED)
        );

        // Send same event again
        kafkaTemplate.send(itemReservedTopic, savedOrder.getId().toString(), event);

        // Wait a bit and verify outbox has only 1 entry (not 2)
        await().during(1, TimeUnit.SECONDS).atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(outboxRepository.findAll()).hasSize(1);
            assertThat(processedEventRepository.existsById(eventId)).isTrue();
        });
    }

    @Test
    void handleItemReserveEvent_savesProcessedEventForIdempotency() {
        kafkaTemplate.send(itemReservedTopic, savedOrder.getId().toString(),
                new InventoryReservedEvent(eventId, savedOrder.getId()));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(processedEventRepository.existsById(eventId)).isTrue()
        );
    }

    // --- handleItemReserveFailedEvent ---

    @Test
    void handleItemReserveFailedEvent_setsOrderStatusToCanceled() {
        kafkaTemplate.send(itemReservedFailedTopic, savedOrder.getId().toString(),
                new InventoryReservationFailedEvent(eventId, savedOrder.getId(), "Out of stock"));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            OrderEntity updated = orderRepository.findById(savedOrder.getId()).orElseThrow();
            assertThat(updated.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
        });
    }

    @Test
    void handleItemReserveFailedEvent_createsOutboxEntry() {
        kafkaTemplate.send(itemReservedFailedTopic, savedOrder.getId().toString(),
                new InventoryReservationFailedEvent(eventId, savedOrder.getId(), "Out of stock"));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(outboxRepository.findAll()).hasSize(1)
        );
    }

    @Test
    void handleItemReserveFailedEvent_duplicateEvent_processedOnlyOnce() {
        InventoryReservationFailedEvent event =
                new InventoryReservationFailedEvent(eventId, savedOrder.getId(), "Sold out");

        kafkaTemplate.send(itemReservedFailedTopic, savedOrder.getId().toString(), event);
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(orderRepository.findById(savedOrder.getId()).orElseThrow().getOrderStatus())
                        .isEqualTo(OrderStatus.CANCELED)
        );

        kafkaTemplate.send(itemReservedFailedTopic, savedOrder.getId().toString(), event);

        await().during(1, TimeUnit.SECONDS).atMost(3, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(outboxRepository.findAll()).hasSize(1)
        );
    }

    // --- handlePaymentSuccessEvent ---

    @Test
    void handlePaymentSuccessEvent_setsOrderStatusToPaid() {
        kafkaTemplate.send(paymentSuccessTopic, savedOrder.getId().toString(),
                new PaymentSuccessEvent(eventId, savedOrder.getId(), savedOrder.getBuyerId(),
                        new BigDecimal("250.00")));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            OrderEntity updated = orderRepository.findById(savedOrder.getId()).orElseThrow();
            assertThat(updated.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        });
    }

    @Test
    void handlePaymentSuccessEvent_createsOutboxEntry() {
        kafkaTemplate.send(paymentSuccessTopic, savedOrder.getId().toString(),
                new PaymentSuccessEvent(eventId, savedOrder.getId(), savedOrder.getBuyerId(),
                        new BigDecimal("250.00")));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(outboxRepository.findAll()).hasSize(1)
        );
    }

    @Test
    void handlePaymentSuccessEvent_duplicateEvent_processedOnlyOnce() {
        PaymentSuccessEvent event = new PaymentSuccessEvent(eventId, savedOrder.getId(),
                savedOrder.getBuyerId(), new BigDecimal("250.00"));

        kafkaTemplate.send(paymentSuccessTopic, savedOrder.getId().toString(), event);
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(orderRepository.findById(savedOrder.getId()).orElseThrow().getOrderStatus())
                        .isEqualTo(OrderStatus.PAID)
        );

        kafkaTemplate.send(paymentSuccessTopic, savedOrder.getId().toString(), event);

        await().during(1, TimeUnit.SECONDS).atMost(3, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(outboxRepository.findAll()).hasSize(1)
        );
    }

    // --- handlePaymentFailedEvent ---

    @Test
    void handlePaymentFailedEvent_setsOrderStatusToCanceled() {
        kafkaTemplate.send(paymentFailedTopic, savedOrder.getId().toString(),
                new PaymentFailedEvent(eventId, savedOrder.getId(), "Insufficient funds"));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            OrderEntity updated = orderRepository.findById(savedOrder.getId()).orElseThrow();
            assertThat(updated.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
        });
    }

    @Test
    void handlePaymentFailedEvent_createsOutboxEntry() {
        kafkaTemplate.send(paymentFailedTopic, savedOrder.getId().toString(),
                new PaymentFailedEvent(eventId, savedOrder.getId(), "Insufficient funds"));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(outboxRepository.findAll()).hasSize(1)
        );
    }

    @Test
    void handlePaymentFailedEvent_duplicateEvent_processedOnlyOnce() {
        PaymentFailedEvent event = new PaymentFailedEvent(eventId, savedOrder.getId(), "Card declined");

        kafkaTemplate.send(paymentFailedTopic, savedOrder.getId().toString(), event);
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(orderRepository.findById(savedOrder.getId()).orElseThrow().getOrderStatus())
                        .isEqualTo(OrderStatus.CANCELED)
        );

        kafkaTemplate.send(paymentFailedTopic, savedOrder.getId().toString(), event);

        await().during(1, TimeUnit.SECONDS).atMost(3, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(outboxRepository.findAll()).hasSize(1)
        );
    }
}
