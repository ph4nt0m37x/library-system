package com.catalogservice.service.impl

import com.catalogservice.model.valueObject.BookId
import com.catalogservice.model.view.BookView
import com.catalogservice.repository.BookViewRepository
import com.catalogservice.service.BookViewReadService
import org.springframework.stereotype.Service

@Service
class BookViewReadServiceImpl(
    val bookViewRepository: BookViewRepository
) : BookViewReadService {

    override fun findById(id: BookId): BookView? {
        return bookViewRepository.findById(id).orElse(null)
    }

    override fun findAll(): List<BookView> {
        return bookViewRepository.findAll()
    }
    override fun searchByTitle(title: String): List<BookView> {
        return bookViewRepository.findByTitleContainingIgnoreCase(title)
    }

    override fun searchByAuthor(author: String): List<BookView> {
        return bookViewRepository.findByAuthorContainingIgnoreCase(author)
    }

    override fun filterByCategory(categoryId: Long): List<BookView> {
        return bookViewRepository.findByCategory_Id(categoryId)
    }
}

