package dn.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dn.orderservice.AbstractOrderIT;
import dn.orderservice.client.ProductClient;
import dn.orderservice.dto.request.OrderItemRequest;
import dn.orderservice.dto.request.OrderRequest;
import dn.orderservice.dto.response.ProductResponse;
import dn.orderservice.entity.OrderEntity;
import dn.orderservice.enums.OrderStatus;
import dn.orderservice.repository.OrderRepository;
import dn.orderservice.service.outbox.OutboxRelay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

class OrderControllerIT extends AbstractOrderIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private ProductClient productClient;

    // Prevent the outbox scheduler from interfering with assertions
    @MockitoBean
    private OutboxRelay outboxRelay;

    @Autowired
    private ObjectMapper objectMapper;

    private static final UUID BUYER_ID   = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    // --- POST /api/v1/orders ---

    @Test
    void createOrder_validRequest_returns201WithPendingStatus() {
        ProductResponse product = new ProductResponse(PRODUCT_ID, new BigDecimal("150.00"), "Widget", 10);
        when(productClient.batch(anySet())).thenReturn(Set.of(product));

        OrderRequest request = new OrderRequest(BUYER_ID, List.of(new OrderItemRequest(PRODUCT_ID, 2)));

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/orders", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("orderId");
        assertThat(response.getBody().get("orderStatus")).isEqualTo("PENDING");
    }

    @Test
    void createOrder_productNotFound_returns400() {
        when(productClient.batch(anySet())).thenReturn(Set.of());

        OrderRequest request = new OrderRequest(BUYER_ID, List.of(new OrderItemRequest(PRODUCT_ID, 1)));

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/orders", request, Map.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND);
    }

    @Test
    void createOrder_nullBuyerId_returns400() {
        OrderRequest request = new OrderRequest(null, List.of(new OrderItemRequest(PRODUCT_ID, 1)));

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/orders", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createOrder_emptyItems_returns400() {
        OrderRequest request = new OrderRequest(BUYER_ID, List.of());

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/orders", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createOrder_itemQuantityZero_returns400() {
        OrderRequest request = new OrderRequest(BUYER_ID,
                List.of(new OrderItemRequest(PRODUCT_ID, 0)));

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/orders", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createOrder_persistsOrderToDatabase() {
        ProductResponse product = new ProductResponse(PRODUCT_ID, new BigDecimal("50.00"), "Gadget", 5);
        when(productClient.batch(anySet())).thenReturn(Set.of(product));

        OrderRequest request = new OrderRequest(BUYER_ID, List.of(new OrderItemRequest(PRODUCT_ID, 3)));

        restTemplate.postForEntity("/api/v1/orders", request, Map.class);

        List<OrderEntity> orders = orderRepository.findAllByBuyerId(BUYER_ID);
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getTotalPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(orders.get(0).getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    // --- GET /api/v1/orders/{orderId} ---

    @Test
    void getOrderById_existingOrder_returns200() {
        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .id(UUID.randomUUID())
                .buyerId(BUYER_ID)
                .totalPrice(new BigDecimal("99.99"))
                .orderStatus(OrderStatus.PENDING)
                .orderItemEntities(List.of())
                .build());

        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/orders/{id}", Map.class, order.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("orderId")).isEqualTo(order.getId().toString());
        assertThat(response.getBody().get("orderStatus")).isEqualTo("PENDING");
    }

    @Test
    void getOrderById_unknownId_returns404() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/orders/{id}", Map.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
