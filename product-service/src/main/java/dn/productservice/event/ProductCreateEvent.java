package dn.productservice.event;


import lombok.Builder;

import java.util.UUID;

@Builder
public record ProductCreateEvent(UUID id, String name) {
}
