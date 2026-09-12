package com.borrowingservice.integration

import org.axonframework.config.EventProcessingConfigurer
import org.axonframework.eventhandling.PropagatingErrorHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

@Configuration
class BorrowingEventProcessingConfiguration {
    @Autowired
    fun configure(configurer: EventProcessingConfigurer) {
        configurer
            .registerSubscribingEventProcessor("borrowing-integration-outbox")
            .registerListenerInvocationErrorHandler("borrowing-integration-outbox") {
                PropagatingErrorHandler.instance()
            }
            .registerErrorHandler("borrowing-integration-outbox") {
                PropagatingErrorHandler.instance()
            }
    }
}
