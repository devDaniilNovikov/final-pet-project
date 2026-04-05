package dn.productservice.event;


import lombok.Builder;

@Builder
public record ProductCreateEvent(String id, String name) {
}
