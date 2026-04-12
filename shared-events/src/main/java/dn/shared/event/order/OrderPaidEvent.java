package dn.shared.event.order;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderPaidEvent(String orderId,
                             String buyerId,
                             BigDecimal amount) {
}
