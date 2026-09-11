package com.catalogservice.web

import com.catalogservice.model.command.CreateBookCommand
import com.catalogservice.model.command.DeleteBookCommand
import com.catalogservice.model.command.UpdateBookCommand
import com.catalogservice.model.valueObject.BookId
import com.catalogservice.model.valueObject.Money
import com.catalogservice.model.valueObject.dto.CreateBookDTO
import com.catalogservice.model.valueObject.dto.DeleteBookDTO
import com.catalogservice.model.valueObject.dto.UpdateBookDTO
import com.catalogservice.model.view.BookView
import com.catalogservice.service.BookCategoryService
import com.catalogservice.service.BookService
import com.catalogservice.service.BookViewReadService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/books")
class BookRestApi(
    private val bookService: BookService,
    private val bookViewReadService: BookViewReadService,
    private val bookCategoryService: BookCategoryService
) {

    @Operation(summary = "Get all books", description = "Get all books.")
    @GetMapping("/all")
    fun findAllBooks(): ResponseEntity<List<*>> =
        ResponseEntity.ok(bookViewReadService.findAll())

    @Operation(summary = "Get all available books", description = "Get all books that have not been deleted.")
    @GetMapping("/available")
    fun findAllAvailableBooks(): ResponseEntity<List<BookView>> =
        ResponseEntity.ok(bookViewReadService.findAllAvailable())

    @Operation(summary = "Get book by ID", description = "Get book by {id: String}.")
    @GetMapping("/{id}")
    fun findBookById(
        @PathVariable id: String
    ): ResponseEntity<Any> {
        val book = bookViewReadService.findById(BookId(id))

        return if (book != null) {
            ResponseEntity.ok(book)
        } else {
            problem(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Book with ID '$id' was not found.")
        }
    }

    @Operation(summary = "Create book", description = "Create a new book.")
    @PostMapping("/create")
    fun createBook(
        @RequestBody commandDto: CreateBookDTO
    ): ResponseEntity<Any> {
        validateBookFields(commandDto.isbn, commandDto.title, commandDto.author, commandDto.price)?.let {
            return it
        }

        val category = commandDto.categoryId?.let { categoryId ->
            if (categoryId <= 0) {
                return problem(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY", "Category ID must be greater than zero.")
            }
            bookCategoryService.findById(categoryId)
                ?: return problem(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Category with ID '$categoryId' was not found.")
        }

        if (bookViewReadService.existsByIsbn(commandDto.isbn)) {
            return problem(HttpStatus.CONFLICT, "DUPLICATE_ISBN", "A book with ISBN '${commandDto.isbn}' already exists.")
        }

        return ResponseEntity.ok(
            bookService.createBook(
                CreateBookCommand(
                    isbn = commandDto.isbn,
                    title = commandDto.title,
                    author = commandDto.author,
                    description = commandDto.description,
                    publicationYear = commandDto.publicationYear,
                    price = Money(commandDto.price),
                    category = category
                )
            )
        )
    }

    @Operation(summary = "Update book", description = "Update an existing book.")
    @PutMapping("/update")
    fun updateBook(
        @RequestBody commandDto: UpdateBookDTO
    ): ResponseEntity<Any> {
        validateBookFields(commandDto.isbn, commandDto.title, commandDto.author, commandDto.price)?.let {
            return it
        }

        if (commandDto.id.isBlank()) {
            return problem(HttpStatus.BAD_REQUEST, "INVALID_BOOK_ID", "Book ID must not be blank.")
        }

        val bookId = BookId(commandDto.id)
        if (bookViewReadService.findById(bookId) == null) {
            return problem(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Book with ID '${commandDto.id}' was not found.")
        }

        val category = commandDto.categoryId?.let { categoryId ->
            if (categoryId <= 0) {
                return problem(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY", "Category ID must be greater than zero.")
            }
            bookCategoryService.findById(categoryId)
                ?: return problem(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Category with ID '$categoryId' was not found.")
        }

        if (bookViewReadService.existsByIsbnAndIdNot(commandDto.isbn, bookId)) {
            return problem(HttpStatus.CONFLICT, "DUPLICATE_ISBN", "A book with ISBN '${commandDto.isbn}' already exists.")
        }

        return ResponseEntity.ok(
            bookService.updateBook(
                UpdateBookCommand(
                    id = bookId,
                    isbn = commandDto.isbn,
                    title = commandDto.title,
                    author = commandDto.author,
                    description = commandDto.description,
                    publicationYear = commandDto.publicationYear,
                    price = Money(commandDto.price),
                    category = category
                )
            )
        )
    }

    @Operation(summary = "Delete book", description = "Delete an existing book.")
    @DeleteMapping("/delete")
    fun deleteBook(
        @RequestBody commandDto: DeleteBookDTO
    ): ResponseEntity<Any> {
        if (commandDto.id.isBlank()) {
            return problem(HttpStatus.BAD_REQUEST, "INVALID_BOOK_ID", "Book ID must not be blank.")
        }

        val bookId = BookId(commandDto.id)
        return ResponseEntity.ok(
            bookService.deleteBook(
                DeleteBookCommand(
                    id = bookId
                )
            ).join()
        )
    }

    @Operation(summary = "Search books by title", description = "Search books whose title contains the given text.")
    @GetMapping("/search/title")
    fun searchByTitle(
        @RequestParam title: String
    ): ResponseEntity<List<BookView>> =
        ResponseEntity.ok(
            bookViewReadService.searchByTitle(title)
        )


    @Operation(summary = "Search books by author", description = "Search books whose author contains the given text.")
    @GetMapping("/search/author")
    fun searchByAuthor(
        @RequestParam author: String
    ): ResponseEntity<List<BookView>> =
        ResponseEntity.ok(
            bookViewReadService.searchByAuthor(author)
        )


    @Operation(summary = "Filter books by category", description = "Get books belonging to a specific category.")
    @GetMapping("/filter/category")
    fun filterByCategory(
        @RequestParam categoryId: Long
    ): ResponseEntity<List<BookView>> =
        ResponseEntity.ok(
            bookViewReadService.filterByCategory(categoryId)
        )

    @GetMapping("/{bookId}/price")
    fun getBookPrice(
        @PathVariable bookId: String
    ): ResponseEntity<Any> {
        val price = bookViewReadService.getBookPrice(bookId)

        return if (price != null) {
            ResponseEntity.ok(price)
        } else {
            problem(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Book with ID '$bookId' was not found.")
        }
    }

    private fun validateBookFields(
        isbn: String,
        title: String,
        author: String,
        price: BigDecimal
    ): ResponseEntity<Any>? {
        if (!isbn.matches(Regex("[0-9]{13}"))) {
            return problem(HttpStatus.BAD_REQUEST, "INVALID_ISBN", "ISBN must contain exactly 13 digits.")
        }
        if (title.isBlank()) {
            return problem(HttpStatus.BAD_REQUEST, "INVALID_TITLE", "Book title must not be blank.")
        }
        if (author.isBlank()) {
            return problem(HttpStatus.BAD_REQUEST, "INVALID_AUTHOR", "Book author must not be blank.")
        }
        if (price <= BigDecimal.ZERO) {
            return problem(HttpStatus.BAD_REQUEST, "INVALID_PRICE", "Book price must be greater than zero.")
        }

        return null
    }

    private fun problem(status: HttpStatus, code: String, detail: String): ResponseEntity<Any> {
        val problem = ProblemDetail.forStatusAndDetail(status, detail)
        problem.setProperty("code", code)
        return ResponseEntity.status(status).body(problem)
    }
}

