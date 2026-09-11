package com.catalogservice.service

import com.catalogservice.model.entity.BookCategory
import com.catalogservice.repository.BookCategoryRepository
import com.catalogservice.repository.BookViewRepository
import org.springframework.stereotype.Service

@Service
class BookCategoryServiceImpl(
    private val bookCategoryRepository: BookCategoryRepository,
    private val bookViewRepository: BookViewRepository
) : BookCategoryService {

    override fun createCategory(name: String): BookCategory {
        return bookCategoryRepository.save(
            BookCategory(name = name)
        )
    }

    override fun updateCategory(id: Long, name: String): BookCategory? {
        val category = bookCategoryRepository.findById(id)

        if (category.isEmpty) {
            return null
        }

        val existingCategory = category.get()
        existingCategory.name = name

        return bookCategoryRepository.save(existingCategory)
    }

    override fun deleteCategory(id: Long): Boolean {
        if (!bookCategoryRepository.existsById(id)) {
            return false
        }

        bookCategoryRepository.deleteById(id)
        return true
    }

    override fun findAllCategories(): List<BookCategory> {
        return bookCategoryRepository.findAll()
    }

    override fun findById(id: Long): BookCategory? {
        return bookCategoryRepository.findById(id).orElse(null)
    }

    override fun existsById(id: Long): Boolean {
        return bookCategoryRepository.existsById(id)
    }

    override fun existsByName(name: String): Boolean {
        return bookCategoryRepository.existsByNameIgnoreCase(name)
    }

    override fun existsByNameAndIdNot(name: String, id: Long): Boolean {
        return bookCategoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)
    }

    override fun isCategoryInUse(id: Long): Boolean {
        return bookViewRepository.existsByCategory_Id(id)
    }
}
