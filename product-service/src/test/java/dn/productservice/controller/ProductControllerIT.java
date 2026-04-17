package dn.productservice.controller;

import dn.productservice.AbstractProductIT;
import dn.productservice.dto.category.CategoryRequest;
import dn.productservice.dto.product.ProductRequest;
import dn.productservice.entity.CategoryEntity;
import dn.productservice.entity.ProductEntity;
import dn.productservice.entity.ProductStatus;
import dn.productservice.repository.CategoryRepository;
import dn.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedKafka(partitions = 1, topics = {
        "product.created", "product.updated", "product.deleted",
        "order.confirmed", "order.created", "order.cancelled", "order.paid",
        "item.reserved", "item.reserved.failed"
})
class ProductControllerIT extends AbstractProductIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private CategoryEntity savedCategory;
    private ProductEntity savedProduct;
    private UUID sellerId;

    @BeforeEach
    void setUp() {
        // CASCADE truncate respects FK constraints in correct order
        jdbcTemplate.execute("TRUNCATE marketplace.products CASCADE");
        jdbcTemplate.execute("TRUNCATE marketplace.categories CASCADE");

        sellerId = UUID.randomUUID();

        savedCategory = categoryRepository.save(buildCategory("Electronics", null));
        savedProduct = productRepository.save(buildProduct("Laptop", sellerId, savedCategory));
    }

    // --- POST /api/v1/categories ---

    @Test
    void createCategory_validRequest_returns201() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Books")
                .build();

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/v1/categories", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(categoryRepository.findAll().stream()
                .anyMatch(c -> c.getName().equals("Books"))).isTrue();
    }

    @Test
    void createCategory_blankName_returns400() {
        CategoryRequest request = CategoryRequest.builder().name("").build();

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/categories", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createCategory_withParent_returns201() {
        CategoryRequest child = CategoryRequest.builder()
                .name("Laptops")
                .parentCategoryId(savedCategory.getId())
                .build();

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/v1/categories", child, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // --- GET /api/v1/categories/{id} ---

    @Test
    void getCategoryById_existingCategory_returns200() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/categories/{id}", Map.class, savedCategory.getId().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("name")).isEqualTo("Electronics");
    }

    @Test
    void getCategoryById_unknownId_returns404() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/categories/{id}", Map.class, UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- POST /api/v1/products ---

    @Test
    void createProduct_validRequest_returns201AndPersists() {
        ProductRequest request = ProductRequest.builder()
                .name("Smartphone")
                .description("Flagship phone")
                .price(new BigDecimal("79999.99"))
                .quantity(20)
                .status(ProductStatus.ACTIVE)
                .sellerId(sellerId)
                .categoryId(savedCategory.getId())
                .build();

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/products", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("id");
        assertThat(response.getBody().get("name")).isEqualTo("Smartphone");
    }

    @Test
    void createProduct_blankName_returns400() {
        ProductRequest request = ProductRequest.builder()
                .name("")
                .description("desc")
                .price(new BigDecimal("100.00"))
                .quantity(5)
                .status(ProductStatus.ACTIVE)
                .sellerId(sellerId)
                .build();

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/products", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createProduct_negativePriceZero_returns400() {
        ProductRequest request = ProductRequest.builder()
                .name("Bad product")
                .description("desc")
                .price(BigDecimal.ZERO)
                .quantity(5)
                .status(ProductStatus.ACTIVE)
                .sellerId(sellerId)
                .build();

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/products", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // --- GET /api/v1/products/{id} ---

    @Test
    void getProductById_existingProduct_returns200() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/products/{id}", Map.class, savedProduct.getId().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("name")).isEqualTo("Laptop");
    }

    @Test
    void getProductById_unknownId_returns404() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/products/{id}", Map.class, UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- GET /api/v1/products ---

    @Test
    void getAllProducts_returns200WithPagedList() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/products?page=0&size=10", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("products");
    }

    // --- GET /api/v1/products/seller/{sellerId} ---

    @Test
    void getProductsBySeller_existingSeller_returnsProducts() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/products/seller/{sellerId}?page=0&size=10",
                Map.class, sellerId.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("products");
    }

    // --- POST /api/v1/products/batch ---

    @Test
    void getProductsByBatch_returnsMatchingProducts() {
        Set<UUID> ids = Set.of(savedProduct.getId());

        ResponseEntity<Object[]> response = restTemplate.postForEntity(
                "/api/v1/products/batch", ids, Object[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getProductsByBatch_unknownIds_returnsEmptySet() {
        Set<UUID> ids = Set.of(UUID.randomUUID());

        ResponseEntity<Object[]> response = restTemplate.postForEntity(
                "/api/v1/products/batch", ids, Object[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // --- PUT /api/v1/products/{id} ---

    @Test
    void updateProduct_validRequest_returns204AndPersists() {
        ProductRequest update = ProductRequest.builder()
                .name("Laptop Pro")
                .description("Updated description")
                .price(new BigDecimal("120000.00"))
                .quantity(3)
                .status(ProductStatus.ACTIVE)
                .sellerId(sellerId)
                .build();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/products/{id}", HttpMethod.PUT,
                new HttpEntity<>(update), Void.class,
                savedProduct.getId().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ProductEntity updated = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Laptop Pro");
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("120000.00"));
    }

    @Test
    void updateProduct_unknownId_returns404() {
        ProductRequest update = ProductRequest.builder()
                .name("X")
                .description("desc")
                .price(new BigDecimal("10.00"))
                .quantity(1)
                .status(ProductStatus.ACTIVE)
                .sellerId(sellerId)
                .build();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/products/{id}", HttpMethod.PUT,
                new HttpEntity<>(update), Map.class,
                UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- DELETE /api/v1/products/{id} ---

    @Test
    void deleteProduct_existingProduct_returns204AndRemovesFromDb() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/products/{id}", HttpMethod.DELETE,
                null, Void.class,
                savedProduct.getId().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(productRepository.findById(savedProduct.getId())).isEmpty();
    }

    @Test
    void deleteProduct_unknownId_returns404() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/products/{id}", HttpMethod.DELETE,
                null, Map.class,
                UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- helpers ---

    private CategoryEntity buildCategory(String name, CategoryEntity parent) {
        CategoryEntity c = new CategoryEntity();
        c.setName(name);
        c.setParentCategory(parent);
        return c;
    }

    private ProductEntity buildProduct(String name, UUID sellerId, CategoryEntity category) {
        ProductEntity p = new ProductEntity();
        p.setName(name);
        p.setDescription("Test description");
        p.setPrice(new BigDecimal("50000.00"));
        p.setQuantity(10);
        p.setStatus(ProductStatus.ACTIVE);
        p.setSellerId(sellerId);
        p.setCategory(category);
        return p;
    }
}
