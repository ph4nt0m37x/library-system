package com.catalogservice.web

import com.catalogservice.model.command.CreateBookCommand
import com.catalogservice.model.command.DeleteBookCommand
import com.catalogservice.model.command.UpdateBookCommand
import com.catalogservice.model.valueObject.BookId
import com.catalogservice.model.valueObject.dto.CreateBookDTO
import com.catalogservice.model.valueObject.dto.DeleteBookDTO
import com.catalogservice.model.valueObject.dto.UpdateBookDTO
import com.catalogservice.service.BookService
import com.catalogservice.service.BookViewReadService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/books")
class BookRestApi(
    private val bookService: BookService,
    private val bookViewReadService: BookViewReadService
) {

    @Operation(summary = "Get all books", description = "Get all books.")
    @GetMapping("/all")
    fun findAllBooks(): ResponseEntity<List<*>> =
        ResponseEntity.ok(bookViewReadService.findAll())

    @Operation(summary = "Get book by ID", description = "Get book by {id: String}.")
    @GetMapping("/{id}")
    fun findBookById(
        @PathVariable id: String
    ): ResponseEntity<Any> {
        val book = bookViewReadService.findById(BookId(id))

        return if (book != null) {
            ResponseEntity.ok(book)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @Operation(summary = "Create book", description = "Create a new book.")
    @PostMapping("/create")
    fun createBook(
        @RequestBody commandDto: CreateBookDTO
    ): ResponseEntity<Any> =
        ResponseEntity.ok(
            bookService.createBook(
                CreateBookCommand(
                    isbn = commandDto.isbn,
                    title = commandDto.title,
                    author = commandDto.author,
                    description = commandDto.description,
                    publicationYear = commandDto.publicationYear,
                    category = commandDto.category
                )
            )
        )

    @Operation(summary = "Update book", description = "Update an existing book.")
    @PutMapping("/update")
    fun updateBook(
        @RequestBody commandDto: UpdateBookDTO
    ): ResponseEntity<Any> =
        ResponseEntity.ok(
            bookService.updateBook(
                UpdateBookCommand(
                    id = BookId(commandDto.id),
                    isbn = commandDto.isbn,
                    title = commandDto.title,
                    author = commandDto.author,
                    description = commandDto.description,
                    publicationYear = commandDto.publicationYear,
                    category = commandDto.category
                )
            )
        )

    @Operation(summary = "Delete book", description = "Delete an existing book.")
    @DeleteMapping("/delete")
    fun deleteBook(
        @RequestBody commandDto: DeleteBookDTO
    ): ResponseEntity<Any> =
        ResponseEntity.ok(
            bookService.deleteBook(
                DeleteBookCommand(
                    id = BookId(commandDto.id)
                )
            )
        )
}

