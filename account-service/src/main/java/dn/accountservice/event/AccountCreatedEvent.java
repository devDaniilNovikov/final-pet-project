package dn.accountservice.event;

public record AccountCreatedEvent(String username,
                                  String id) {
}
