package com.borrowingservice.model.valueObject

import com.borrowingservice.model.valueObject.enums.LoanRejectionReason

class LoanEligibilityException(
    val reason: LoanRejectionReason
) : IllegalStateException(reason.name)
