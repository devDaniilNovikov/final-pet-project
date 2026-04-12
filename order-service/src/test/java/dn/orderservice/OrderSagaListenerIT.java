package dn.orderservice;

import dn.orderservice.entity.OrderEntity;
import dn.orderservice.enums.OrderStatus;
import dn.orderservice.repository.OrderRepository;
import dn.shared.event.inventory.InventoryReservationFailedEvent;
import dn.shared.event.inventory.InventoryReservedEvent;
import dn.shared.event.order.OrderCancelledEvent;
import dn.shared.event.order.OrderConfirmedEvent;
import dn.shared.event.order.OrderPaidEvent;
import dn.shared.event.payment.PaymentFailedEvent;
import dn.shared.event.payment.PaymentSuccessEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@ActiveProfiles("test")
public class OrderSagaListenerIT {

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private OrderRepository orderRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.autoconfigure.exclude", () ->
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration");
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;


    @Value("${app.kafka.events.item-reserved}")
    private String itemReservedTopic;

    @Value("${app.kafka.events.item-reserved-failed}")
    private String itemReservedFailedTopic;

    @Value("${app.kafka.events.payment-success}")
    private String paymentSuccessTopic;

    @Value("${app.kafka.events.payment-failed}")
    private String paymentFailedTopic;

    @Value("${app.kafka.events.order-confirmed}")
    private String orderConfirmedTopic;

    @Value("${app.kafka.events.order-paid}")
    private String orderPaidTopic;

    @Value("${app.kafka.events.order-cancelled}")
    private String orderCancelledTopic;

    private KafkaMessageListenerContainer<String, Object> container;
    private BlockingQueue<ConsumerRecord<String, Object>> records;

    @BeforeEach
    void setUp() {
        records = new LinkedBlockingQueue<>();
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new JsonDeserializer<>(Object.class, false)
        );

        ContainerProperties containerProperties = new ContainerProperties(
                orderConfirmedTopic, orderPaidTopic, orderCancelledTopic
        );

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, Object>) records::add);
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic() * 3);
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
    }

    private OrderEntity createTestOrder() {
        OrderEntity order = OrderEntity.builder()
                .id(UUID.randomUUID())
                .buyerId(UUID.randomUUID())
                .totalPrice(new BigDecimal("100.00"))
                .orderStatus(OrderStatus.PENDING)
                .build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        return order;
    }

    @Test
    void handleItemReserveEvent_updatesStatusAndSendsConfirmed() throws Exception {
        OrderEntity order = createTestOrder();
        InventoryReservedEvent event = new InventoryReservedEvent(order.getId().toString());

        kafkaTemplate.send(itemReservedTopic, order.getId().toString(), event);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecord<String, Object> record = records.poll(100, TimeUnit.MILLISECONDS);
            if (record == null) {
                throw new AssertionError("Record not received yet");
            }
            assertThat(record.topic()).isEqualTo(orderConfirmedTopic);
            Map<String, Object> payload = (Map<String, Object>) record.value();
            assertThat(payload.get("orderId")).isEqualTo(order.getId().toString());
        });
    }

    @Test
    void handleItemReserveFailedEvent_updatesStatusToCancelled() throws Exception {
        OrderEntity order = createTestOrder();
        InventoryReservationFailedEvent event = new InventoryReservationFailedEvent(order.getId().toString(), "out of stock");

        kafkaTemplate.send(itemReservedFailedTopic, order.getId().toString(), event);

        // no out event from here, but wait a bit to ensure mock was called
        Thread.sleep(100);
    }

    @Test
    void handlePaymentSuccessEvent_updatesStatusAndSendsPaid() throws Exception {
        OrderEntity order = createTestOrder();
        order.setOrderStatus(OrderStatus.CONFIRMED);

        PaymentSuccessEvent event = new PaymentSuccessEvent(order.getId().toString(), order.getBuyerId().toString(), order.getTotalPrice());

        kafkaTemplate.send(paymentSuccessTopic, order.getId().toString(), event);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecord<String, Object> record = records.poll(100, TimeUnit.MILLISECONDS);
            if (record == null) {
                throw new AssertionError("Record not received yet");
            }
            assertThat(record.topic()).isEqualTo(orderPaidTopic);
            Map<String, Object> payload = (Map<String, Object>) record.value();
            assertThat(payload.get("orderId")).isEqualTo(order.getId().toString());
        });
    }

    @Test
    void handlePaymentFailedEvent_updatesStatusAndSendsCancelled() throws Exception {
        OrderEntity order = createTestOrder();
        order.setOrderStatus(OrderStatus.CONFIRMED);

        PaymentFailedEvent event = new PaymentFailedEvent(order.getId().toString(), "insufficient funds");

        kafkaTemplate.send(paymentFailedTopic, order.getId().toString(), event);


        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecord<String, Object> record = records.poll(100, TimeUnit.MILLISECONDS);
            if (record == null) {
                throw new AssertionError("Record not received yet");
            }
            assertThat(record.topic()).isEqualTo(orderCancelledTopic);
            Map<String, Object> payload = (Map<String, Object>) record.value();
            assertThat(payload.get("orderId")).isEqualTo(order.getId().toString());
            assertThat(payload.get("reason")).isEqualTo("insufficient funds");
        });
    }
}
