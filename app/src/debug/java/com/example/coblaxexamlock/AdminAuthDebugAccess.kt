package com.example.coblaxexamlock

internal object AdminAuthDebugAccess {
    private const val DebugPasswordXorKey = 0x17
    private val debugPasswordEncoded = intArrayOf(37, 37, 36, 33, 38, 38)

    fun knownPasswordForTests(): String =
        buildString(debugPasswordEncoded.size) {
            debugPasswordEncoded.forEach { encoded ->
                append((encoded xor DebugPasswordXorKey).toChar())
            }
        }
}
