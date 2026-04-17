package dn.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dn.orderservice.dto.request.OrderItemRequest;
import dn.orderservice.dto.request.OrderRequest;
import dn.orderservice.dto.response.OrderResponse;
import dn.orderservice.enums.OrderStatus;
import dn.orderservice.exception.GlobalExceptionHandler;
import dn.orderservice.service.order.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {OrderController.class, GlobalExceptionHandler.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private static final String URL = "/api/v1/orders";

    @Test
    void createOrder_validRequest_returns201WithResponse() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderRequest request = new OrderRequest(buyerId, List.of(new OrderItemRequest(productId, 2)));
        OrderResponse response = new OrderResponse(orderId, OrderStatus.PENDING);

        when(orderService.createOrder(any(OrderRequest.class))).thenReturn(response);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.orderStatus").value("PENDING"));
    }

    @Test
    void createOrder_missingBuyerId_returns400() throws Exception {
        OrderRequest request = new OrderRequest(null, List.of(new OrderItemRequest(UUID.randomUUID(), 1)));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void createOrder_emptyItems_returns400() throws Exception {
        OrderRequest request = new OrderRequest(UUID.randomUUID(), List.of());

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void createOrder_nullItems_returns400() throws Exception {
        OrderRequest request = new OrderRequest(UUID.randomUUID(), null);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void createOrder_itemWithZeroQuantity_returns400() throws Exception {
        OrderRequest request = new OrderRequest(UUID.randomUUID(),
                List.of(new OrderItemRequest(UUID.randomUUID(), 0)));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void createOrder_serviceThrowsException_returns500() throws Exception {
        OrderRequest request = new OrderRequest(UUID.randomUUID(),
                List.of(new OrderItemRequest(UUID.randomUUID(), 1)));

        when(orderService.createOrder(any())).thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }
}