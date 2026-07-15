package com.coblax.examlock.persistence

internal const val BypassEnvelopeSchemaVersion = 1
internal const val BypassDeviceBindingSchemeVersion = 2

internal data class BypassEnvelope(
    val payload: String,
    val mac: String
)

internal data class BypassEnvelopePayload(
    val schemaVersion: Int = BypassEnvelopeSchemaVersion,
    val monotonicCounter: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deviceBinding: String,
    val gateStates: Map<Int, Boolean>
)

internal data class BypassStorageReadResult(
    val gateStates: Map<Int, Boolean>,
    val tampered: Boolean,
    val migrationResetNotice: Boolean,
    val reason: String? = null
) {
    fun isEnabled(gateId: Int): Boolean = gateStates[gateId] == true
}

internal enum class BypassEnvelopeValidationStatus {
    Valid,
    Malformed,
    Tampered,
    CounterRollback,
    CryptoUnavailable
}

internal data class BypassEnvelopeValidationResult(
    val status: BypassEnvelopeValidationStatus,
    val payload: BypassEnvelopePayload? = null,
    val reason: String? = null
)
