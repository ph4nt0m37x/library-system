package com.catalogservice.service

import com.catalogservice.model.entity.BookCategory

interface BookCategoryService {

    fun createCategory(name: String): BookCategory

    fun updateCategory(id: Long, name: String): BookCategory?

    fun deleteCategory(id: Long): Boolean

    fun findAllCategories(): List<BookCategory>
}