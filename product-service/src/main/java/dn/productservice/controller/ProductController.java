package dn.productservice.controller;

import dn.productservice.dto.product.ListProductResponse;
import dn.productservice.dto.product.ProductRequest;
import dn.productservice.dto.product.ProductResponse;
import dn.productservice.exception.ErrorBody;
import dn.productservice.service.product.ProductService;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Tag(name = "Products", description = "API для управления товарами маркетплейса")
@RestController
@RequestMapping(ProductController.BASE)
@RequiredArgsConstructor
public class ProductController {

    static final String BASE                    = "/api/v1/products";
    private static final String BY_ID          = "/{id}";
    private static final String BY_SELLER      = "/seller/{sellerId}";
    private static final String BY_CATEGORY    = "/category/{categoryId}";
    private static final String BATCH          = "/batch";

    private static final String DEFAULT_PAGE_NUMBER = "0";
    private static final String DEFAULT_PAGE_SIZE   = "10";

    private final ProductService productService;

    @Operation(summary = "Создать товар", description = "Создаёт новый товар в каталоге маркетплейса")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Товар создан",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }

    @Operation(summary = "Получить товар по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Товар найден",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Товар не найден",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @GetMapping(BY_ID)
    public ProductResponse getProductById(
            @Parameter(description = "UUID товара", required = true) @PathVariable String id) {
        return productService.findById(id);
    }

    @Operation(summary = "Получить все товары", description = "Возвращает постраничный список всех товаров")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список товаров",
                    content = @Content(schema = @Schema(implementation = ListProductResponse.class)))
    })
    @GetMapping
    public ListProductResponse getAllProducts(
            @Parameter(description = "Номер страницы (с 0)", example = "0") @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) int page,
            @Parameter(description = "Размер страницы", example = "10") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return productService.findAll(page, size);
    }

    @Operation(summary = "Получить товары продавца")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список товаров продавца",
                    content = @Content(schema = @Schema(implementation = ListProductResponse.class)))
    })
    @GetMapping(BY_SELLER)
    public ListProductResponse getProductsBySeller(
            @Parameter(description = "UUID продавца", required = true) @PathVariable String sellerId,
            @Parameter(description = "Номер страницы (с 0)", example = "0") @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) int page,
            @Parameter(description = "Размер страницы", example = "10") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return productService.findBySellerId(sellerId, size, page);
    }

    @Operation(summary = "Получить товары по категории")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список товаров категории",
                    content = @Content(schema = @Schema(implementation = ListProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Категория не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @GetMapping(BY_CATEGORY)
    public ListProductResponse getProductsByCategory(
            @Parameter(description = "UUID категории", required = true) @PathVariable String categoryId,
            @Parameter(description = "Номер страницы (с 0)", example = "0") @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) int page,
            @Parameter(description = "Размер страницы", example = "10") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return productService.findByCategoryId(categoryId, size, page);
    }

    @Operation(summary = "Обновить товар")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Товар обновлён"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Товар не найден",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @PutMapping(BY_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProduct(
            @Parameter(description = "UUID товара", required = true) @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {
        productService.updateProduct(id, request);
    }

    @Operation(summary = "Удалить товар по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Товар удалён"),
            @ApiResponse(responseCode = "404", description = "Товар не найден",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @DeleteMapping(BY_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(
            @Parameter(description = "UUID товара", required = true) @PathVariable String id) {
        productService.deleteById(id);
    }

    @Operation(summary = "Удалить несколько товаров по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Товары удалены"),
            @ApiResponse(responseCode = "400", description = "Неверный запрос",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProducts(
            @Parameter(description = "Список UUID товаров", required = true) @RequestParam List<String> ids) {
        productService.deleteByIds(ids);
    }

    @Operation(summary = "Получить товары по набору ID (batch)", description = "Используется order-service для проверки наличия товаров")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Набор найденных товаров",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class)))
    })
    @PostMapping(BATCH)
    public Set<ProductResponse> getProductsByIds(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Набор UUID товаров", required = true)
            @RequestBody Set<UUID> ids) {
        return productService.findAllByIdsList(ids);
    }
}
