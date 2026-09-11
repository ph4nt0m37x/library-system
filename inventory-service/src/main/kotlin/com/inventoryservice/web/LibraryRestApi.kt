package com.inventoryservice.web

import com.inventoryservice.model.command.library.CreateLibraryCommand
import com.inventoryservice.model.command.library.DeleteLibraryCommand
import com.inventoryservice.model.command.library.UpdateLibraryCommand
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.dto.CreateLibraryDTO
import com.inventoryservice.model.valueObject.dto.DeleteLibraryDTO
import com.inventoryservice.model.valueObject.dto.UpdateLibraryDTO
import com.inventoryservice.model.view.LibraryView
import com.inventoryservice.service.LibraryService
import com.inventoryservice.service.LibraryViewReadService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/libraries")
class LibraryRestApi(
    private val libraryService: LibraryService,
    private val libraryViewReadService: LibraryViewReadService
) {

    @Operation(
        summary = "Get all libraries",
        description = "Get all libraries."
    )
    @GetMapping("/all")
    fun findAllLibraries(): ResponseEntity<List<*>> =
        ResponseEntity.ok(
            libraryViewReadService.findAll()
        )

    @Operation(
        summary = "Get library by ID",
        description = "Get library by {id: String}."
    )
    @GetMapping("/{id}")
    fun findLibraryById(
        @PathVariable id: String
    ): ResponseEntity<Any> {

        val library = libraryViewReadService.findById(
            LibraryId(id)
        )

        return if (library != null) {
            ResponseEntity.ok(library)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @Operation(
        summary = "Create library",
        description = "Create a new library."
    )
    @PostMapping("/create")
    fun createLibrary(
        @RequestBody commandDto: CreateLibraryDTO
    ): ResponseEntity<Any> =
        ResponseEntity.ok(
            libraryService.createLibrary(
                CreateLibraryCommand(
                    name = commandDto.name,
                    address = commandDto.address
                )
            )
        )

    @Operation(
        summary = "Update library",
        description = "Update an existing library."
    )
    @PutMapping("/update")
    fun updateLibrary(
        @RequestBody commandDto: UpdateLibraryDTO
    ): ResponseEntity<Any> =
        ResponseEntity.ok(
            libraryService.updateLibrary(
                UpdateLibraryCommand(
                    id = LibraryId(commandDto.id),
                    name = commandDto.name,
                    address = commandDto.address
                )
            )
        )

    @Operation(
        summary = "Delete library",
        description = "Delete an existing library."
    )
    @DeleteMapping("/delete")
    fun deleteLibrary(
        @RequestBody commandDto: DeleteLibraryDTO
    ): ResponseEntity<Any> =
        ResponseEntity.ok(
            libraryService.deleteLibrary(
                DeleteLibraryCommand(
                    id = LibraryId(commandDto.id)
                )
            )
        )
}