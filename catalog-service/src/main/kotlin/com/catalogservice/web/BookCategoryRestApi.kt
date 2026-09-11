package com.catalogservice.web

import com.catalogservice.model.entity.BookCategory
import com.catalogservice.model.valueObject.dto.CreateCategoryDTO
import com.catalogservice.model.valueObject.dto.UpdateCategoryDTO
import com.catalogservice.service.BookCategoryService
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/categories")
class BookCategoryRestApi(
    private val bookCategoryService: BookCategoryService
) {

    @PostMapping("/create")
    fun createCategory(
        @RequestBody dto: CreateCategoryDTO
    ): ResponseEntity<Any> {
        if (dto.name.isBlank()) {
            return problem(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY_NAME", "Category name must not be blank.")
        }

        if (bookCategoryService.existsByName(dto.name)) {
            return problem(HttpStatus.CONFLICT, "DUPLICATE_CATEGORY", "A category named '${dto.name}' already exists.")
        }

        return ResponseEntity.ok(
            bookCategoryService.createCategory(dto.name)
        )
    }

    @PutMapping("/update/{id}")
    fun updateCategory(
        @PathVariable id: Long,
        @RequestBody dto: UpdateCategoryDTO
    ): ResponseEntity<Any> {
        if (id <= 0) {
            return problem(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY_ID", "Category ID must be greater than zero.")
        }

        if (dto.name.isBlank()) {
            return problem(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY_NAME", "Category name must not be blank.")
        }

        if (!bookCategoryService.existsById(id)) {
            return problem(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Category with ID '$id' was not found.")
        }

        if (bookCategoryService.existsByNameAndIdNot(dto.name, id)) {
            return problem(HttpStatus.CONFLICT, "DUPLICATE_CATEGORY", "A category named '${dto.name}' already exists.")
        }

        val category = bookCategoryService.updateCategory(id, dto.name)

        return if (category != null) {
            ResponseEntity.ok(category)
        } else {
            problem(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Category with ID '$id' was not found.")
        }
    }

    @DeleteMapping("/delete/{id}")
    fun deleteCategory(
        @PathVariable id: Long
    ): ResponseEntity<Any> {
        if (id <= 0) {
            return problem(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY_ID", "Category ID must be greater than zero.")
        }

        if (!bookCategoryService.existsById(id)) {
            return problem(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Category with ID '$id' was not found.")
        }

        if (bookCategoryService.isCategoryInUse(id)) {
            return problem(
                HttpStatus.CONFLICT,
                "CATEGORY_IN_USE",
                "Category with ID '$id' cannot be deleted because it is referenced by one or more books."
            )
        }

        val deleted = bookCategoryService.deleteCategory(id)

        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            problem(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Category with ID '$id' was not found.")
        }
    }

    @GetMapping("/all")
    fun findAllCategories(): ResponseEntity<List<BookCategory>> {
        return ResponseEntity.ok(
            bookCategoryService.findAllCategories()
        )
    }

    private fun problem(status: HttpStatus, code: String, detail: String): ResponseEntity<Any> {
        val problem = ProblemDetail.forStatusAndDetail(status, detail)
        problem.setProperty("code", code)
        return ResponseEntity.status(status).body(problem)
    }
}
