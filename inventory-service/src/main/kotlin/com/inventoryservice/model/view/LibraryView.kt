package com.inventoryservice.model.view

import com.inventoryservice.model.common.Identifier
import com.inventoryservice.model.common.LabeledEntity
import com.inventoryservice.model.valueObject.LibraryId
import jakarta.persistence.*

@Entity
@Table(name = "library")
data class LibraryView(

    @EmbeddedId
    @AttributeOverride(name = "value", column = Column(name = "id"))
    val id: LibraryId,

    val name: String,

    val address: String,

    val deleted: Boolean = false

) : LabeledEntity {

    override fun getId(): Identifier<out Any> = id

    override fun getLabel(): String {
        return "Library $name"
    }
}