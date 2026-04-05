package dn.productservice.controller;

import dn.productservice.dto.category.CategoryRequest;
import dn.productservice.dto.category.CategoryResponse;
import dn.productservice.dto.category.ListCategoryResponse;
import dn.productservice.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createCategory(@Valid @RequestBody CategoryRequest request) {
        categoryService.createCategory(request);
    }

    @GetMapping(BY_ID)
    public CategoryResponse getCategoryById(@PathVariable String id) {
        return categoryService.getCategoryById(id);
    }

    @GetMapping(LIST)
    public ListCategoryResponse getAllCategories(
            @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return categoryService.findAll(page, size);
    }

    @PutMapping(BY_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCategory(@PathVariable String id,
                               @Valid @RequestBody CategoryRequest request) {
        categoryService.updateCategory(id, request);
    }

    @DeleteMapping(BY_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategories(@RequestParam List<String> ids) {
        categoryService.deleteCategory(ids);
    }
}
