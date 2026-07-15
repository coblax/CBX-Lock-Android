package com.coblax.examlock.persistence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BypassEnvelopeVerifierTest {
    private fun fakeMac(payload: String, deviceBinding: String): String = "MAC:${payload}:${deviceBinding}"

    private fun encodeEnvelope(
        payload: BypassEnvelopePayload,
        macOverride: String? = null
    ): String {
        val serializedPayload = BypassEnvelopeCodec.encodePayload(payload)
        return BypassEnvelopeCodec.encodeEnvelope(
            BypassEnvelope(
                payload = serializedPayload,
                mac = macOverride ?: fakeMac(serializedPayload, payload.deviceBinding)
            )
        )
    }

    @Test
    fun validEnvelopePassesVerification() {
        val payload = BypassEnvelopePayload(
            monotonicCounter = 3L,
            createdAtEpochMillis = 10L,
            updatedAtEpochMillis = 20L,
            deviceBinding = "BINDING",
            gateStates = mapOf(1 to true, 2 to false)
        )

        val result = BypassEnvelopeVerifier.validateEncodedEnvelope(
            serializedEnvelope = encodeEnvelope(payload),
            expectedDeviceBinding = "BINDING",
            lastSeenCounter = 2L,
            macComputer = ::fakeMac
        )

        assertEquals(BypassEnvelopeValidationStatus.Valid, result.status)
        assertEquals(payload, result.payload)
    }

    @Test
    fun macMismatchIsReportedAsTampered() {
        val payload = BypassEnvelopePayload(
            monotonicCounter = 5L,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L,
            deviceBinding = "BINDING",
            gateStates = mapOf(1 to true)
        )

        val result = BypassEnvelopeVerifier.validateEncodedEnvelope(
            serializedEnvelope = encodeEnvelope(payload, macOverride = "WRONG"),
            expectedDeviceBinding = "BINDING",
            lastSeenCounter = 4L,
            macComputer = ::fakeMac
        )

        assertEquals(BypassEnvelopeValidationStatus.Tampered, result.status)
        assertEquals("mac_mismatch", result.reason)
        assertNull(result.payload)
    }

    @Test
    fun deviceBindingMismatchIsReportedAsTampered() {
        val payload = BypassEnvelopePayload(
            monotonicCounter = 5L,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L,
            deviceBinding = "BINDING-A",
            gateStates = mapOf(1 to true)
        )

        val result = BypassEnvelopeVerifier.validateEncodedEnvelope(
            serializedEnvelope = encodeEnvelope(payload),
            expectedDeviceBinding = "BINDING-B",
            lastSeenCounter = 4L,
            macComputer = ::fakeMac
        )

        assertEquals(BypassEnvelopeValidationStatus.Tampered, result.status)
        assertEquals("device_binding_mismatch", result.reason)
    }

    @Test
    fun counterRollbackIsReported() {
        val payload = BypassEnvelopePayload(
            monotonicCounter = 1L,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L,
            deviceBinding = "BINDING",
            gateStates = mapOf(1 to true)
        )

        val result = BypassEnvelopeVerifier.validateEncodedEnvelope(
            serializedEnvelope = encodeEnvelope(payload),
            expectedDeviceBinding = "BINDING",
            lastSeenCounter = 9L,
            macComputer = ::fakeMac
        )

        assertEquals(BypassEnvelopeValidationStatus.CounterRollback, result.status)
        assertEquals("counter_rollback", result.reason)
    }

    @Test
    fun malformedPayloadIsRejected() {
        val result = BypassEnvelopeVerifier.validateEncodedEnvelope(
            serializedEnvelope = BypassEnvelopeCodec.encodeEnvelope(
                BypassEnvelope(payload = "bad-payload", mac = "ANY")
            ),
            expectedDeviceBinding = "BINDING",
            lastSeenCounter = -1L,
            macComputer = ::fakeMac
        )

        assertEquals(BypassEnvelopeValidationStatus.Malformed, result.status)
        assertEquals("payload_malformed", result.reason)
    }
}
