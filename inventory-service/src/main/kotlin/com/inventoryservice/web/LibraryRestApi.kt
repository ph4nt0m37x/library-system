package com.inventoryservice.web

import com.inventoryservice.model.command.library.CreateLibraryCommand
import com.inventoryservice.model.command.library.DeleteLibraryCommand
import com.inventoryservice.model.command.library.UpdateLibraryCommand
import com.inventoryservice.model.dto.LibraryCreatedResponse
import com.inventoryservice.model.exception.ResourceNotFoundException
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.dto.CreateLibraryDTO
import com.inventoryservice.model.valueObject.dto.DeleteLibraryDTO
import com.inventoryservice.model.valueObject.dto.UpdateLibraryDTO
import com.inventoryservice.model.view.LibraryView
import com.inventoryservice.service.LibraryService
import com.inventoryservice.service.LibraryViewReadService
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
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
        summary = "Get all available libraries",
        description = "Get all libraries that have not been deleted."
    )
    @GetMapping("/available")
    fun findAllAvailableLibraries(): ResponseEntity<List<LibraryView>> =
        ResponseEntity.ok(
            libraryViewReadService.findAllAvailable()
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

        return ResponseEntity.ok(
            library ?: throw ResourceNotFoundException("Library '$id' does not exist")
        )
    }

    @Operation(
        summary = "Create library",
        description = "Create a new library."
    )
    @PostMapping("/create")
    fun createLibrary(
        @Valid @RequestBody commandDto: CreateLibraryDTO
    ): ResponseEntity<LibraryCreatedResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            libraryService.createLibrary(
                CreateLibraryCommand(
                    name = commandDto.name,
                    address = commandDto.address
                )
            ).join()
        )

    @Operation(
        summary = "Update library",
        description = "Update an existing library."
    )
    @PutMapping("/update")
    fun updateLibrary(
        @Valid @RequestBody commandDto: UpdateLibraryDTO
    ): ResponseEntity<Void> {
        libraryService.updateLibrary(
            UpdateLibraryCommand(
                id = LibraryId(commandDto.id),
                name = commandDto.name,
                address = commandDto.address
            )
        ).join()
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Delete library",
        description = "Delete an existing library."
    )
    @DeleteMapping("/delete")
    fun deleteLibrary(
        @Valid @RequestBody commandDto: DeleteLibraryDTO
    ): ResponseEntity<Void> {
        libraryService.deleteLibrary(
            DeleteLibraryCommand(
                id = LibraryId(commandDto.id)
            )
        ).join()
        return ResponseEntity.noContent().build()
    }
}
