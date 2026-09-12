package com.borrowingservice.client

class MembershipMemberNotFoundException : RuntimeException("Member was not found in Membership service")

class MembershipServiceUnavailableException(cause: Throwable? = null) :
    RuntimeException("Membership service is unavailable", cause)
