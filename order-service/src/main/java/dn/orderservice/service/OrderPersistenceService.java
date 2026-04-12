package dn.orderservice.service;


import dn.orderservice.dto.request.OrderRequest;
import dn.orderservice.dto.response.OrderResponse;
import dn.orderservice.dto.response.ProductResponse;
import dn.orderservice.entity.OrderEntity;
import dn.orderservice.entity.OrderItemEntity;
import dn.orderservice.entity.OutboxEntity;
import dn.orderservice.enums.OrderStatus;
import dn.orderservice.mapper.OutboxMapper;
import dn.orderservice.repository.OrderRepository;
import dn.orderservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPersistenceService {

    private final OrderRepository orderRepository;
    private final OutboxMapper outboxMapper;
    private final OutboxRepository outboxRepository;

    @Value("${app.kafka.events.order-created}")
    private String orderCreatedEvent;

    @Transactional
    public OrderResponse persistOrder(OrderRequest orderRequest,
                                      Set<ProductResponse> productResponseList){
        Map<UUID,Integer> quantityMap = MapBuilderService.buildQuantityMap(orderRequest);
        Map<UUID,BigDecimal> priceMap = MapBuilderService.buildPriceMap(
                productResponseList,
                quantityMap
        );
        BigDecimal totalOrderPrice = MapBuilderService.calculate(priceMap);
        OrderEntity orderEntity = OrderEntity.builder()
                .buyerId(orderRequest.getBuyerId())
                .totalPrice(totalOrderPrice)
                .orderStatus(OrderStatus.PENDING)
                .build();
        List<OrderItemEntity> orderItemItems = mapToOrderItem(
                orderRequest,
                priceMap,
                MapBuilderService.buildProductNameMap(productResponseList),
                orderEntity
        );
        orderEntity.setOrderItemEntities(orderItemItems);
        orderRepository.save(orderEntity);
        OutboxEntity outboxEntity = outboxMapper.toOutboxEntity(orderEntity, orderItemItems, orderCreatedEvent);
        outboxRepository.save(outboxEntity);
        log.info("Order={} saved, outbox entry created", orderEntity.getId());
        return new OrderResponse(orderEntity.getId(), orderEntity.getOrderStatus());
    }

    private List<OrderItemEntity> mapToOrderItem(OrderRequest orderRequest,
                                                 Map<UUID, BigDecimal> priceMap,
                                                 Map<UUID, String> productNameMap,
                                                 OrderEntity order){
        return orderRequest.getItems()
                .stream()
                .map(orderItemRequest -> OrderItemEntity.builder()
                        .productId(orderItemRequest.getProductId())
                        .quantity(orderItemRequest.getQuantity())
                        .order(order)
                        .priceAtPurchase(priceMap.get(orderItemRequest.getProductId()))
                        .productName(productNameMap.get(orderItemRequest.getProductId()))
                        .build())
                .toList();

    }
}
