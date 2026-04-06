package dn.orderservice.mapper;

import dn.orderservice.entity.OrderItemEntity;
import dn.shared.event.order.OrderItemDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderItemMapper {

    @Mapping(source = "priceAtPurchase", target = "price")
    OrderItemDto toDto(OrderItemEntity entity);

    List<OrderItemDto> toDtoList(List<OrderItemEntity> entities);
}
