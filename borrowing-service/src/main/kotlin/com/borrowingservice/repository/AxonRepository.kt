package com.borrowingservice.repository

import com.borrowingservice.model.aggregate.BorrowingBanRecord
import com.borrowingservice.model.aggregate.Fee
import com.borrowingservice.model.aggregate.Loan
import com.borrowingservice.model.aggregate.Payment
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.axonframework.common.jpa.SimpleEntityManagerProvider
import org.axonframework.eventhandling.EventBus
import org.axonframework.messaging.annotation.ParameterResolverFactory
import org.axonframework.modelling.command.GenericJpaRepository
import org.axonframework.modelling.command.Repository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AxonRepository(
    @PersistenceContext private val entityManager: EntityManager
) {
    @Bean("axonLoanRepository")
    fun loanRepository(
        eventBus: EventBus,
        parameterResolverFactory: ParameterResolverFactory
    ): Repository<Loan> = repositoryFor(eventBus, parameterResolverFactory, Loan::class.java)

    @Bean("axonFeeRepository")
    fun feeRepository(
        eventBus: EventBus,
        parameterResolverFactory: ParameterResolverFactory
    ): Repository<Fee> = repositoryFor(eventBus, parameterResolverFactory, Fee::class.java)

    @Bean("axonPaymentRepository")
    fun paymentRepository(
        eventBus: EventBus,
        parameterResolverFactory: ParameterResolverFactory
    ): Repository<Payment> = repositoryFor(eventBus, parameterResolverFactory, Payment::class.java)

    @Bean("axonBorrowingBanRecordRepository")
    fun borrowingBanRecordRepository(
        eventBus: EventBus,
        parameterResolverFactory: ParameterResolverFactory
    ): Repository<BorrowingBanRecord> = repositoryFor(
        eventBus,
        parameterResolverFactory,
        BorrowingBanRecord::class.java
    )

    private fun <T : Any> repositoryFor(
        eventBus: EventBus,
        parameterResolverFactory: ParameterResolverFactory,
        aggregateType: Class<T>
    ): Repository<T> = GenericJpaRepository.builder(aggregateType)
        .entityManagerProvider(SimpleEntityManagerProvider(entityManager))
        .parameterResolverFactory(parameterResolverFactory)
        .eventBus(eventBus)
        .build()
}
