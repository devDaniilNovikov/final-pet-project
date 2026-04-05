package dn.shared.event.order;

import lombok.Builder;

@Builder
public record OrderPaidEvent(String orderId,
                             String buyerId) {
}
