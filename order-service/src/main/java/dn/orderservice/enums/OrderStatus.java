package dn.orderservice.entity;

import java.io.Serializable;

public enum OrderStatus implements Serializable {

    PENDING,
    PAID,
    CANCELED,
    CONFIRMED,
}
