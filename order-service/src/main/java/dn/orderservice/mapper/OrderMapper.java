package dn.orderservice.mapper;

import dn.orderservice.dto.response.OrderResponse;
import dn.orderservice.entity.OrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "id", target = "orderId")
    OrderResponse toResponse(OrderEntity entity);

    @Mapping(source = "orderId", target = "id")
    @Mapping(target = "buyerId", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "orderItemEntities", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderEntity toEntity(OrderResponse response);

    List<OrderResponse> toResponseList(List<OrderEntity> entities);

    List<OrderEntity> toEntityList(List<OrderResponse> responses);
}

