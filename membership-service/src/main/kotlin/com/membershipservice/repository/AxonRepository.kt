package com.membershipservice.repository

import com.membershipservice.model.aggregate.Member
import com.membershipservice.model.valueObject.MemberId
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
    @Bean("axonMemberRepository")
    fun memberGenericJpaRepository(
        eventBus: EventBus,
        parameterResolverFactory: ParameterResolverFactory
    ): Repository<Member> = GenericJpaRepository.builder(Member::class.java)
        .entityManagerProvider(SimpleEntityManagerProvider(entityManager))
        .parameterResolverFactory(parameterResolverFactory)
        .eventBus(eventBus)
        .identifierConverter { MemberId(it) }
        .build()
}
