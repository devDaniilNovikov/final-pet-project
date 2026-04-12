package dn.orderservice.controller;

import dn.orderservice.dto.request.OrderRequest;
import dn.orderservice.dto.response.OrderResponse;
import dn.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Orders", description = "API для управления заказами (SAGA-паттерн)")
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    private static final String CREATE_ORDER    = "/api/v1/orders";
    private static final String GET_ORDER_BY_ID = "/api/v1/orders/{orderId}";

    @Operation(summary = "Создать заказ",
            description = "Создаёт новый заказ и запускает SAGA: резервирование товаров → подтверждение/отмена → оплата")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Заказ создан, SAGA запущена",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации запроса",
                    content = @Content(schema = @Schema(implementation = Schema.class))),
            @ApiResponse(responseCode = "404", description = "Товары не найдены")
    })
    @PostMapping(CREATE_ORDER)
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@RequestBody @Valid OrderRequest orderRequest) {
        return orderService.createOrder(orderRequest);
    }

    @Operation(summary = "Получить заказ по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заказ найден",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @GetMapping(GET_ORDER_BY_ID)
    @ResponseStatus(HttpStatus.OK)
    public OrderResponse getOrderById(
            @Parameter(description = "UUID заказа", required = true) @PathVariable("orderId") UUID id) {
        return orderService.getOrderById(id);
    }
}
