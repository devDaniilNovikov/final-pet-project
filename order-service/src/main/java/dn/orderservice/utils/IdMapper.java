package dn.orderservice.utils;

import lombok.experimental.UtilityClass;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

@UtilityClass
public class IdMapper {

     public static UUID mapToUUIDFromString(String orderId) {
         if (orderId.isBlank()){
             throw new IllegalArgumentException("OrderId can't be blank");
         }
         try {
             return UUID.fromString(orderId);
         }catch (IllegalArgumentException ex) {
             throw new IllegalArgumentException(
                     MessageFormat.format("Something went wrong cause: {0} please try again", ex.getMessage()));
         }
    }

     public static List<UUID> mapToListUUIDFromString(List<String> orderIds){
        return orderIds.stream()
                .map(IdMapper::mapToUUIDFromString)
                .toList();
    }
}
