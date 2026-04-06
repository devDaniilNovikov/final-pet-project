package dn.shared.event.order;

import lombok.Builder;

import java.util.List;

@Builder
public record OrderCancelledEvent(String orderId,
                                  String reason,
                                  List<OrderItemDto> orders) {
}
