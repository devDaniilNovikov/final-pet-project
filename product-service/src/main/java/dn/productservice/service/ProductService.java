package dn.productservice.service;


import dn.productservice.dto.product.ListProductResponse;
import dn.productservice.dto.product.ProductRequest;
import dn.productservice.dto.product.ProductResponse;
import dn.productservice.entity.ProductEntity;
import dn.productservice.event.ProductCreateEvent;
import dn.productservice.event.ProductDeletedEvent;
import dn.productservice.event.ProductUpdatedEvent;
import dn.productservice.exception.ProductNotFoundException;
import dn.productservice.mapper.IdConverter;
import dn.productservice.mapper.ProductImageMapper;
import dn.productservice.mapper.ProductMapper;
import dn.productservice.repository.CategoryRepository;
import dn.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {


    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final EventService eventService;
    private final IdConverter idMapper;




    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        var productEntity = productMapper.toEntity(productRequest);
        if (productRequest.getCategoryId() != null) {
            categoryRepository.findById(productRequest.getCategoryId())
                    .ifPresent(productEntity::setCategory);
        }
        if (productRequest.getImages() != null) {
            var images = productRequest.getImages()
                    .stream()
                    .map(productImageMapper::toEntity)
                    .peek(img -> img.setProduct(productEntity))
                    .toList();
            productEntity.setProductImageEntities(new HashSet<>(images));
        }

        productRepository.save(productEntity);
        eventService.sendProductCreatedEvent(ProductCreateEvent.builder()
                        .id(productEntity.getId().toString())
                        .name(productRequest.getName())
                        .build());
        return productMapper.toResponse(productEntity);
    }



    @Transactional
    public void updateProduct(String productId,
                              ProductRequest productRequest) {
        productRepository.findById(idMapper.fromString(productId))
                .ifPresentOrElse(product -> {
            productMapper.updateEntity(productRequest, product);
            productRepository.save(product);
            eventService.sendProductUpdatedEvent(ProductUpdatedEvent
                            .builder()
                            .id(productId)
                            .name(productRequest.getName())
                            .updatedTime(Instant.now())
                            .build());
        },()->{ throw new ProductNotFoundException(MessageFormat.format(
                            "Product with id={0} not found", idMapper.fromString(productId)
                    ));
                });
    }

    public ProductResponse findById(String productId) {
        return productMapper.toResponse(productRepository.findById(idMapper.fromString(productId))
                .orElseThrow(ProductNotFoundException::new));
    }

    public ListProductResponse findAll(int page, int size) {
         var pageable = PageRequest.of(page, size);
         return productMapper.toListResponse(productRepository.findAll(pageable));
    }

    @Transactional
    public void deleteById(String productId) {
        productRepository.deleteById(idMapper.fromString(productId));
        eventService.sendProductDeletedEvent(ProductDeletedEvent.builder()
                        .productId(productId)
                        .build());
    }

    @Transactional
    public void deleteByIds(List<String> productIds) {
        productRepository.deleteAllByIdInBatch(idMapper.fromStringList(productIds));
        eventService.sendProductDeletedEvent(ProductDeletedEvent.builder()
                .ids(productIds)
                .build());
    }

    @Transactional(readOnly = true)
    public ListProductResponse findBySellerId(String sellerId,
                                          int pageSize,
                                          int pageNumber) {
        var pageable = PageRequest.of(pageNumber, pageSize);
        var productList = productRepository.findAllBySellerId(
                idMapper.fromString(sellerId), pageable);
        if (productList.isEmpty()){
            return ListProductResponse.builder()
                    .products(Collections.emptyList())
                    .build();
        }
        return productMapper.toListResponse(productList);
    }

    @Transactional(readOnly = true)
    public ListProductResponse findByCategoryId(String categoryId,
                                                int pageSize,
                                                int pageNumber) {
        var pageable = PageRequest.of(pageNumber, pageSize);
        var productList = productRepository.findAllByCategoryId(
                idMapper.fromString(categoryId), pageable);
        if (productList.isEmpty()){
            log.error("Product list is empty!");
            return ListProductResponse.builder()
                    .products(Collections.emptyList())
                    .totalPages(pageSize)
                    .currentPage(pageNumber)
                    .build();
        }
        return productMapper.toListResponse(productList);
    }
}
