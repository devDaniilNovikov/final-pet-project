package dn.productservice.service.product;

import dn.productservice.entity.ProductEntity;
import org.springframework.stereotype.Component;
import dn.shared.event.product.ProductCreateEvent;
import dn.shared.event.product.ProductUpdatedEvent;
import dn.shared.event.product.ProductDeletedEvent;

import java.util.List;
import java.util.UUID;

@Component
public class ProductEventFactory {




    public ProductCreateEvent createProductCreateEvent(ProductEntity product){
        return ProductCreateEvent.builder()
                .eventId(UUID.randomUUID())
                .id(product.getId())
                .name(product.getName())
                .build();
    }

    public ProductUpdatedEvent createProductUpdateEvent(ProductEntity product){
        return ProductUpdatedEvent.builder()
                .eventId(UUID.randomUUID())
                .id(product.getId())
                .updatedTime(product.getUpdatedAt())
                .name(product.getName())
                .build();
    }

    public ProductDeletedEvent createProductDeletedEvent(ProductEntity product){
        return ProductDeletedEvent.builder()
                .eventId(UUID.randomUUID())
                .productName(product.getName())
                .id(product.getId())
                .build();
    }

    public List<ProductDeletedEvent> createProductDeletedEvents(List<ProductEntity> products){
        return products.stream()
                .map(this::createProductDeletedEvent)
                .toList();
    }

}
