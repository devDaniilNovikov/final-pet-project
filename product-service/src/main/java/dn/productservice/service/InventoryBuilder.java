package dn.productservice.service;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import dn.shared.event.inventory.InventoryReservedEvent;
import dn.shared.event.inventory.InventoryReservationFailedEvent;

import java.util.UUID;

@Component
public class InventoryBuilder {


    public InventoryReservedEvent buildInventoryReservedEvent(UUID orderId,
                                                              UUID eventId){
        return InventoryReservedEvent.builder()
                .orderId(orderId)
                .eventId(eventId)
                .build();
    }

    public InventoryReservationFailedEvent buildInventoryReservationFailedEvent(UUID orderId,
                                                                                UUID eventId,
                                                                                String reason){
        return InventoryReservationFailedEvent.builder()
                .orderId(orderId)
                .eventId(eventId)
                .reason(reason)
                .build();
    }
}
