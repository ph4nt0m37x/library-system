package com.catalogservice.service

import com.catalogservice.model.entity.BookCategory

interface BookCategoryService {

    fun createCategory(name: String): BookCategory

    fun updateCategory(id: Long, name: String): BookCategory?

    fun deleteCategory(id: Long): Boolean

    fun findAllCategories(): List<BookCategory>

    fun findById(id: Long): BookCategory?

    fun existsById(id: Long): Boolean

    fun existsByName(name: String): Boolean

    fun existsByNameAndIdNot(name: String, id: Long): Boolean

    fun isCategoryInUse(id: Long): Boolean
}
