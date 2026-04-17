package dn.shared.event.order;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record OrderCancelledEvent(UUID eventId,
                                  UUID orderId,
                                  String reason,
                                  List<OrderItemDto> orders) {
}
