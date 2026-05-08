package dn.orderservice.dto;

import dn.orderservice.dto.request.OrderRequest;
import dn.orderservice.entity.OrderEntity;


import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;


public record OrderWithProductsRequest(
        OrderRequest orderRequest,
        Map<UUID, BigDecimal> priceMap,
        Map<UUID, String> productNameMap,
        OrderEntity order) {

}
