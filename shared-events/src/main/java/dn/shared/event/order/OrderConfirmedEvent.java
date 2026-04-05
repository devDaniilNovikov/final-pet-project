package dn.shared.event.order;

import lombok.Builder;

@Builder
public record OrderConfirmedEvent(String orderId,
                                  String buyerId) {
}
