package com.catalogservice.repository

import com.catalogservice.model.aggregate.Book
import com.catalogservice.model.valueObject.BookId
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.axonframework.common.jpa.SimpleEntityManagerProvider
import org.axonframework.eventhandling.EventBus
import org.axonframework.messaging.annotation.ParameterResolverFactory
import org.axonframework.modelling.command.GenericJpaRepository
import org.axonframework.modelling.command.Repository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration("catalogRepository")
class AxonRepository(
    @PersistenceContext val entityManager: EntityManager
) {

    @Bean("axonBookRepository")
    fun bookGenericJpaRepository(
        eventBus: EventBus,
        parameterResolverFactory: ParameterResolverFactory
    ): Repository<Book> {

        return GenericJpaRepository.builder(Book::class.java)
            .entityManagerProvider(
                SimpleEntityManagerProvider(entityManager)
            )
            .parameterResolverFactory(parameterResolverFactory)
            .eventBus(eventBus)
            .identifierConverter { BookId(it) }
            .build()
    }
}

