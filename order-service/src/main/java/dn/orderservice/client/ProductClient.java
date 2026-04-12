package dn.orderservice.client;

import dn.orderservice.dto.response.ProductResponse;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public interface ProductClient {

    String URL = "/batch";

    Map<String,String> headers = Map.of("","");


    @PostExchange(value = URL,headers = {
            "",""
    })
    Set<ProductResponse> batch(Set<UUID> ids);
}
