package dn.orderservice.service;

import dn.orderservice.dto.request.OrderItemRequest;
import dn.orderservice.dto.request.OrderRequest;
import dn.orderservice.dto.response.OrderResponse;
import dn.orderservice.dto.response.ProductResponse;
import dn.orderservice.entity.OrderEntity;
import dn.orderservice.entity.OrderItemEntity;
import dn.orderservice.entity.OutboxEntity;
import dn.orderservice.enums.OrderStatus;
import dn.orderservice.enums.OutboxStatus;
import dn.orderservice.mapper.OutboxMapper;
import dn.orderservice.repository.OrderRepository;
import dn.orderservice.repository.OutboxRepository;
import dn.orderservice.service.order.OrderPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPersistenceServiceTest {

    private static final String ORDER_CREATED_TOPIC = "order.created";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxMapper outboxMapper;

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private OrderPersistenceService orderPersistenceService;

    private static final UUID BUYER_ID   = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderPersistenceService, "orderCreatedEvent", ORDER_CREATED_TOPIC);
    }

    @Test
    void persistOrder_singleItem_calculatesTotalPriceCorrectly() {
        OrderRequest request = buildRequest(PRODUCT_ID, 3);
        ProductResponse product = new ProductResponse(PRODUCT_ID, new BigDecimal("20.00"), "Widget", 10);
        OutboxEntity outbox = outboxStub();
        when(outboxMapper.toOutboxEntity(any(), anyList(), any())).thenReturn(outbox);

        OrderResponse response = orderPersistenceService.persistOrder(request, Set.of(product));

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());
        // 3 * 20.00 = 60.00
        assertThat(captor.getValue().getTotalPrice()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void persistOrder_multipleItems_sumsPricesCorrectly() {
        UUID id2 = UUID.randomUUID();
        OrderRequest request = new OrderRequest(BUYER_ID, List.of(
                new OrderItemRequest(PRODUCT_ID, 2),
                new OrderItemRequest(id2, 5)
        ));
        ProductResponse p1 = new ProductResponse(PRODUCT_ID, new BigDecimal("10.00"), "A", 10);
        ProductResponse p2 = new ProductResponse(id2, new BigDecimal("4.00"), "B", 10);
        when(outboxMapper.toOutboxEntity(any(), anyList(), any())).thenReturn(outboxStub());

        orderPersistenceService.persistOrder(request, Set.of(p1, p2));

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());
        // 2*10 + 5*4 = 40
        assertThat(captor.getValue().getTotalPrice()).isEqualByComparingTo(new BigDecimal("40.00"));
    }

    @Test
    void persistOrder_setsOrderStatusToPending() {
        OrderRequest request = buildRequest(PRODUCT_ID, 1);
        ProductResponse product = new ProductResponse(PRODUCT_ID, BigDecimal.TEN, "Item", 5);
        when(outboxMapper.toOutboxEntity(any(), anyList(), any())).thenReturn(outboxStub());

        orderPersistenceService.persistOrder(request, Set.of(product));

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(captor.getValue().getBuyerId()).isEqualTo(BUYER_ID);
    }

    @Test
    void persistOrder_mapsOrderItemsWithCorrectFields() {
        OrderRequest request = buildRequest(PRODUCT_ID, 4);
        ProductResponse product = new ProductResponse(PRODUCT_ID, new BigDecimal("25.00"), "Gadget", 10);
        when(outboxMapper.toOutboxEntity(any(), anyList(), any())).thenReturn(outboxStub());

        orderPersistenceService.persistOrder(request, Set.of(product));

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());

        List<OrderItemEntity> items = captor.getValue().getOrderItemEntities();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(items.get(0).getQuantity()).isEqualTo(4);
        assertThat(items.get(0).getPriceAtPurchase()).isEqualByComparingTo(new BigDecimal("100.00")); // 4*25
        assertThat(items.get(0).getProductName()).isEqualTo("Gadget");
    }

    @Test
    void persistOrder_savesOutboxAfterOrder() {
        OrderRequest request = buildRequest(PRODUCT_ID, 1);
        ProductResponse product = new ProductResponse(PRODUCT_ID, BigDecimal.TEN, "Item", 5);
        OutboxEntity outbox = outboxStub();
        when(outboxMapper.toOutboxEntity(any(), anyList(), eq(ORDER_CREATED_TOPIC))).thenReturn(outbox);

        orderPersistenceService.persistOrder(request, Set.of(product));

        var inOrder = inOrder(orderRepository, outboxRepository);
        inOrder.verify(orderRepository).save(any(OrderEntity.class));
        inOrder.verify(outboxRepository).save(outbox);
    }

    @Test
    void persistOrder_passesCorrectTopicToOutboxMapper() {
        OrderRequest request = buildRequest(PRODUCT_ID, 1);
        ProductResponse product = new ProductResponse(PRODUCT_ID, BigDecimal.TEN, "Item", 5);
        when(outboxMapper.toOutboxEntity(any(), anyList(), any())).thenReturn(outboxStub());

        orderPersistenceService.persistOrder(request, Set.of(product));

        verify(outboxMapper).toOutboxEntity(any(OrderEntity.class), anyList(), eq(ORDER_CREATED_TOPIC));
    }

    // --- helpers ---

    private OrderRequest buildRequest(UUID productId, int quantity) {
        return new OrderRequest(BUYER_ID, List.of(new OrderItemRequest(productId, quantity)));
    }

    private OutboxEntity outboxStub() {
        return OutboxEntity.builder()
                .outboxStatus(OutboxStatus.PENDING)
                .topic(ORDER_CREATED_TOPIC)
                .build();
    }
}
