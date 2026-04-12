package dn.orderservice.service;
import dn.orderservice.dto.request.OrderItemRequest;
import dn.orderservice.dto.request.OrderRequest;
import dn.orderservice.dto.response.ProductResponse;
import lombok.experimental.UtilityClass;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@UtilityClass
public class MapBuilderService {

    public static BigDecimal calculate(Map<UUID,BigDecimal> priceMap ){
        return priceMap.values()
                .stream()
                .reduce(BigDecimal.ZERO,BigDecimal::add);
    }

    public static Map<UUID,BigDecimal> buildPriceMap(Set<ProductResponse> responses,
                                                      Map<UUID,Integer> quantityMap){
        return responses.stream()
                .collect(Collectors.toMap(
                        ProductResponse::getProductId,
                        productResponse -> {
                            var quantity = quantityMap.get(productResponse.getProductId());
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

    public static Map<UUID,String> buildProductNameMap(Set<ProductResponse> responses){
        return responses.stream()
                .collect(Collectors.toMap(
                        ProductResponse::getProductId,
                        ProductResponse::getProductName)
                );
    }


}
