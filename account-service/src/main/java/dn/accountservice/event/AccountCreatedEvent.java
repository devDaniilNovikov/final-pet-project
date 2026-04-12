package dn.accountservice.event;

import lombok.Builder;

@Builder
public record AccountCreatedEvent(String username,
                                  String id) {
}
