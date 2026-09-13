package com.borrowingservice.pact

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt
import au.com.dius.pact.consumer.junit5.PactTestFor
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.annotations.Pact
import com.borrowingservice.client.MembershipClient
import com.borrowingservice.client.MembershipClientConfiguration
import com.borrowingservice.client.MembershipMemberNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(PactConsumerTestExt::class)
@PactTestFor(providerName = "membership-http-provider", pactVersion = PactSpecVersion.V3)
class MembershipFeignConsumerPactTest {

    @Pact(consumer = "borrowing-http-consumer")
    fun activeMember(builder: PactDslWithProvider): RequestResponsePact = builder
        .given("member $MEMBER_ID has an active subscription")
        .uponReceiving("a request for active membership eligibility")
        .path("/api/members/$MEMBER_ID/subscription-status")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(JSON_HEADERS)
        .body(
            PactDslJsonBody()
                .stringValue("memberId", MEMBER_ID)
                .booleanValue("exists", true)
                .booleanValue("active", true)
                .stringMatcher("currentPeriodEndsAt", OFFSET_DATE_TIME_PATTERN, "2026-12-31T23:59:59Z")
        )
        .toPact()

    @Pact(consumer = "borrowing-http-consumer")
    fun inactiveMember(builder: PactDslWithProvider): RequestResponsePact = builder
        .given("member $MEMBER_ID has an inactive subscription")
        .uponReceiving("a request for inactive membership eligibility")
        .path("/api/members/$MEMBER_ID/subscription-status")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(JSON_HEADERS)
        .body(
            PactDslJsonBody()
                .stringValue("memberId", MEMBER_ID)
                .booleanValue("exists", true)
                .booleanValue("active", false)
                .stringMatcher("currentPeriodEndsAt", OFFSET_DATE_TIME_PATTERN, "2026-01-31T23:59:59Z")
        )
        .toPact()

    @Pact(consumer = "borrowing-http-consumer")
    fun missingMember(builder: PactDslWithProvider): RequestResponsePact = builder
        .given("member $MISSING_MEMBER_ID does not exist")
        .uponReceiving("a request for missing membership eligibility")
        .path("/api/members/$MISSING_MEMBER_ID/subscription-status")
        .method("GET")
        .willRespondWith()
        .status(404)
        .toPact()

    @Test
    @PactTestFor(pactMethod = "activeMember")
    fun `Feign decodes active eligibility`(mockServer: MockServer) {
        val response = client(mockServer).subscriptionEligibility(MEMBER_ID)

        assertEquals(MEMBER_ID, response.memberId)
        assertTrue(response.exists)
        assertTrue(response.active)
        assertEquals("2026-12-31T23:59:59Z", response.currentPeriodEndsAt.toString())
    }

    @Test
    @PactTestFor(pactMethod = "inactiveMember")
    fun `Feign decodes inactive eligibility`(mockServer: MockServer) {
        val response = client(mockServer).subscriptionEligibility(MEMBER_ID)

        assertTrue(response.exists)
        assertFalse(response.active)
    }

    @Test
    @PactTestFor(pactMethod = "missingMember")
    fun `Feign maps a missing member to the domain exception`(mockServer: MockServer) {
        assertThrows(MembershipMemberNotFoundException::class.java) {
            client(mockServer).subscriptionEligibility(MISSING_MEMBER_ID)
        }
    }

    private fun client(mockServer: MockServer): MembershipClient = pactFeignClient(
        MembershipClient::class.java,
        mockServer.getUrl(),
        MembershipClientConfiguration().membershipErrorDecoder()
    )

    private companion object {
        const val MEMBER_ID = "11111111-1111-1111-1111-111111111111"
        const val MISSING_MEMBER_ID = "11111111-1111-1111-1111-111111111112"
        const val OFFSET_DATE_TIME_PATTERN =
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})$"
        val JSON_HEADERS = mapOf("Content-Type" to "application/json")
    }
}
