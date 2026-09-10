package com.membershipservice.model.valueObject

import com.membershipservice.model.common.Identifier
import jakarta.persistence.Embeddable
import java.util.UUID

@Embeddable
open class MemberId(value: String) : Identifier<MemberId>(value, MemberId::class.java) {
    constructor() : this(UUID.randomUUID().toString())
}
