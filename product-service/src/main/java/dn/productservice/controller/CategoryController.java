package dn.productservice.controller;

import dn.productservice.dto.category.CategoryRequest;
import dn.productservice.dto.category.CategoryResponse;
import dn.productservice.dto.category.ListCategoryResponse;
import dn.productservice.exception.ErrorBody;
import dn.productservice.service.CategoryService;
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

@Tag(name = "Categories", description = "API для управления категориями товаров")
@RestController
@RequestMapping(CategoryController.BASE)
@RequiredArgsConstructor
public class CategoryController {

    static final String BASE = "/api/v1/categories";
    private static final String BY_ID = "/{id}";
    private static final String DEFAULT_PAGE_NUMBER = "0";
    private static final String DEFAULT_PAGE_SIZE   = "10";
    private static final String LIST = "/list";

    private final CategoryService categoryService;

    @Operation(summary = "Создать категорию")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Категория создана"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createCategory(@Valid @RequestBody CategoryRequest request) {
        categoryService.createCategory(request);
    }

    @Operation(summary = "Получить категорию по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Категория найдена",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Категория не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @GetMapping(BY_ID)
    public CategoryResponse getCategoryById(
            @Parameter(description = "UUID категории", required = true) @PathVariable String id) {
        return categoryService.getCategoryById(id);
    }

    @Operation(summary = "Получить все категории", description = "Возвращает постраничный список категорий")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список категорий",
                    content = @Content(schema = @Schema(implementation = ListCategoryResponse.class)))
    })
    @GetMapping(LIST)
    public ListCategoryResponse getAllCategories(
            @Parameter(description = "Номер страницы (с 0)", example = "0") @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) int page,
            @Parameter(description = "Размер страницы", example = "10") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return categoryService.findAll(page, size);
    }

    @Operation(summary = "Обновить категорию")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Категория обновлена"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Категория не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @PutMapping(BY_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCategory(
            @Parameter(description = "UUID категории", required = true) @PathVariable String id,
            @Valid @RequestBody CategoryRequest request) {
        categoryService.updateCategory(id, request);
    }

    @Operation(summary = "Удалить категорию по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Категория удалена"),
            @ApiResponse(responseCode = "404", description = "Категория не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @DeleteMapping(BY_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(
            @Parameter(description = "UUID категории", required = true) @PathVariable String id) {
        categoryService.deleteCategory(id);
    }

    @Operation(summary = "Удалить несколько категорий по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Категории удалены"),
            @ApiResponse(responseCode = "400", description = "Неверный запрос",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategories(
            @Parameter(description = "Список UUID категорий", required = true) @RequestParam List<String> ids) {
        categoryService.deleteCategory(ids);
    }
}
