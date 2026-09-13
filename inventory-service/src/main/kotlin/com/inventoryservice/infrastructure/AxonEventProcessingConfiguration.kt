package com.inventoryservice.infrastructure

import org.axonframework.config.EventProcessingConfigurer
import org.axonframework.eventhandling.PropagatingErrorHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

@Configuration
class AxonEventProcessingConfiguration {

    @Autowired
    fun configure(eventProcessingConfigurer: EventProcessingConfigurer) {
        eventProcessingConfigurer
            .registerSubscribingEventProcessor("transfer-stock")
            .registerListenerInvocationErrorHandler("transfer-stock") {
                PropagatingErrorHandler.instance()
            }
            .registerErrorHandler("transfer-stock") {
                PropagatingErrorHandler.instance()
            }
    }
}
