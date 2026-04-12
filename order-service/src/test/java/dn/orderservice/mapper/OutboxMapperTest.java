package dn.orderservice.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dn.orderservice.entity.OrderEntity;
import dn.orderservice.entity.OrderItemEntity;
import dn.orderservice.entity.OutboxEntity;
import dn.orderservice.enums.OutboxStatus;
import dn.shared.event.order.OrderCreatedEvent;
import dn.shared.event.order.OrderItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxMapperTest {

    @Mock
    private OrderItemMapper orderItemMapper;

    private OutboxMapper outboxMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final String TOPIC = "order.created";

    @BeforeEach
    void setUp() {
        outboxMapper = new OutboxMapper(objectMapper, orderItemMapper);
    }

    // --- toOutboxEntity ---

    @Test
    void toOutboxEntity_setsAggregateIdFromOrder() {
        OrderEntity order = buildOrder();
        when(orderItemMapper.toDtoList(anyList())).thenReturn(List.of());

        OutboxEntity result = outboxMapper.toOutboxEntity(order, List.of(), TOPIC);

        assertThat(result.getAggregateId()).isEqualTo(ORDER_ID);
    }

    @Test
    void toOutboxEntity_setsTopic() {
        OrderEntity order = buildOrder();
        when(orderItemMapper.toDtoList(anyList())).thenReturn(List.of());

        OutboxEntity result = outboxMapper.toOutboxEntity(order, List.of(), TOPIC);

        assertThat(result.getTopic()).isEqualTo(TOPIC);
    }

    @Test
    void toOutboxEntity_setsStatusToPending() {
        OrderEntity order = buildOrder();
        when(orderItemMapper.toDtoList(anyList())).thenReturn(List.of());

        OutboxEntity result = outboxMapper.toOutboxEntity(order, List.of(), TOPIC);

        assertThat(result.getOutboxStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void toOutboxEntity_payloadContainsOrderId() throws JsonProcessingException {
        OrderEntity order = buildOrder();
        when(orderItemMapper.toDtoList(anyList())).thenReturn(List.of());

        OutboxEntity result = outboxMapper.toOutboxEntity(order, List.of(), TOPIC);

        OrderCreatedEvent event = objectMapper.readValue(result.getPayload().toString(), OrderCreatedEvent.class);
        assertThat(event.orderId()).isEqualTo(ORDER_ID.toString());
    }

    @Test
    void toOutboxEntity_payloadContainsBuyerId() throws JsonProcessingException {
        OrderEntity order = buildOrder();
        when(orderItemMapper.toDtoList(anyList())).thenReturn(List.of());

        OutboxEntity result = outboxMapper.toOutboxEntity(order, List.of(), TOPIC);

        OrderCreatedEvent event = objectMapper.readValue(result.getPayload().toString(), OrderCreatedEvent.class);
        assertThat(event.buyerId()).isEqualTo(BUYER_ID.toString());
    }

    @Test
    void toOutboxEntity_payloadContainsTotalPrice() throws JsonProcessingException {
        OrderEntity order = buildOrder();
        when(orderItemMapper.toDtoList(anyList())).thenReturn(List.of());

        OutboxEntity result = outboxMapper.toOutboxEntity(order, List.of(), TOPIC);

        OrderCreatedEvent event = objectMapper.readValue(result.getPayload().toString(), OrderCreatedEvent.class);
        assertThat(event.totalPrice()).isEqualByComparingTo(new BigDecimal("250.00"));
    }

    @Test
    void toOutboxEntity_payloadContainsOrderItems() throws JsonProcessingException {
        OrderEntity order = buildOrder();
        OrderItemEntity itemEntity = OrderItemEntity.builder()
                .productId(PRODUCT_ID)
                .productName("Widget")
                .quantity(5)
                .priceAtPurchase(new BigDecimal("50.00"))
                .build();
        OrderItemDto itemDto = new OrderItemDto(PRODUCT_ID, "Widget", 5, new BigDecimal("50.00"));
        when(orderItemMapper.toDtoList(List.of(itemEntity))).thenReturn(List.of(itemDto));

        OutboxEntity result = outboxMapper.toOutboxEntity(order, List.of(itemEntity), TOPIC);

        OrderCreatedEvent event = objectMapper.readValue(result.getPayload().toString(), OrderCreatedEvent.class);
        assertThat(event.orderItems()).hasSize(1);
        assertThat(event.orderItems().get(0).getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(event.orderItems().get(0).getQuantity()).isEqualTo(5);
    }

    // --- toOrderCreatedEvent ---

    @Test
    void toOrderCreatedEvent_deserializesPayloadCorrectly() throws JsonProcessingException {
        OrderCreatedEvent original = OrderCreatedEvent.builder()
                .orderId(ORDER_ID.toString())
                .buyerId(BUYER_ID.toString())
                .totalPrice(new BigDecimal("100.00"))
                .orderItems(List.of())
                .build();

        OutboxEntity outbox = OutboxEntity.builder()
                .id(UUID.randomUUID())
                .payload(objectMapper.writeValueAsString(original))
                .build();

        OrderCreatedEvent result = outboxMapper.toOrderCreatedEvent(outbox);

        assertThat(result.orderId()).isEqualTo(ORDER_ID.toString());
        assertThat(result.buyerId()).isEqualTo(BUYER_ID.toString());
        assertThat(result.totalPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.orderItems()).isEmpty();
    }

    @Test
    void toOrderCreatedEvent_invalidPayload_throwsIllegalStateException() {
        OutboxEntity outbox = OutboxEntity.builder()
                .id(UUID.randomUUID())
                .payload("not-valid-json{{{")
                .build();

        assertThatThrownBy(() -> outboxMapper.toOrderCreatedEvent(outbox))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to deserialize outbox");
    }

    @Test
    void toOrderCreatedEvent_preservesOrderItems() throws JsonProcessingException {
        OrderItemDto item = new OrderItemDto(PRODUCT_ID, "Gadget", 2, new BigDecimal("30.00"));
        OrderCreatedEvent original = OrderCreatedEvent.builder()
                .orderId(ORDER_ID.toString())
                .buyerId(BUYER_ID.toString())
                .totalPrice(new BigDecimal("60.00"))
                .orderItems(List.of(item))
                .build();

        OutboxEntity outbox = OutboxEntity.builder()
                .id(UUID.randomUUID())
                .payload(objectMapper.writeValueAsString(original))
                .build();

        OrderCreatedEvent result = outboxMapper.toOrderCreatedEvent(outbox);

        assertThat(result.orderItems()).hasSize(1);
        assertThat(result.orderItems().get(0).getProductName()).isEqualTo("Gadget");
        assertThat(result.orderItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(result.orderItems().get(0).getPrice()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    // --- helpers ---

    private OrderEntity buildOrder() {
        return OrderEntity.builder()
                .id(ORDER_ID)
                .buyerId(BUYER_ID)
                .totalPrice(new BigDecimal("250.00"))
                .build();
    }
}

