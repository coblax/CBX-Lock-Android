package com.example.coblaxexamlock.persistence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BypassEnvelopeCodecTest {
    @Test
    fun payloadRoundTripsWithoutDrift() {
        val payload = BypassEnvelopePayload(
            monotonicCounter = 7L,
            createdAtEpochMillis = 1_000L,
            updatedAtEpochMillis = 2_000L,
            deviceBinding = "DEVICE-BINDING",
            gateStates = linkedMapOf(101 to true, 202 to false, 303 to true)
        )

        val encodedPayload = BypassEnvelopeCodec.encodePayload(payload)
        val decodedPayload = BypassEnvelopeCodec.decodePayload(encodedPayload)

        assertEquals(payload, decodedPayload)
    }

    @Test
    fun envelopeRoundTripsWithoutDrift() {
        val envelope = BypassEnvelope(payload = "v=1|ctr=1|crt=1|upd=1|dev=QQ|g=1:1", mac = "A1B2C3")

        val encoded = BypassEnvelopeCodec.encodeEnvelope(envelope)
        val decoded = BypassEnvelopeCodec.decodeEnvelope(encoded)

        assertEquals(envelope, decoded)
    }

    @Test
    fun malformedEnvelopeReturnsNull() {
        assertNull(BypassEnvelopeCodec.decodeEnvelope("not-a-valid-envelope"))
    }

    @Test
    fun malformedPayloadReturnsNull() {
        val decoded = BypassEnvelopeCodec.decodePayload("v=1|ctr=nope|crt=1|upd=1|dev=QQ|g=1:1")
        assertNull(decoded)
    }
}
