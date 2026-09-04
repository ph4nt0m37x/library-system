package com.catalogservice.model.event

import com.catalogservice.model.common.Identifier
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

abstract class AbstractEvent(open val identifier: Identifier<out Any>) {

    @JsonProperty("_eventType")
    fun eventType(): String = this.javaClass.simpleName

    @JsonIgnore
    fun eventTopic(): String =
        this.javaClass.simpleName
            .removeSuffix("Event")
            .replace(Regex("([a-z])([A-Z])"), "$1.$2")
            .lowercase()

    @JsonIgnore
    open fun toExternalEvent(): Any? = null
}