package dn.orderservice.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;


public interface IdMapper {

    default UUID mapToUUIDFromString(String orderId) {
        return UUID.fromString(orderId);
    }

    default List<UUID> mapToListUUIDFromString(List<String> orderIds){
        return orderIds.stream()
                .map(this::mapToUUIDFromString)
                .toList();
    }
}
