package dn.shared.event.payment;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record PaymentSuccessEvent(UUID eventId,
                                  UUID orderId,
                                  UUID buyerId,
                                  BigDecimal amount) {
}
