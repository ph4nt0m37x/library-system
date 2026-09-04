package com.catalogservice.model.view

import com.catalogservice.model.common.Identifier
import com.catalogservice.model.common.LabeledEntity
import com.catalogservice.model.entity.BookCategory
import com.catalogservice.model.valueObject.BookId
import jakarta.persistence.*

@Entity
@Table(name = "book")
data class BookView(

    @EmbeddedId
    @AttributeOverride(name = "value", column = Column(name = "id"))
    val id: BookId,

    val isbn: String,

    val title: String,

    val author: String,

    val description: String?,

    val publicationYear: String?,

    @ManyToOne
    @JoinColumn(name = "category_id")
    val category: BookCategory?,

    val deleted: Boolean = false

) : LabeledEntity {

    override fun getId(): Identifier<out Any> = id

    override fun getLabel(): String {
        return "Book $title"
    }
}

