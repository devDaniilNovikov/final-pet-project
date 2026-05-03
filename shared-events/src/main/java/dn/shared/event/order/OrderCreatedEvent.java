package dn.shared.event.order;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record OrderCreatedEvent(UUID eventId,
                                UUID orderId,
                                UUID buyerId,
                                UUID sellerId,
                                String recipientContact,
                                String sellerContact,
                                List<OrderItemDto> orderItems,
                                BigDecimal totalPrice) {
}
