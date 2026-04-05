package dn.shared.event.order;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record OrderCreatedEvent(String orderId,
                                String buyerId,
                                List<OrderItemDto> orderItems,
                                BigDecimal totalPrice) {
}
