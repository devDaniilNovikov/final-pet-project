package dn.productservice.service;

import dn.productservice.dto.category.CategoryRequest;
import dn.productservice.dto.category.CategoryResponse;
import dn.productservice.dto.category.ListCategoryResponse;
import dn.productservice.entity.CategoryEntity;
import dn.productservice.exception.CategoryNotFoundException;
import dn.productservice.mapper.CategoryMapper;
import dn.productservice.mapper.IdConverter;
import dn.productservice.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryMapper categoryMapper;
    @Mock private IdConverter idMapper;

    @InjectMocks
    private CategoryService categoryService;

    private UUID categoryId;
    private String categoryIdStr;
    private CategoryEntity categoryEntity;
    private CategoryResponse categoryResponse;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        categoryIdStr = categoryId.toString();

        categoryEntity = new CategoryEntity();
        categoryEntity.setId(categoryId);
        categoryEntity.setName("Electronics");

        categoryResponse = CategoryResponse.builder()
                .id(categoryIdStr)
                .name("Electronics")
                .build();

        when(idMapper.fromString(categoryIdStr)).thenReturn(categoryId);
    }

    @Nested
    @DisplayName("createCategory")
    class CreateCategory {

        @Test
        @DisplayName("должен создать категорию без родительской категории")
        void shouldCreateCategoryWithoutParent() {
            CategoryRequest request = new CategoryRequest("Electronics", null);

            when(categoryMapper.toEntity(request)).thenReturn(categoryEntity);

            categoryService.createCategory(request);

            verify(categoryMapper).toEntity(request);
            verify(categoryRepository).save(categoryEntity);
        }

        @Test
        @DisplayName("должен создать категорию с родительской категорией")
        void shouldCreateCategoryWithParent() {
            UUID parentId = UUID.randomUUID();
            CategoryRequest request = new CategoryRequest("Smartphones", parentId);

            CategoryEntity childEntity = new CategoryEntity();
            childEntity.setName("Smartphones");

            when(categoryMapper.toEntity(request)).thenReturn(childEntity);

            categoryService.createCategory(request);

            verify(categoryRepository).save(childEntity);
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategory {

        @Test
        @DisplayName("должен обновить категорию когда она существует")
        void shouldUpdateCategoryWhenExists() {
            CategoryRequest request = new CategoryRequest("Updated Electronics", null);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(categoryEntity));

            categoryService.updateCategory(categoryIdStr, request);

            verify(categoryMapper).updateEntity(request, categoryEntity);
            verify(categoryRepository).save(categoryEntity);
        }

        @Test
        @DisplayName("должен выбросить CategoryNotFoundException если категория не найдена")
        void shouldThrowWhenNotFound() {
            CategoryRequest request = new CategoryRequest("Updated", null);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.updateCategory(categoryIdStr, request))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(categoryRepository, never()).save(any());
            verify(categoryMapper, never()).updateEntity(any(), any());
        }
    }

    @Nested
    @DisplayName("deleteCategory(String)")
    class DeleteCategoryById {

        @Test
        @DisplayName("должен удалить категорию если она существует")
        void shouldDeleteCategoryWhenExists() {
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(categoryEntity));

            categoryService.deleteCategory(categoryIdStr);

            verify(categoryRepository).deleteById(categoryId);
        }

        @Test
        @DisplayName("не должен вызывать deleteById если категория не найдена")
        void shouldNotDeleteWhenNotFound() {
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            categoryService.deleteCategory(categoryIdStr);

            verify(categoryRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("deleteCategory(List<String>)")
    class DeleteCategoriesByIds {

        @Test
        @DisplayName("должен удалить несколько категорий пакетом")
        void shouldDeleteAllInBatch() {
            UUID id2 = UUID.randomUUID();
            List<String> ids = List.of(categoryIdStr, id2.toString());

            when(idMapper.fromString(id2.toString())).thenReturn(id2);

            categoryService.deleteCategory(ids);

            verify(categoryRepository).deleteAllByIdInBatch(List.of(categoryId, id2));
        }

        @Test
        @DisplayName("должен обработать пустой список без ошибок")
        void shouldHandleEmptyList() {
            categoryService.deleteCategory(Collections.emptyList());

            verify(categoryRepository).deleteAllByIdInBatch(Collections.emptyList());
        }
    }

    @Nested
    @DisplayName("getCategoryById")
    class GetCategoryById {

        @Test
        @DisplayName("должен вернуть категорию по id")
        void shouldReturnCategoryWhenExists() {
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(categoryEntity));
            when(categoryMapper.toResponse(categoryEntity)).thenReturn(categoryResponse);

            CategoryResponse result = categoryService.getCategoryById(categoryIdStr);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(categoryIdStr);
            assertThat(result.getName()).isEqualTo("Electronics");
        }

        @Test
        @DisplayName("должен выбросить CategoryNotFoundException если категория не найдена")
        void shouldThrowWhenNotFound() {
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getCategoryById(categoryIdStr))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("должен вернуть список категорий с пагинацией")
        void shouldReturnPaginatedCategories() {
            var pageable = PageRequest.of(0, 10);
            var page = new PageImpl<>(List.of(categoryEntity), pageable, 1);

            ListCategoryResponse expected = new ListCategoryResponse();
            expected.setCategories(List.of(categoryResponse));

            when(categoryRepository.findAll(pageable)).thenReturn(page);
            when(categoryMapper.toListResponse(List.of(categoryEntity))).thenReturn(expected);

            ListCategoryResponse result = categoryService.findAll(0, 10);

            assertThat(result).isNotNull();
            assertThat(result.getCategories()).hasSize(1);
            assertThat(result.getCategories().get(0).getName()).isEqualTo("Electronics");
        }

        @Test
        @DisplayName("должен вернуть пустой список если категорий нет")
        void shouldReturnEmptyList() {
            var pageable = PageRequest.of(0, 10);
            var emptyPage = new PageImpl<CategoryEntity>(Collections.emptyList(), pageable, 0);

            ListCategoryResponse expected = new ListCategoryResponse();
            expected.setCategories(Collections.emptyList());

            when(categoryRepository.findAll(pageable)).thenReturn(emptyPage);
            when(categoryMapper.toListResponse(Collections.emptyList())).thenReturn(expected);

            ListCategoryResponse result = categoryService.findAll(0, 10);

            assertThat(result.getCategories()).isEmpty();
        }
    }
}
