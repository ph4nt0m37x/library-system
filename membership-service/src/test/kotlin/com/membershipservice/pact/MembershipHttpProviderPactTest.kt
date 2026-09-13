package com.membershipservice.pact

import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.dto.SubscriptionEligibilityResponse
import com.membershipservice.service.MemberViewReadService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.ZonedDateTime

@Provider("membership-http-provider")
@PactFolder("pacts")
@ActiveProfiles("pact")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MembershipHttpProviderPactTest {
    @LocalServerPort
    private var port: Int = 0

    @MockitoBean
    private lateinit var memberViewReadService: MemberViewReadService

    @BeforeEach
    fun setTarget(context: PactVerificationContext) {
        context.target = HttpTestTarget("localhost", port)
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider::class)
    fun verifyPact(context: PactVerificationContext) {
        context.verifyInteraction()
    }

    @State("member $MEMBER_ID has an active subscription")
    fun activeMember() {
        `when`(memberViewReadService.findSubscriptionEligibility(MemberId(MEMBER_ID))).thenReturn(
            SubscriptionEligibilityResponse(
                memberId = MEMBER_ID,
                exists = true,
                active = true,
                currentPeriodEndsAt = ZonedDateTime.parse("2026-12-31T23:59:59Z")
            )
        )
    }

    @State("member $MEMBER_ID has an inactive subscription")
    fun inactiveMember() {
        `when`(memberViewReadService.findSubscriptionEligibility(MemberId(MEMBER_ID))).thenReturn(
            SubscriptionEligibilityResponse(
                memberId = MEMBER_ID,
                exists = true,
                active = false,
                currentPeriodEndsAt = ZonedDateTime.parse("2026-01-31T23:59:59Z")
            )
        )
    }

    @State("member $MISSING_MEMBER_ID does not exist")
    fun missingMember() {
        `when`(memberViewReadService.findSubscriptionEligibility(MemberId(MISSING_MEMBER_ID))).thenReturn(null)
    }

    private companion object {
        const val MEMBER_ID = "11111111-1111-1111-1111-111111111111"
        const val MISSING_MEMBER_ID = "11111111-1111-1111-1111-111111111112"
    }
}
