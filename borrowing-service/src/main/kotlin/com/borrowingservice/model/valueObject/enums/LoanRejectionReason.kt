package com.borrowingservice.model.valueObject.enums

enum class LoanRejectionReason {
    MEMBERSHIP_INACTIVE,
    ACTIVE_LOAN_LIMIT_REACHED,
    TOO_MANY_UNPAID_FEES,
    MEMBER_TEMPORARILY_BANNED,
    MEMBER_PERMANENTLY_BANNED
}
