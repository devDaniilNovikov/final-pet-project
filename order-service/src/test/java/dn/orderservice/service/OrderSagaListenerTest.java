package dn.orderservice.service;

import dn.orderservice.entity.OrderEntity;
import dn.orderservice.enums.OrderStatus;
import dn.orderservice.exception.OrderNotFoundException;
import dn.orderservice.mapper.OrderItemMapper;
import dn.orderservice.repository.OrderRepository;
import dn.shared.event.inventory.InventoryReservationFailedEvent;
import dn.shared.event.inventory.InventoryReservedEvent;
import dn.shared.event.order.OrderCancelledEvent;
import dn.shared.event.order.OrderConfirmedEvent;
import dn.shared.event.order.OrderItemDto;
import dn.shared.event.order.OrderPaidEvent;
import dn.shared.event.payment.PaymentFailedEvent;
import dn.shared.event.payment.PaymentSuccessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderSagaListenerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemMapper orderItemMapper;

    @InjectMocks
    private OrderSagaListener orderSagaListener;

    private static final String ORDER_CONFIRMED_TOPIC = "order-confirmed";
    private static final String ORDER_PAID_TOPIC = "order-paid";
    private static final String ORDER_CANCELLED_TOPIC = "order-cancelled";

    private UUID orderId;
    private OrderEntity order;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderSagaListener, "orderConfirmedTopic", ORDER_CONFIRMED_TOPIC);
        ReflectionTestUtils.setField(orderSagaListener, "orderPaidTopic", ORDER_PAID_TOPIC);
        ReflectionTestUtils.setField(orderSagaListener, "orderCancelledTopic", ORDER_CANCELLED_TOPIC);

        orderId = UUID.randomUUID();
        order = OrderEntity.builder()
                .id(orderId)
                .buyerId(UUID.randomUUID())
                .totalPrice(new BigDecimal("100.00"))
                .orderStatus(OrderStatus.PENDING)
                .orderItemEntities(List.of())
                .build();
    }

    // --- handleItemReserveEvent ---

    @Test
    void handleItemReserveEvent_setsStatusToConfirmedAndPublishesEvent() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderSagaListener.handleItemReserveEvent(new InventoryReservedEvent(orderId.toString()));

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);

        ArgumentCaptor<OrderConfirmedEvent> captor = ArgumentCaptor.forClass(OrderConfirmedEvent.class);
        verify(kafkaTemplate).send(eq(ORDER_CONFIRMED_TOPIC), eq(orderId.toString()), captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(orderId.toString());
        assertThat(captor.getValue().buyerId()).isEqualTo(order.getBuyerId().toString());
    }

    @Test
    void handleItemReserveEvent_orderNotFound_throwsOrderNotFoundException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderSagaListener.handleItemReserveEvent(new InventoryReservedEvent(orderId.toString()))
        ).isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(orderId.toString());

        verify(orderRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    // --- handleItemReserveFailedEvent ---

    @Test
    void handleItemReserveFailedEvent_setsStatusToCanceled() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderSagaListener.handleItemReserveFailedEvent(
                new InventoryReservationFailedEvent(orderId.toString(), "Out of stock")
        );

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
        verify(orderRepository).save(order);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void handleItemReserveFailedEvent_orderNotFound_throwsOrderNotFoundException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderSagaListener.handleItemReserveFailedEvent(
                        new InventoryReservationFailedEvent(orderId.toString(), "reason")
                )
        ).isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    // --- handlePaymentSuccessEvent ---

    @Test
    void handlePaymentSuccessEvent_setsStatusToPaidAndPublishesEvent() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderSagaListener.handlePaymentSuccessEvent(
                new PaymentSuccessEvent(orderId.toString(), order.getBuyerId().toString(), new BigDecimal("100.00"))
        );

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);

        ArgumentCaptor<OrderPaidEvent> captor = ArgumentCaptor.forClass(OrderPaidEvent.class);
        verify(kafkaTemplate).send(eq(ORDER_PAID_TOPIC), eq(orderId.toString()), captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(orderId.toString());
        assertThat(captor.getValue().buyerId()).isEqualTo(order.getBuyerId().toString());
    }

    @Test
    void handlePaymentSuccessEvent_orderNotFound_throwsOrderNotFoundException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderSagaListener.handlePaymentSuccessEvent(
                        new PaymentSuccessEvent(orderId.toString(), "buyer", BigDecimal.TEN)
                )
        ).isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    // --- handlePaymentFailedEvent ---

    @Test
    void handlePaymentFailedEvent_setsStatusToCanceledAndPublishesEvent() {
        List<OrderItemDto> itemDtos = List.of(
                new OrderItemDto(UUID.randomUUID(), "Product", 1, new BigDecimal("100.00"))
        );
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderItemMapper.toDtoList(order.getOrderItemEntities())).thenReturn(itemDtos);

        orderSagaListener.handlePaymentFailedEvent(
                new PaymentFailedEvent(orderId.toString(), "Insufficient funds")
        );

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
        verify(orderRepository).save(order);

        ArgumentCaptor<OrderCancelledEvent> captor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(kafkaTemplate).send(eq(ORDER_CANCELLED_TOPIC), eq(orderId.toString()), captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(orderId.toString());
        assertThat(captor.getValue().reason()).isEqualTo("Insufficient funds");
        assertThat(captor.getValue().orders()).isEqualTo(itemDtos);
    }

    @Test
    void handlePaymentFailedEvent_orderNotFound_throwsOrderNotFoundException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderSagaListener.handlePaymentFailedEvent(
                        new PaymentFailedEvent(orderId.toString(), "reason")
                )
        ).isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}