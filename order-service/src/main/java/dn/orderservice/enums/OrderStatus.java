package dn.orderservice.enums;

import java.io.Serializable;

public enum OrderStatus implements Serializable {

    PENDING,
    PAID,
    CANCELED,
    CONFIRMED,
}
