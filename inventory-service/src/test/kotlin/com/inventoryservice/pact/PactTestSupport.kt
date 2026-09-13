package com.inventoryservice.pact

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import feign.Feign
import feign.codec.Decoder
import org.springframework.cloud.openfeign.support.SpringMvcContract

internal fun pactObjectMapper(): ObjectMapper = ObjectMapper()
    .registerKotlinModule()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

internal fun <T> pactFeignClient(type: Class<T>, baseUrl: String): T {
    val objectMapper = pactObjectMapper()
    val decoder = Decoder { response, targetType ->
        response.body().asInputStream().use { body ->
            objectMapper.readValue(body, objectMapper.typeFactory.constructType(targetType))
        }
    }

    return Feign.builder()
        .contract(SpringMvcContract())
        .decoder(decoder)
        .target(type, baseUrl)
}
