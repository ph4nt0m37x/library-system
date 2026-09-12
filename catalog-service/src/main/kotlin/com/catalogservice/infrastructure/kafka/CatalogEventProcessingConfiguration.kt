package com.catalogservice.infrastructure.kafka

import org.axonframework.config.EventProcessingConfigurer
import org.axonframework.eventhandling.PropagatingErrorHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

@Configuration
class CatalogEventProcessingConfiguration {
    @Autowired
    fun configure(configurer: EventProcessingConfigurer) {
        configurer
            .registerSubscribingEventProcessor("catalog-integration-outbox")
            .registerListenerInvocationErrorHandler("catalog-integration-outbox") {
                PropagatingErrorHandler.instance()
            }
            .registerErrorHandler("catalog-integration-outbox") {
                PropagatingErrorHandler.instance()
            }
    }
}
