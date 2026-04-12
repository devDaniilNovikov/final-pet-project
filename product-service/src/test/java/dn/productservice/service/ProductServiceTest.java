package dn.productservice.service;

import dn.productservice.dto.image.ProductImageRequest;
import dn.productservice.dto.product.ListProductResponse;
import dn.productservice.dto.product.ProductRequest;
import dn.productservice.dto.product.ProductResponse;
import dn.productservice.entity.CategoryEntity;
import dn.productservice.entity.ProductEntity;
import dn.productservice.entity.ProductStatus;
import dn.productservice.event.ProductCreateEvent;
import dn.productservice.event.ProductDeletedEvent;
import dn.productservice.event.ProductUpdatedEvent;
import dn.productservice.exception.ProductNotFoundException;
import dn.productservice.utils.IdConverter;
import dn.productservice.mapper.ProductImageMapper;
import dn.productservice.mapper.ProductMapper;
import dn.productservice.repository.CategoryRepository;
import dn.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductMapper productMapper;
    @Mock private ProductImageMapper productImageMapper;
    @Mock private EventService eventService;
    @InjectMocks
    private ProductService productService;

    @Captor private ArgumentCaptor<ProductCreateEvent> createdEventCaptor;
    @Captor private ArgumentCaptor<ProductUpdatedEvent> updatedEventCaptor;
    @Captor private ArgumentCaptor<ProductDeletedEvent> deletedEventCaptor;

    private UUID productId;
    private String productIdStr;
    private ProductEntity productEntity;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        productIdStr = productId.toString();

        productEntity = new ProductEntity();
        productEntity.setId(productId);
        productEntity.setName("Test Product");
        productEntity.setPrice(BigDecimal.valueOf(99.99));
        productEntity.setStatus(ProductStatus.ACTIVE);

        productResponse = ProductResponse.builder()
                .id(productIdStr)
                .name("Test Product")
                .price(BigDecimal.valueOf(99.99))
                .build();

    }
    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("должен создать продукт без категории и изображений")
        void shouldCreateProductWithoutCategoryAndImages() {
            ProductRequest request = ProductRequest.builder()
                    .name("Test Product")
                    .price(BigDecimal.valueOf(99.99))
                    .status(ProductStatus.ACTIVE)
                    .sellerId(UUID.randomUUID())
                    .build();

            when(productMapper.toEntity(request)).thenReturn(productEntity);
            when(productRepository.save(productEntity)).thenReturn(productEntity);
            when(productMapper.toResponse(productEntity)).thenReturn(productResponse);

            ProductResponse result = productService.createProduct(request);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Test Product");
            verify(productRepository).save(productEntity);
            verify(eventService).sendProductCreatedEvent(createdEventCaptor.capture());
            assertThat(createdEventCaptor.getValue().id()).isEqualTo(productIdStr);
        }

        @Test
        @DisplayName("должен установить категорию если categoryId передан")
        void shouldSetCategoryWhenCategoryIdProvided() {
            UUID categoryId = UUID.randomUUID();
            CategoryEntity category = new CategoryEntity();
            category.setId(categoryId);

            ProductRequest request = ProductRequest.builder()
                    .name("Test Product")
                    .price(BigDecimal.valueOf(99.99))
                    .status(ProductStatus.ACTIVE)
                    .sellerId(UUID.randomUUID())
                    .categoryId(categoryId)
                    .build();

            when(productMapper.toEntity(request)).thenReturn(productEntity);
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(productRepository.save(productEntity)).thenReturn(productEntity);
            when(productMapper.toResponse(productEntity)).thenReturn(productResponse);

            productService.createProduct(request);

            assertThat(productEntity.getCategory()).isEqualTo(category);
            verify(categoryRepository).findById(categoryId);
        }

        @Test
        @DisplayName("не должен обращаться к categoryRepository если categoryId null")
        void shouldNotCallCategoryRepositoryWhenCategoryIdNull() {
            ProductRequest request = ProductRequest.builder()
                    .name("Test Product")
                    .price(BigDecimal.valueOf(99.99))
                    .status(ProductStatus.ACTIVE)
                    .sellerId(UUID.randomUUID())
                    .build();

            when(productMapper.toEntity(request)).thenReturn(productEntity);
            when(productRepository.save(productEntity)).thenReturn(productEntity);
            when(productMapper.toResponse(productEntity)).thenReturn(productResponse);

            productService.createProduct(request);

            verifyNoInteractions(categoryRepository);
        }

        @Test
        @DisplayName("должен сохранить изображения при создании продукта")
        void shouldSaveImagesWhenProvided() {
            ProductImageRequest imageRequest = new ProductImageRequest("http://img.url", 1);
            ProductRequest request = ProductRequest.builder()
                    .name("Test Product")
                    .price(BigDecimal.valueOf(99.99))
                    .status(ProductStatus.ACTIVE)
                    .sellerId(UUID.randomUUID())
                    .images(List.of(imageRequest))
                    .build();

            var imageEntity = new dn.productservice.entity.ProductImageEntity();
            imageEntity.setUrl("http://img.url");

            when(productMapper.toEntity(request)).thenReturn(productEntity);
            when(productImageMapper.toEntity(imageRequest)).thenReturn(imageEntity);
            when(productRepository.save(productEntity)).thenReturn(productEntity);
            when(productMapper.toResponse(productEntity)).thenReturn(productResponse);

            productService.createProduct(request);

            assertThat(productEntity.getProductImageEntities()).hasSize(1);
            assertThat(imageEntity.getProduct()).isEqualTo(productEntity);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("должен вернуть продукт когда он существует")
        void shouldReturnProductWhenExists() {
            when(productRepository.findById(productId)).thenReturn(Optional.of(productEntity));
            when(productMapper.toResponse(productEntity)).thenReturn(productResponse);

            ProductResponse result = productService.findById(productIdStr);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(productIdStr);
        }

        @Test
        @DisplayName("должен выбросить ProductNotFoundException когда продукт не найден")
        void shouldThrowWhenNotFound() {
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findById(productIdStr))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("должен вернуть список с метаданными пагинации")
        void shouldReturnPaginatedList() {
            var page = new PageImpl<>(List.of(productEntity), PageRequest.of(0, 10), 1);
            ListProductResponse expected = new ListProductResponse();
            expected.setProducts(List.of(productResponse));
            expected.setTotalElements(1);
            expected.setTotalPages(1);
            expected.setCurrentPage(0);

            when(productRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);
            when(productMapper.toListResponse(page)).thenReturn(expected);

            ListProductResponse result = productService.findAll(0, 10);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.getProducts()).hasSize(1);
        }

        @Test
        @DisplayName("должен вернуть пустой список")
        void shouldReturnEmptyList() {
            var emptyPage = new PageImpl<ProductEntity>(Collections.emptyList());
            ListProductResponse expected = new ListProductResponse();
            expected.setProducts(Collections.emptyList());
            expected.setTotalElements(0);

            when(productRepository.findAll(PageRequest.of(0, 10))).thenReturn(emptyPage);
            when(productMapper.toListResponse(emptyPage)).thenReturn(expected);

            ListProductResponse result = productService.findAll(0, 10);

            assertThat(result.getProducts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("должен обновить продукт и отправить событие")
        void shouldUpdateAndSendEvent() {
            ProductRequest request = ProductRequest.builder()
                    .name("Updated Product")
                    .price(BigDecimal.valueOf(199.99))
                    .status(ProductStatus.ACTIVE)
                    .sellerId(UUID.randomUUID())
                    .build();

            when(productRepository.findById(productId)).thenReturn(Optional.of(productEntity));
            when(productRepository.save(productEntity)).thenReturn(productEntity);

            productService.updateProduct(productIdStr, request);

            verify(productMapper).updateEntity(request, productEntity);
            verify(productRepository).save(productEntity);
            verify(eventService).sendProductUpdatedEvent(updatedEventCaptor.capture());
            assertThat(updatedEventCaptor.getValue().id()).isEqualTo(productIdStr);
            assertThat(updatedEventCaptor.getValue().name()).isEqualTo("Updated Product");
        }

        @Test
        @DisplayName("должен выбросить ProductNotFoundException если продукт не найден")
        void shouldThrowWhenNotFound() {
            ProductRequest request = ProductRequest.builder()
                    .name("Updated")
                    .price(BigDecimal.ONE)
                    .status(ProductStatus.ACTIVE)
                    .sellerId(UUID.randomUUID())
                    .build();

            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(productIdStr, request))
                    .isInstanceOf(ProductNotFoundException.class);

            verify(eventService, never()).sendProductUpdatedEvent(any());
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("должен удалить продукт и отправить событие")
        void shouldDeleteAndSendEvent() {
            when(productRepository.existsById(productId)).thenReturn(true);
            productService.deleteById(productIdStr);

            verify(productRepository).deleteById(productId);
            verify(eventService).sendProductDeletedEvent(deletedEventCaptor.capture());
            assertThat(deletedEventCaptor.getValue().productId()).isEqualTo(productIdStr);
        }
    }

    @Nested
    @DisplayName("deleteByIds")
    class DeleteByIds {

        @Test
        @DisplayName("должен удалить несколько продуктов и отправить событие")
        void shouldDeleteAllAndSendEvent() {
            UUID id2 = UUID.randomUUID();
            List<String> ids = List.of(productIdStr, id2.toString());
            List<UUID> uuids = List.of(productId, id2);

            ProductEntity productEntity2 = new ProductEntity();
            productEntity2.setId(id2);
            when(productRepository.findAllById(uuids)).thenReturn(List.of(productEntity, productEntity2));

            productService.deleteByIds(ids);

            verify(productRepository).deleteAllByIdInBatch(uuids);
            verify(eventService).sendProductDeletedEvent(deletedEventCaptor.capture());
            assertThat(deletedEventCaptor.getValue().ids()).isEqualTo(ids);
        }
    }

    @Nested
    @DisplayName("findBySellerId")
    class FindBySellerId {

        @Test
        @DisplayName("должен вернуть продукты продавца с пагинацией")
        void shouldReturnSellerProducts() {
            UUID sellerId = UUID.randomUUID();
            String sellerIdStr = sellerId.toString();
            var pageable = PageRequest.of(0, 10);
            var page = new PageImpl<>(List.of(productEntity), pageable, 1);
            ListProductResponse expected = new ListProductResponse();
            expected.setProducts(List.of(productResponse));
            expected.setTotalElements(1);

            when(productRepository.findAllBySellerId(sellerId, pageable)).thenReturn(page);
            when(productMapper.toListResponse(page)).thenReturn(expected);

            ListProductResponse result = productService.findBySellerId(sellerIdStr, 10, 0);

            assertThat(result.getProducts()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findByCategoryId")
    class FindByCategoryId {

        @Test
        @DisplayName("должен вернуть продукты по категории с пагинацией")
        void shouldReturnProductsByCategory() {
            UUID categoryId = UUID.randomUUID();
            String categoryIdStr = categoryId.toString();
            var pageable = PageRequest.of(0, 5);
            var page = new PageImpl<>(List.of(productEntity), pageable, 1);
            ListProductResponse expected = new ListProductResponse();
            expected.setProducts(List.of(productResponse));

            when(productRepository.findAllByCategoryId(categoryId, pageable)).thenReturn(page);
            when(productMapper.toListResponse(page)).thenReturn(expected);

            ListProductResponse result = productService.findByCategoryId(categoryIdStr, 5, 0);
            assertThat(result.getProducts()).hasSize(1);
        }
    }
}
