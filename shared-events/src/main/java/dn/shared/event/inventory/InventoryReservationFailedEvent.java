package dn.shared.event.inventory;

import lombok.Builder;

@Builder
public record InventoryReservationFailedEvent(String orderId,
                                              String reason) {
}
