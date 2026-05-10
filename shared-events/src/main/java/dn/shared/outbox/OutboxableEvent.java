package dn.shared.outbox;

import java.util.UUID;

public interface OutboxableEvent {

    UUID id();
    UUID eventId();
}
