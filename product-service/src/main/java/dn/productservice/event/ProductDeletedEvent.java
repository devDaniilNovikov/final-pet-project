package dn.productservice.event;


import lombok.Builder;

import java.util.List;

@Builder
public record ProductDeletedEvent(List<String> ids, String productId) {
}
