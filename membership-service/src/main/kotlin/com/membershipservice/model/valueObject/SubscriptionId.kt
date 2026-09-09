package com.membershipservice.model.valueObject

import com.membershipservice.model.common.Identifier
import jakarta.persistence.Embeddable
import java.util.UUID

@Embeddable
open class SubscriptionId(value: String) : Identifier<SubscriptionId>(value, SubscriptionId::class.java) {
    constructor() : this(UUID.randomUUID().toString())
}
