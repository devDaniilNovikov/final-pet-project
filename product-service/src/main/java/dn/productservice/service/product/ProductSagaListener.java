package dn.productservice.service.product;
import dn.productservice.entity.ProductEntity;
import dn.productservice.exception.InvalidProductQuantityException;
import dn.productservice.repository.ProductRepository;
import dn.shared.idempotency.EventProcessor;
import dn.shared.idempotency.ProcessedEventRepository;
import dn.shared.event.order.OrderCancelledEvent;
import dn.shared.event.order.OrderCreatedEvent;
import dn.shared.event.order.OrderItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductSagaListener {

    private static final String PRODUCTS_NOT_FOUND_MESSAGE = "Products not found";

    private final ProductRepository productRepository;
    private final ProductOutboxService productOutboxService;
    private final ProcessedEventRepository processedEventRepository;
    private final EventProcessor eventProcessor;




    
    @KafkaListener(topics = "${app.kafka.events.order-created}")
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderCreatedEvent(OrderCreatedEvent orderCreatedEvent) {
        if (!processedEventRepository.existsById(orderCreatedEvent.eventId())) {
            Map<UUID, Integer> productMap = orderCreatedEvent.orderItems()
                    .stream()
                    .collect(Collectors.toMap(
                            OrderItemDto::productId,
                            OrderItemDto::quantity)
                    );
            List<ProductEntity> productEntities = productRepository.findAllById(productMap.keySet());
            if (productEntities.size() != productMap.size()) {

                productOutboxService.createOutbox(orderCreatedEvent, PRODUCTS_NOT_FOUND_MESSAGE);
                return;
            }
            try {
                List<ProductEntity> productEntityList = productEntities.stream()
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
                eventProcessor.processEvent(orderCreatedEvent.eventId());
                productOutboxService.createOutbox(orderCreatedEvent);
            } catch (InvalidProductQuantityException e) {
                log.error("Error while sending order event cause: {}", e.getMessage());
                productOutboxService.createOutbox(orderCreatedEvent, e.getMessage());
            }
        }
        return;
    };

    @KafkaListener(topics = "${app.kafka.events.order-cancelled}")
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderCancelledEvent(OrderCancelledEvent event) {
        if (!processedEventRepository.existsById(event.eventId())) {
            Map<UUID, Integer> productMap = event.orders()
                    .stream()
                    .collect(Collectors.toMap(
                            OrderItemDto::productId,
                            OrderItemDto::quantity
                    ));
            List<ProductEntity> productEntities = productRepository.findAllById(productMap.keySet());
            if (productEntities.size() != productMap.size()) {
                log.error("Product quantity don't equals");
                return;
            }
            List<ProductEntity> products = productEntities.stream()
                    .peek(productEntity -> {
                        int quantity = productMap.get(productEntity.getId());
                        int finalQuantity = productEntity.getQuantity() + quantity;
                        productEntity.setQuantity(finalQuantity);
                    })
                    .toList();
            List<UUID> productIds = products.stream()
                    .map(ProductEntity::getId)
                    .toList();
            log.info("Charge back is done for products with ids: {}", productIds);
            productRepository.saveAll(products);
            eventProcessor.processEvent(event.eventId());

        }
        return;
    }

}
