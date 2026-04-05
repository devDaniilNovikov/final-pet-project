package dn.productservice.controller;

import dn.productservice.dto.product.ListProductResponse;
import dn.productservice.dto.product.ProductRequest;
import dn.productservice.dto.product.ProductResponse;
import dn.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ProductController.BASE)
@RequiredArgsConstructor
public class ProductController {

    static final String BASE                    = "/api/v1/products";
    private static final String BY_ID          = "/{id}";
    private static final String BY_SELLER      = "/seller/{sellerId}";
    private static final String BY_CATEGORY    = "/category/{categoryId}";

    private static final String DEFAULT_PAGE_NUMBER = "0";
    private static final String DEFAULT_PAGE_SIZE   = "10";

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }

    @GetMapping(BY_ID)
    public ProductResponse getProductById(@PathVariable String id) {
        return productService.findById(id);
    }

    @GetMapping
    public ListProductResponse getAllProducts(
            @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return productService.findAll(page, size);
    }

    @GetMapping(BY_SELLER)
    public ListProductResponse getProductsBySeller(
            @PathVariable String sellerId,
            @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return productService.findBySellerId(sellerId, size, page);
    }

    @GetMapping(BY_CATEGORY)
    public ListProductResponse getProductsByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return productService.findByCategoryId(categoryId, size, page);
    }

    @PutMapping(BY_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProduct(@PathVariable String id,
                              @Valid @RequestBody ProductRequest request) {
        productService.updateProduct(id, request);
    }

    @DeleteMapping(BY_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable String id) {
        productService.deleteById(id);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProducts(@RequestParam List<String> ids) {
        productService.deleteByIds(ids);
    }
}
