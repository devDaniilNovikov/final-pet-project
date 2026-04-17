package dn.shared.event.order;

import lombok.Builder;

import java.util.UUID;

@Builder
public record OrderConfirmedEvent(UUID eventId,
                                  UUID orderId,
                                  UUID buyerId) {
}
