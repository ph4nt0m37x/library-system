package com.borrowingservice.integration

import org.springframework.data.jpa.repository.JpaRepository

interface IntegrationOutboxRepository : JpaRepository<IntegrationOutboxEvent, String> {
    fun findTop50ByPublishedAtIsNullAndDeadLetteredAtIsNullOrderByCreatedAtAsc(): List<IntegrationOutboxEvent>
    fun countByPublishedAtIsNullAndDeadLetteredAtIsNull(): Long
    fun countByDeadLetteredAtIsNotNull(): Long
}
