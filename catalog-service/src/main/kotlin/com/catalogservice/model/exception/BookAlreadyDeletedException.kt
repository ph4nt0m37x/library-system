package com.catalogservice.model.exception

import com.catalogservice.model.valueObject.BookId

class BookAlreadyDeletedException(id: BookId) :
    IllegalStateException("Book with ID '${id.value}' has already been deleted.")
