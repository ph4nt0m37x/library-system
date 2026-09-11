package com.inventoryservice.repository

import com.inventoryservice.model.aggregate.Library
import com.inventoryservice.model.aggregate.Transfer
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.TransferId
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.axonframework.common.jpa.SimpleEntityManagerProvider
import org.axonframework.eventhandling.EventBus
import org.axonframework.messaging.annotation.ParameterResolverFactory
import org.axonframework.modelling.command.GenericJpaRepository
import org.axonframework.modelling.command.Repository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration("inventoryRepository")
class AxonRepository(
    @PersistenceContext val entityManager: EntityManager
) {

    @Bean("axonLibraryRepository")
    fun libraryGenericJpaRepository(
        eventBus: EventBus,
        parameterResolverFactory: ParameterResolverFactory
    ): Repository<Library> {

        return GenericJpaRepository.builder(Library::class.java)
            .entityManagerProvider(
                SimpleEntityManagerProvider(entityManager)
            )
            .parameterResolverFactory(parameterResolverFactory)
            .eventBus(eventBus)
            .identifierConverter { LibraryId(it) }
            .build()
    }

    @Bean("axonTransferRepository")
    fun transferGenericJpaRepository(
        eventBus: EventBus,
        parameterResolverFactory: ParameterResolverFactory
    ): Repository<Transfer> {

        return GenericJpaRepository.builder(Transfer::class.java)
            .entityManagerProvider(
                SimpleEntityManagerProvider(entityManager)
            )
            .parameterResolverFactory(parameterResolverFactory)
            .eventBus(eventBus)
            .identifierConverter { TransferId(it) }
            .build()
    }
}