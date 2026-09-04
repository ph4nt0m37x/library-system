package com.catalogservice.service.impl

import com.catalogservice.model.command.CreateBookCommand
import com.catalogservice.model.command.DeleteBookCommand
import com.catalogservice.model.command.UpdateBookCommand
import com.catalogservice.model.valueObject.BookId
import com.catalogservice.service.BookService
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class BookServiceImpl(
    val commandGateway: CommandGateway
) : BookService {

    override fun createBook(
        command: CreateBookCommand
    ): CompletableFuture<BookId> {
        return commandGateway.send(command)
    }

    override fun updateBook(
        command: UpdateBookCommand
    ): CompletableFuture<BookId> {
        return commandGateway.send(command)
    }

    override fun deleteBook(
        command: DeleteBookCommand
    ): CompletableFuture<BookId> {
        return commandGateway.send(command)
    }
}