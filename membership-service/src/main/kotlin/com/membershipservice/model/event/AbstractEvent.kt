package com.membershipservice.model.event

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.membershipservice.model.common.Identifier

abstract class AbstractEvent(open val identifier: Identifier<out Any>) {
    @JsonProperty("_eventType")
    fun eventType(): String = javaClass.simpleName

    @JsonIgnore
    fun eventTopic(): String = javaClass.simpleName
        .removeSuffix("Event")
        .replace(Regex("([a-z])([A-Z])"), "$1.$2")
        .lowercase()
}
