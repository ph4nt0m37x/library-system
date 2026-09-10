package com.catalogservice.service

import com.catalogservice.model.command.CreateBookCommand
import com.catalogservice.model.command.DeleteBookCommand
import com.catalogservice.model.command.UpdateBookCommand
import com.catalogservice.model.valueObject.BookId
import java.util.concurrent.CompletableFuture

interface BookService {
    fun createBook(command: CreateBookCommand): CompletableFuture<BookId>
    fun updateBook(command: UpdateBookCommand): CompletableFuture<BookId>
    fun deleteBook(command: DeleteBookCommand): CompletableFuture<BookId>
}