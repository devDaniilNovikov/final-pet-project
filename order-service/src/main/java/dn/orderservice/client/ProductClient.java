package dn.orderservice.client;

import dn.orderservice.dto.ProductResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.UUID;


public interface ProductClientExchange {

    String BASE_URL = "http://localhost:3001/api/v1/products";

    @GetExchange(BASE_URL+"/{id}")
    ProductResponse getProductResponseById(@PathVariable UUID id);
}
