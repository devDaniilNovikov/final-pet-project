package dn.productservice.service;

import dn.productservice.dto.category.CategoryRequest;
import dn.productservice.dto.category.CategoryResponse;
import dn.productservice.dto.category.ListCategoryResponse;
import dn.productservice.entity.CategoryEntity;
import dn.productservice.exception.CategoryNotFoundException;
import dn.productservice.mapper.CategoryMapper;
import dn.productservice.utils.IdConverter;
import dn.productservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;



    @Transactional
    public void createCategory(CategoryRequest categoryRequest) {
        var categoryEntity = categoryMapper.toEntity(categoryRequest);
        categoryRepository.save(categoryEntity);
    }

    @Transactional
    public void updateCategory(String id,
                               CategoryRequest categoryRequest) {
        CategoryEntity categoryForUpdate = categoryRepository.findById(
                        IdConverter.fromString(id))
                .orElseThrow(CategoryNotFoundException::new);
        categoryMapper.updateEntity(categoryRequest,categoryForUpdate);
        categoryRepository.save(categoryForUpdate);

    }

    @Transactional
    public void deleteCategory(String id) {
        categoryRepository.findById(IdConverter.fromString(id))
                .stream()
                .map(CategoryEntity::getId)
                .forEach(categoryRepository::deleteById);
    }

    @Transactional
    public void deleteCategory(List<String> id){
        List<UUID> uuids = id.stream()
                .map(IdConverter::fromString)
                .toList();
        categoryRepository.deleteAllByIdInBatch(uuids);
    }


    public CategoryResponse getCategoryById(String id) {
        return categoryMapper.toResponse(
                categoryRepository.findById(IdConverter.fromString(id))
                .orElseThrow(CategoryNotFoundException::new));
    }

    @Transactional(readOnly = true)
    public ListCategoryResponse findAll(int page, int size) {
        PageRequest categoryPages = PageRequest.of(page, size);
        Page<CategoryEntity> categoryList = categoryRepository.findAll(categoryPages);
        if (categoryList.isEmpty()){
            log.error("Category list is empty!");
            return ListCategoryResponse.builder()
                    .categories(Collections.emptyList())
                    .totalPages(0)
                    .currentPage(page)
                    .build();
        }
        return categoryMapper.toListResponse(categoryList);
    }
}
