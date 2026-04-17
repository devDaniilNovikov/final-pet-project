package dn.orderservice.service.order;
import dn.orderservice.client.ProductClient;
import dn.orderservice.dto.request.OrderItemRequest;
import dn.orderservice.dto.request.OrderRequest;
import dn.orderservice.dto.response.OrderResponse;
import dn.orderservice.dto.response.ProductResponse;
import dn.orderservice.exception.OrderNotFoundException;
import dn.orderservice.exception.ProductNotFoundException;
import dn.orderservice.mapper.OrderMapper;
import dn.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {


    private final OrderPersistenceService orderPersistenceService;
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;

    private final ProductClient productClient;

    private void validateIds(Set<UUID> productIds,
                             Set<ProductResponse> productResponseList){
        Set<UUID> foundIds = productResponseList.stream()
                .map(ProductResponse::getProductId)
                .collect(Collectors.toSet());
        Set<UUID> missingIds = productIds.stream()
                .filter(id->!foundIds.contains(id))
                .collect(Collectors.toSet());
        if (!foundIds.containsAll(productIds)){
            throw new ProductNotFoundException(
                    MessageFormat.format("Products not found with ids: {0}",missingIds));
        }
    }

    public OrderResponse getOrderById(UUID orderId){
        return orderMapper.toResponse(orderRepository.findById(orderId)
                .orElseThrow(()->new OrderNotFoundException(
                        MessageFormat.format("Order with id: {0} not found",orderId)
                )));
    }



    public OrderResponse createOrder(OrderRequest orderRequest){
        Set<UUID> productIds = getProductIds(orderRequest);
        Set<ProductResponse> productResponseSet = getProductsByHttpRequest(productIds);
        validateIds(productIds,productResponseSet);
        return orderPersistenceService.persistOrder(orderRequest,productResponseSet);

    }


    private Set<UUID> getProductIds(OrderRequest orderRequest){
        return orderRequest.getItems()
                .stream()
                .map(OrderItemRequest::getProductId)
                .collect(Collectors.toSet());
    }

    private Set<ProductResponse> getProductsByHttpRequest(Set<UUID> productIds){
        Set<ProductResponse> productResponses = productClient.batch(productIds);
        Map<UUID,Integer> productsCollection = productResponses.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        ProductResponse::getProductId,
                        ProductResponse::getQuantity)
                );
        if (productsCollection.size() != productIds.size()){
            final String LOG_MESSAGE = "ProductsCollection must be equals size of productIds";
            log.error(LOG_MESSAGE);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, LOG_MESSAGE);
        }
        return productResponses;
    }
}
