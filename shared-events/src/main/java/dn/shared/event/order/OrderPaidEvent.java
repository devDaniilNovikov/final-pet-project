package dn.shared.event.order;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record OrderPaidEvent(UUID eventId,
                             UUID orderId,
                             UUID buyerId,
                             BigDecimal amount) {
}
