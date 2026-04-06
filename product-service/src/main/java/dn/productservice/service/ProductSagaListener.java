package dn.productservice.service;
import dn.productservice.entity.ProductEntity;
import dn.productservice.exception.InvalidProductQuantityException;
import dn.productservice.exception.ProductNotFoundException;
import dn.productservice.repository.ProductRepository;
import dn.shared.event.inventory.InventoryReservationFailedEvent;
import dn.shared.event.inventory.InventoryReservedEvent;
import dn.shared.event.order.OrderCancelledEvent;
import dn.shared.event.order.OrderCreatedEvent;
import dn.shared.event.order.OrderItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductSagaListener {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;



    @Value("${app.kafka.events.item-reserved}")
    private String itemReservedEvent;

    @Value("${app.kafka.events.item-reserved-failed}")
    private String itemReservedFailedEvent;


    
    @KafkaListener(topics = "${app.kafka.events.order-created}")
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderCreatedEvent(OrderCreatedEvent orderCreatedEvent) {
        Map<UUID, Integer> productMap = orderCreatedEvent.orderItems()
                .stream()
                .collect(Collectors.toConcurrentMap(
                        OrderItemDto::getProductId,
                        OrderItemDto::getQuantity)
                );
        List<ProductEntity> productEntities = productRepository.findAllById(productMap.keySet());
        if (productEntities.size() != productMap.size()) {
            throw new ProductNotFoundException("Products not found");
        }
        try {
            List<ProductEntity> productEntityList = productEntities
                    .stream()
                    .map(product -> {
                        int productQuantity = productMap.get(product.getId());
                        if (product.getQuantity() < productQuantity) {
                            throw new InvalidProductQuantityException(
                                    MessageFormat.format(
                                            "Stock: {0} is less than ordered quantity: {1}", product.getQuantity(), productQuantity)
                            );

                        }
                        final int finalQuantity = product.getQuantity() - productQuantity;
                        product.setQuantity(finalQuantity);
                        log.info("Product quantity {} has been updated", product.getId());
                        return product;
                    })
                    .toList();
            productRepository.saveAll(productEntityList);
            kafkaTemplate.send(itemReservedEvent, InventoryReservedEvent.builder()
                    .orderId(orderCreatedEvent.orderId())
                    .build());
            log.info("Successful sending order event");
        } catch (InvalidProductQuantityException e) {
            log.error("Error while sending order event cause: {}", e.getMessage());
            kafkaTemplate.send(itemReservedFailedEvent, InventoryReservationFailedEvent.builder()
                    .orderId(orderCreatedEvent.orderId())
                    .build());
        }
    }

    @KafkaListener(topics = "${app.kafka.events.order-cancelled}")
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderCancelledEvent(OrderCancelledEvent event){
        Map<UUID,Integer> productMap = event.orders()
                .stream()
                .collect(Collectors.toConcurrentMap(
                        OrderItemDto::getProductId,
                        OrderItemDto::getQuantity
                ));
        List<ProductEntity> productEntities = productRepository.findAllById(productMap.keySet());
        if (productEntities.size() != productMap.size()) {
            throw new ProductNotFoundException("Products not found");
        }
        List<ProductEntity> products = productEntities.stream()
                    .map(productEntity -> {
                        int quantity = productMap.get(productEntity.getId());
                        int finalQuantity = productEntity.getQuantity()+quantity;
                        productEntity.setQuantity(finalQuantity);
                        return productEntity;
                    })
                    .toList();
        log.info("Charge back is done for products with ids: {}",products.stream()
                .map(ProductEntity::getId)
                .toList());
        productRepository.saveAll(products);

    }

}

