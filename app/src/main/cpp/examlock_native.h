#pragma once

#include <jni.h>
#include <cstdint>
#include <string>
#include <vector>

namespace examlock_native {

struct ClipboardNormalizedItemCoreInput {
    std::string normalized_text;
    std::string normalized_html_plain_text;
    std::string normalized_uri;
    std::string normalized_intent_action;
    std::string normalized_intent_data;
    std::string normalized_intent_component;
    std::string raw_text;
    std::string raw_html;
    std::string raw_uri;
    std::string raw_intent_action;
    std::string raw_intent_data;
    std::string raw_intent_component;
    bool has_text = false;
    bool has_html = false;
    bool has_uri = false;
    bool has_intent = false;
};

struct ClipboardSnapshotCoreResult {
    std::string decision_fingerprint;
    std::string semantic_signature;
    std::string raw_signature;
    int item_count = 0;
    bool has_text = false;
    bool has_html = false;
    bool has_uri = false;
    bool has_intent = false;
    bool is_empty = true;
};

std::string JStringToUtf8(JNIEnv* env, jstring value);
jstring NewUtf8String(JNIEnv* env, const std::string& value);
std::vector<std::string> JObjectArrayToStrings(JNIEnv* env, jobjectArray array);
jobjectArray StringsToJObjectArray(JNIEnv* env, const std::vector<std::string>& values);
std::vector<uint8_t> JByteArrayToVector(JNIEnv* env, jbyteArray array);
jbyteArray VectorToJByteArray(JNIEnv* env, const std::vector<uint8_t>& values);
std::vector<double> JDoubleArrayToVector(JNIEnv* env, jdoubleArray array);
std::vector<ClipboardNormalizedItemCoreInput> JObjectArrayToClipboardInputs(JNIEnv* env, jobjectArray array);
jobject NewClipboardSnapshotCoreResult(JNIEnv* env, const ClipboardSnapshotCoreResult& result);

int ReadTracerPidNative();
std::vector<std::string> ScanProcMapsNative(const std::vector<std::string>& markers);
std::string ReadDexHashNative(const std::string& apkPath);
std::string ReadSystemPropertyNative(const std::string& key);
std::string DecodeBase64XorNative(const std::string& obfuscated);
std::vector<uint8_t> EncryptQrPayloadNative(JNIEnv* env, const std::vector<uint8_t>& plaintext);
std::vector<uint8_t> DecryptQrPayloadNative(JNIEnv* env, const std::vector<uint8_t>& packed);
ClipboardSnapshotCoreResult BuildClipboardSnapshotCoreNative(int mode, const std::vector<ClipboardNormalizedItemCoreInput>& items);
bool VerifyAdminSecretNative(const std::string& packageName, const std::string& dataDir, const std::string& androidId, const std::string& signingFingerprint, const std::string& candidate);
std::string BuildBypassDeviceBindingNative(const std::string& packageName, const std::string& dataDir, const std::string& androidId, const std::string& signingFingerprint, int versionCode);
std::string ComputeBypassMacNative(const std::string& payload, const std::string& deviceBinding);
std::string ComputeHmacSha256HexUpper(const std::string& key, const std::string& data);
bool IsPointInPolygonNative(double pointLat, double pointLng, const std::vector<double>& polygonLatitudes, const std::vector<double>& polygonLongitudes);
bool IsSelfIntersectingPolygonNative(const std::vector<double>& polygonLatitudes, const std::vector<double>& polygonLongitudes);
double DistancePointToSegmentMetersNative(double pointLat, double pointLng, double startLat, double startLng, double endLat, double endLng);
double ClosestCircleDistanceMetersNative(double locationLat, double locationLng, const std::vector<double>& centerLatitudes, const std::vector<double>& centerLongitudes);

}  // namespace examlock_native
