package com.example.coblaxexamlock.persistence

internal object BypassEnvelopeVerifier {
    fun validateEncodedEnvelope(
        serializedEnvelope: String,
        expectedDeviceBinding: String,
        lastSeenCounter: Long,
        macComputer: (payload: String, deviceBinding: String) -> String
    ): BypassEnvelopeValidationResult {
        if (serializedEnvelope.isBlank()) {
            return BypassEnvelopeValidationResult(
                status = BypassEnvelopeValidationStatus.Malformed,
                reason = "envelope_missing"
            )
        }
        if (expectedDeviceBinding.isBlank()) {
            return BypassEnvelopeValidationResult(
                status = BypassEnvelopeValidationStatus.CryptoUnavailable,
                reason = "device_binding_unavailable"
            )
        }
        val envelope = BypassEnvelopeCodec.decodeEnvelope(serializedEnvelope)
            ?: return BypassEnvelopeValidationResult(
                status = BypassEnvelopeValidationStatus.Malformed,
                reason = "envelope_malformed"
            )
        val payload = BypassEnvelopeCodec.decodePayload(envelope.payload)
            ?: return BypassEnvelopeValidationResult(
                status = BypassEnvelopeValidationStatus.Malformed,
                reason = "payload_malformed"
            )
        if (payload.schemaVersion != BypassEnvelopeSchemaVersion) {
            return BypassEnvelopeValidationResult(
                status = BypassEnvelopeValidationStatus.Tampered,
                reason = "schema_version_mismatch"
            )
        }
        if (payload.deviceBinding != expectedDeviceBinding) {
            return BypassEnvelopeValidationResult(
                status = BypassEnvelopeValidationStatus.Tampered,
                reason = "device_binding_mismatch"
            )
        }
        val expectedMac = macComputer(envelope.payload, expectedDeviceBinding)
        if (expectedMac.isBlank()) {
            return BypassEnvelopeValidationResult(
                status = BypassEnvelopeValidationStatus.CryptoUnavailable,
                reason = "mac_unavailable"
            )
        }
        if (!constantTimeEquals(envelope.mac, expectedMac)) {
            return BypassEnvelopeValidationResult(
                status = BypassEnvelopeValidationStatus.Tampered,
                reason = "mac_mismatch"
            )
        }
        if (lastSeenCounter >= 0L && payload.monotonicCounter < lastSeenCounter) {
            return BypassEnvelopeValidationResult(
                status = BypassEnvelopeValidationStatus.CounterRollback,
                reason = "counter_rollback"
            )
        }
        return BypassEnvelopeValidationResult(
            status = BypassEnvelopeValidationStatus.Valid,
            payload = payload
        )
    }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        if (left.length != right.length) return false
        var diff = 0
        for (index in left.indices) {
            diff = diff or (left[index].code xor right[index].code)
        }
        return diff == 0
    }
}
