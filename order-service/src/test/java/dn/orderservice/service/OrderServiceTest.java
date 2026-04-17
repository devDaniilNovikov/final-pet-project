package dn.orderservice.service;

import dn.orderservice.client.ProductClient;
import dn.orderservice.dto.request.OrderItemRequest;
import dn.orderservice.dto.request.OrderRequest;
import dn.orderservice.dto.response.OrderResponse;
import dn.orderservice.dto.response.ProductResponse;
import dn.orderservice.enums.OrderStatus;
import dn.orderservice.exception.ProductNotFoundException;
import dn.orderservice.mapper.OrderMapper;
import dn.orderservice.repository.OrderRepository;
import dn.orderservice.service.order.OrderPersistenceService;
import dn.orderservice.service.order.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderPersistenceService orderPersistenceService;

    @Mock
    private ProductClient productClient;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private static final UUID BUYER_ID    = UUID.randomUUID();
    private static final UUID PRODUCT_ID  = UUID.randomUUID();

    private OrderItemRequest itemRequest;
    private OrderRequest orderRequest;
    private ProductResponse product;

    @BeforeEach
    void setUp() {
        itemRequest  = new OrderItemRequest(PRODUCT_ID, 2);
        orderRequest = new OrderRequest(BUYER_ID, java.util.List.of(itemRequest));
        product      = new ProductResponse(PRODUCT_ID, new BigDecimal("50.00"), "Widget", 10);
    }

    @Test
    void createOrder_happyPath_delegatesToPersistenceService() {
        when(productClient.batch(Set.of(PRODUCT_ID))).thenReturn(Set.of(product));
        OrderResponse expected = new OrderResponse(UUID.randomUUID(), OrderStatus.PENDING);
        when(orderPersistenceService.persistOrder(any(), anySet())).thenReturn(expected);

        OrderResponse result = orderService.createOrder(orderRequest);

        assertThat(result).isEqualTo(expected);
        verify(orderPersistenceService).persistOrder(eq(orderRequest), eq(Set.of(product)));
    }

    @Test
    void createOrder_productNotFound_throwsProductNotFoundException() {
        UUID missingId = UUID.randomUUID();
        OrderRequest requestWithMissing = new OrderRequest(BUYER_ID,
                java.util.List.of(new OrderItemRequest(missingId, 1)));

        // productClient returns empty — none of the requested products found
        when(productClient.batch(Set.of(missingId))).thenReturn(Set.of());

        assertThatThrownBy(() -> orderService.createOrder(requestWithMissing))
                .isInstanceOf(ResponseStatusException.class);

        verifyNoInteractions(orderPersistenceService);
    }

    @Test
    void createOrder_partialProductsReturned_throwsProductNotFoundException() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        OrderRequest request = new OrderRequest(BUYER_ID, java.util.List.of(
                new OrderItemRequest(id1, 1),
                new OrderItemRequest(id2, 1)
        ));

        // only one product returned, one is missing
        ProductResponse p1 = new ProductResponse(id1, BigDecimal.TEN, "P1", 5);
        when(productClient.batch(Set.of(id1, id2))).thenReturn(Set.of(p1));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ProductNotFoundException.class);

        verifyNoInteractions(orderPersistenceService);
    }
}
