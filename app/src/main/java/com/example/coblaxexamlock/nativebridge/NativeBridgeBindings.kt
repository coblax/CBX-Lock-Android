@file:JvmName("NativeBridgeBindings")

package com.example.coblaxexamlock.nativebridge

import com.example.coblaxexamlock.ClipboardNormalizedItemInput
import com.example.coblaxexamlock.NativeClipboardSnapshotCore

internal external fun nativeReadTracerPid(): Int
internal external fun nativeScanProcMaps(markers: Array<String>): Array<String>
internal external fun nativeReadDexHash(apkPath: String): String
internal external fun nativeGetSystemProperty(key: String): String
internal external fun nativeDecodeBase64Xor(obfuscated: String): String
internal external fun nativeEncryptQrPayload(plaintext: ByteArray): ByteArray
internal external fun nativeDecryptQrPayload(packed: ByteArray): ByteArray
internal external fun nativeBuildClipboardSnapshotCore(
    mode: Int,
    items: Array<ClipboardNormalizedItemInput>
): NativeClipboardSnapshotCore
internal external fun nativeVerifyAdminSecret(
    packageName: String,
    dataDir: String,
    androidId: String,
    signingFingerprint: String,
    candidate: String
): Boolean
internal external fun nativeBuildBypassDeviceBinding(
    packageName: String,
    dataDir: String,
    androidId: String,
    signingFingerprint: String,
    versionCode: Int
): String
internal external fun nativeComputeBypassMac(
    payload: String,
    deviceBinding: String
): String
internal external fun nativeIsPointInPolygon(
    pointLat: Double,
    pointLng: Double,
    polygonLatitudes: DoubleArray,
    polygonLongitudes: DoubleArray
): Boolean
internal external fun nativeIsSelfIntersectingPolygon(
    polygonLatitudes: DoubleArray,
    polygonLongitudes: DoubleArray
): Boolean
internal external fun nativeDistancePointToSegmentMeters(
    pointLat: Double,
    pointLng: Double,
    startLat: Double,
    startLng: Double,
    endLat: Double,
    endLng: Double
): Double
internal external fun nativeClosestCircleDistanceMeters(
    locationLat: Double,
    locationLng: Double,
    centerLatitudes: DoubleArray,
    centerLongitudes: DoubleArray
): Double
