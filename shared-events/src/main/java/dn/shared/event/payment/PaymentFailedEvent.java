package dn.shared.event.payment;

import lombok.Builder;

import java.util.UUID;

@Builder
public record PaymentFailedEvent(UUID eventId,
                                 UUID orderId,
                                 String reason) {
}
