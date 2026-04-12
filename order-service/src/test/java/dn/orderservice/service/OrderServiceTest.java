package dn.orderservice.service;

import dn.orderservice.client.ProductClient;
import dn.orderservice.dto.request.OrderItemRequest;
import dn.orderservice.dto.request.OrderRequest;
import dn.orderservice.dto.response.OrderResponse;
import dn.orderservice.dto.response.ProductResponse;
import dn.orderservice.entity.OrderEntity;
import dn.orderservice.entity.OrderItemEntity;
import dn.orderservice.entity.OutboxEntity;
import dn.orderservice.enums.OrderStatus;
import dn.orderservice.enums.OutboxStatus;
import dn.orderservice.mapper.OrderItemMapper;
import dn.orderservice.mapper.OutboxMapper;
import dn.orderservice.repository.OrderRepository;
import dn.orderservice.repository.OutboxRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OutboxMapper outboxMapper;

    @InjectMocks
    private OrderService orderService;

    private static final String ORDER_CREATED_TOPIC = "order.created";
    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID_1 = UUID.randomUUID();
    private static final UUID PRODUCT_ID_2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "orderCreatedEvent", ORDER_CREATED_TOPIC);
    }

    // --- createOrder: happy path ---

    @Test
    void createOrder_singleItem_savesOrderAndOutbox() {
        OrderItemRequest itemRequest = new OrderItemRequest(PRODUCT_ID_1, 2);
        OrderRequest request = new OrderRequest(BUYER_ID, List.of(itemRequest));

        ProductResponse product = new ProductResponse(PRODUCT_ID_1, new BigDecimal("50.00"), "Widget");
        when(productClient.batch(List.of(PRODUCT_ID_1))).thenReturn(List.of(product));

        OutboxEntity outbox = OutboxEntity.builder()
                .outboxStatus(OutboxStatus.PENDING)
                .topic(ORDER_CREATED_TOPIC)
                .build();
        when(outboxMapper.toOutboxEntity(any(OrderEntity.class), anyList(), eq(ORDER_CREATED_TOPIC)))
                .thenReturn(outbox);

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).save(any(OrderEntity.class));
        verify(outboxRepository).save(outbox);
    }

    @Test
    void createOrder_multipleItems_calculatesTotalPriceCorrectly() {
        OrderItemRequest item1 = new OrderItemRequest(PRODUCT_ID_1, 2);
        OrderItemRequest item2 = new OrderItemRequest(PRODUCT_ID_2, 3);
        OrderRequest request = new OrderRequest(BUYER_ID, List.of(item1, item2));

        ProductResponse product1 = new ProductResponse(PRODUCT_ID_1, new BigDecimal("10.00"), "Product A");
        ProductResponse product2 = new ProductResponse(PRODUCT_ID_2, new BigDecimal("20.00"), "Product B");
        when(productClient.batch(any())).thenReturn(List.of(product1, product2));
        when(outboxMapper.toOutboxEntity(any(), anyList(), any()))
                .thenReturn(OutboxEntity.builder().outboxStatus(OutboxStatus.PENDING).build());

        orderService.createOrder(request);

        // Expected total: 2*10 + 3*20 = 80
        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getTotalPrice()).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    void createOrder_setsOrderStatusToPending() {
        OrderItemRequest itemRequest = new OrderItemRequest(PRODUCT_ID_1, 1);
        OrderRequest request = new OrderRequest(BUYER_ID, List.of(itemRequest));

        ProductResponse product = new ProductResponse(PRODUCT_ID_1, new BigDecimal("100.00"), "Item");
        when(productClient.batch(any())).thenReturn(List.of(product));
        when(outboxMapper.toOutboxEntity(any(), anyList(), any()))
                .thenReturn(OutboxEntity.builder().outboxStatus(OutboxStatus.PENDING).build());

        orderService.createOrder(request);

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(orderCaptor.getValue().getBuyerId()).isEqualTo(BUYER_ID);
    }

    @Test
    void createOrder_setsCorrectBuyerId() {
        UUID buyerId = UUID.randomUUID();
        OrderItemRequest itemRequest = new OrderItemRequest(PRODUCT_ID_1, 1);
        OrderRequest request = new OrderRequest(buyerId, List.of(itemRequest));

        ProductResponse product = new ProductResponse(PRODUCT_ID_1, new BigDecimal("5.00"), "Cheap Item");
        when(productClient.batch(any())).thenReturn(List.of(product));
        when(outboxMapper.toOutboxEntity(any(), anyList(), any()))
                .thenReturn(OutboxEntity.builder().outboxStatus(OutboxStatus.PENDING).build());

        orderService.createOrder(request);

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getBuyerId()).isEqualTo(buyerId);
    }

    // --- createOrder: outbox integration ---

    @Test
    void createOrder_passesCorrectTopicToOutboxMapper() {
        OrderItemRequest itemRequest = new OrderItemRequest(PRODUCT_ID_1, 1);
        OrderRequest request = new OrderRequest(BUYER_ID, List.of(itemRequest));

        ProductResponse product = new ProductResponse(PRODUCT_ID_1, new BigDecimal("10.00"), "Item");
        when(productClient.batch(any())).thenReturn(List.of(product));
        when(outboxMapper.toOutboxEntity(any(), anyList(), any()))
                .thenReturn(OutboxEntity.builder().outboxStatus(OutboxStatus.PENDING).build());

        orderService.createOrder(request);

        verify(outboxMapper).toOutboxEntity(any(OrderEntity.class), anyList(), eq(ORDER_CREATED_TOPIC));
    }

    @Test
    void createOrder_savesOutboxAfterOrder() {
        OrderItemRequest itemRequest = new OrderItemRequest(PRODUCT_ID_1, 1);
        OrderRequest request = new OrderRequest(BUYER_ID, List.of(itemRequest));

        ProductResponse product = new ProductResponse(PRODUCT_ID_1, new BigDecimal("25.00"), "Item");
        when(productClient.batch(any())).thenReturn(List.of(product));

        OutboxEntity outboxEntity = OutboxEntity.builder()
                .topic(ORDER_CREATED_TOPIC)
                .outboxStatus(OutboxStatus.PENDING)
                .payload("{}")
                .build();
        when(outboxMapper.toOutboxEntity(any(), anyList(), any())).thenReturn(outboxEntity);

        orderService.createOrder(request);

        var inOrder = inOrder(orderRepository, outboxRepository);
        inOrder.verify(orderRepository).save(any(OrderEntity.class));
        inOrder.verify(outboxRepository).save(outboxEntity);
    }

    // --- createOrder: order items mapping ---

    @Test
    void createOrder_mapsOrderItemsCorrectly() {
        OrderItemRequest itemRequest = new OrderItemRequest(PRODUCT_ID_1, 3);
        OrderRequest request = new OrderRequest(BUYER_ID, List.of(itemRequest));

        ProductResponse product = new ProductResponse(PRODUCT_ID_1, new BigDecimal("15.00"), "Triple Item");
        when(productClient.batch(any())).thenReturn(List.of(product));
        when(outboxMapper.toOutboxEntity(any(), anyList(), any()))
                .thenReturn(OutboxEntity.builder().outboxStatus(OutboxStatus.PENDING).build());

        orderService.createOrder(request);

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(orderCaptor.capture());

        List<OrderItemEntity> items = orderCaptor.getValue().getOrderItemEntities();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getProductId()).isEqualTo(PRODUCT_ID_1);
        assertThat(items.get(0).getQuantity()).isEqualTo(3);
        assertThat(items.get(0).getPriceAtPurchase()).isEqualByComparingTo(new BigDecimal("45.00"));
        assertThat(items.get(0).getProductName()).isEqualTo("Triple Item");
    }

    @Test
    void createOrder_multipleItems_mapsAllItemsWithCorrectPrices() {
        OrderItemRequest item1 = new OrderItemRequest(PRODUCT_ID_1, 1);
        OrderItemRequest item2 = new OrderItemRequest(PRODUCT_ID_2, 5);
        OrderRequest request = new OrderRequest(BUYER_ID, List.of(item1, item2));

        ProductResponse product1 = new ProductResponse(PRODUCT_ID_1, new BigDecimal("100.00"), "Expensive");
        ProductResponse product2 = new ProductResponse(PRODUCT_ID_2, new BigDecimal("3.00"), "Cheap");
        when(productClient.batch(any())).thenReturn(List.of(product1, product2));
        when(outboxMapper.toOutboxEntity(any(), anyList(), any()))
                .thenReturn(OutboxEntity.builder().outboxStatus(OutboxStatus.PENDING).build());

        orderService.createOrder(request);

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());

        List<OrderItemEntity> items = captor.getValue().getOrderItemEntities();
        assertThat(items).hasSize(2);

        OrderItemEntity itemEntity1 = items.stream()
                .filter(i -> i.getProductId().equals(PRODUCT_ID_1)).findFirst().orElseThrow();
        assertThat(itemEntity1.getPriceAtPurchase()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(itemEntity1.getQuantity()).isEqualTo(1);

        OrderItemEntity itemEntity2 = items.stream()
                .filter(i -> i.getProductId().equals(PRODUCT_ID_2)).findFirst().orElseThrow();
        assertThat(itemEntity2.getPriceAtPurchase()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(itemEntity2.getQuantity()).isEqualTo(5);
    }
}