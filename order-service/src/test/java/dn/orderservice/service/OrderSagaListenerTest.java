package dn.orderservice.service;

import dn.orderservice.entity.OrderEntity;
import dn.orderservice.enums.OrderStatus;
import dn.orderservice.exception.OrderNotFoundException;
import dn.orderservice.repository.OrderRepository;
import dn.orderservice.service.order.OrderSagaListener;
import dn.orderservice.service.outbox.OutboxService;
import dn.shared.event.inventory.InventoryReservationFailedEvent;
import dn.shared.event.inventory.InventoryReservedEvent;
import dn.shared.event.payment.PaymentFailedEvent;
import dn.shared.event.payment.PaymentSuccessEvent;
import dn.shared.idempotency.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderSagaListenerTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private OrderSagaListener orderSagaListener;

    private UUID eventId;
    private UUID orderId;
    private OrderEntity order;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
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
    void handleItemReserveEvent_setsStatusToConfirmedAndCreatesOutbox() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(orderRepository.findByIdWithLock(orderId)).thenReturn(Optional.of(order));

        InventoryReservedEvent event = new InventoryReservedEvent(eventId, orderId);
        orderSagaListener.handleItemReserveEvent(event);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);
        verify(processedEventRepository).save(any());
        verify(outboxService).createOutbox(order, event);
    }

    @Test
    void handleItemReserveEvent_duplicate_skipsProcessing() {
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        orderSagaListener.handleItemReserveEvent(new InventoryReservedEvent(eventId, orderId));

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(outboxService);
    }

    @Test
    void handleItemReserveEvent_orderNotFound_throwsOrderNotFoundException() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(orderRepository.findByIdWithLock(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderSagaListener.handleItemReserveEvent(new InventoryReservedEvent(eventId, orderId))
        ).isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(orderId.toString());

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    // --- handleItemReserveFailedEvent ---

    @Test
    void handleItemReserveFailedEvent_setsStatusToCanceledAndCreatesOutbox() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(orderRepository.findByIdWithLock(orderId)).thenReturn(Optional.of(order));

        InventoryReservationFailedEvent event = new InventoryReservationFailedEvent(eventId, orderId, "Out of stock");
        orderSagaListener.handleItemReserveFailedEvent(event);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
        verify(orderRepository).save(order);
        verify(processedEventRepository).save(any());
        verify(outboxService).createOutbox(order, event);
    }

    @Test
    void handleItemReserveFailedEvent_duplicate_skipsProcessing() {
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        orderSagaListener.handleItemReserveFailedEvent(
                new InventoryReservationFailedEvent(eventId, orderId, "reason")
        );

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(outboxService);
    }

    @Test
    void handleItemReserveFailedEvent_orderNotFound_throwsOrderNotFoundException() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(orderRepository.findByIdWithLock(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderSagaListener.handleItemReserveFailedEvent(
                        new InventoryReservationFailedEvent(eventId, orderId, "reason")
                )
        ).isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    // --- handlePaymentSuccessEvent ---

    @Test
    void handlePaymentSuccessEvent_setsStatusToPaidAndCreatesOutbox() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(orderRepository.findByIdWithLock(orderId)).thenReturn(Optional.of(order));

        PaymentSuccessEvent event = new PaymentSuccessEvent(eventId, orderId, order.getBuyerId(), new BigDecimal("100.00"));
        orderSagaListener.handlePaymentSuccessEvent(event);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
        verify(processedEventRepository).save(any());
        verify(outboxService).createOutbox(order, event);
    }

    @Test
    void handlePaymentSuccessEvent_duplicate_skipsProcessing() {
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        orderSagaListener.handlePaymentSuccessEvent(
                new PaymentSuccessEvent(eventId, orderId, UUID.randomUUID(), BigDecimal.TEN)
        );

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(outboxService);
    }

    @Test
    void handlePaymentSuccessEvent_orderNotFound_throwsOrderNotFoundException() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(orderRepository.findByIdWithLock(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderSagaListener.handlePaymentSuccessEvent(
                        new PaymentSuccessEvent(eventId, orderId, UUID.randomUUID(), BigDecimal.TEN)
                )
        ).isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    // --- handlePaymentFailedEvent ---

    @Test
    void handlePaymentFailedEvent_setsStatusToCanceledAndCreatesOutbox() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(orderRepository.findByIdWithLock(orderId)).thenReturn(Optional.of(order));

        PaymentFailedEvent event = new PaymentFailedEvent(eventId, orderId, "Insufficient funds");
        orderSagaListener.handlePaymentFailedEvent(event);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
        verify(orderRepository).save(order);
        verify(processedEventRepository).save(any());
        verify(outboxService).createOutbox(order, event);
    }

    @Test
    void handlePaymentFailedEvent_duplicate_skipsProcessing() {
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        orderSagaListener.handlePaymentFailedEvent(
                new PaymentFailedEvent(eventId, orderId, "reason")
        );

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(outboxService);
    }

    @Test
    void handlePaymentFailedEvent_orderNotFound_throwsOrderNotFoundException() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(orderRepository.findByIdWithLock(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderSagaListener.handlePaymentFailedEvent(
                        new PaymentFailedEvent(eventId, orderId, "reason")
                )
        ).isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }
}
