package dn.orderservice.service;
import dn.orderservice.dto.request.OrderItemRequest;
import dn.orderservice.dto.request.OrderRequest;
import dn.orderservice.dto.response.ProductResponse;
import dn.orderservice.exception.ProductNotFoundException;
import lombok.experimental.UtilityClass;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class MapBuilderService {

    public BigDecimal calculateTotalPrice(Map<UUID,BigDecimal> priceMap ){
        return priceMap.values()
                .stream()
                .reduce(BigDecimal.ZERO,BigDecimal::add);
    }

    public Map<UUID,BigDecimal> buildPriceMap(Set<ProductResponse> responses,
                                               Map<UUID,Integer> quantityMap){
        return responses.stream()
                .collect(Collectors.toMap(
                        ProductResponse::getProductId,
                        productResponse -> {
                            var product = quantityMap.get(productResponse.getProductId());
                            int quantity = Optional.ofNullable(product)
                                    .orElseThrow(()->new ProductNotFoundException(
                                            MessageFormat.format("Product with id: {0} not found",productResponse.getProductId())
                                    ));

                            return productResponse.getPrice().multiply(BigDecimal.valueOf(quantity));
                        }
                ));
    }
    public static Map<UUID,Integer> buildQuantityMap(OrderRequest orderRequest){
        return orderRequest.getItems()
                .stream()
                .collect(Collectors.toMap(
                        OrderItemRequest::getProductId,
                        OrderItemRequest::getQuantity)
                );
    }

    public Map<UUID,String> buildProductNameMap(Set<ProductResponse> responses){
        for (int i = 0; i < responses.size(); i++) {

        }
        return responses.stream()
                .collect(Collectors.toMap(
                        ProductResponse::getProductId,
                        ProductResponse::getProductName)
                );
    }


}
