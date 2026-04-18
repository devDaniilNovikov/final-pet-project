package dn.shared.outbox;

import java.io.Serializable;

public enum OutboxStatus implements Serializable {

    PENDING,
    IN_PROGRESS,
    SENT,
    FAILED,
}
