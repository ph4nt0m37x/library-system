package com.catalogservice.web

import com.catalogservice.model.entity.BookCategory
import com.catalogservice.model.valueObject.dto.CreateCategoryDTO
import com.catalogservice.model.valueObject.dto.UpdateCategoryDTO
import com.catalogservice.service.BookCategoryService
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
    ): ResponseEntity<BookCategory> {

        return ResponseEntity.ok(
            bookCategoryService.createCategory(dto.name)
        )
    }

    @PutMapping("/update/{id}")
    fun updateCategory(
        @PathVariable id: Long,
        @RequestBody dto: UpdateCategoryDTO
    ): ResponseEntity<BookCategory> {

        val category = bookCategoryService.updateCategory(id, dto.name)

        return if (category != null) {
            ResponseEntity.ok(category)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/delete/{id}")
    fun deleteCategory(
        @PathVariable id: Long
    ): ResponseEntity<Void> {

        val deleted = bookCategoryService.deleteCategory(id)

        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/all")
    fun findAllCategories(): ResponseEntity<List<BookCategory>> {
        return ResponseEntity.ok(
            bookCategoryService.findAllCategories()
        )
    }
}