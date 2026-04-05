package dn.shared.event.payment;

import lombok.Builder;

@Builder
public record PaymentFailedEvent(String orderId,
                                 String reason) {
}
