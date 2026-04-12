package dn.accountservice.utils;

import lombok.experimental.UtilityClass;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

@UtilityClass
public class IdMapper {

    public UUID mapToUUIDFromString(String orderId) {
        if (orderId.isEmpty()){
            throw new IllegalArgumentException(
                    MessageFormat.format("OrderId: {0} can't be empty or blank",orderId)
            );
        }
        return UUID.fromString(orderId);
    }

    public List<UUID> mapToListUUIDFromString(List<String> orderIds){
        if (orderIds.isEmpty()){
            throw new IllegalArgumentException(
                    MessageFormat.format("OrderIds: {0} can't be empty or blank",orderIds)
            );
        }
        return orderIds.stream()
                .map(IdMapper::mapToUUIDFromString)
                .toList();
    }
}
