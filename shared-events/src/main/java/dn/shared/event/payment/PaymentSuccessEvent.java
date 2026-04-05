package dn.shared.event.payment;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentSuccessEvent(String orderId,
                                  String buyerId,
                                  BigDecimal amount) {
}
