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
}

