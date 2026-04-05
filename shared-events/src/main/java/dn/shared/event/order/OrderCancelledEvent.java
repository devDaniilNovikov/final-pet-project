package dn.shared.event.order;

import lombok.Builder;

@Builder
public record OrderCancelledEvent(String orderId,
                                  String reason) {
}
