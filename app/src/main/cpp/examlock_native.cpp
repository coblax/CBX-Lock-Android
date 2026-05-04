#include "examlock_native.h"

#include <android/log.h>
#include <jni.h>

#include <string>
#include <utility>
#include <vector>

namespace examlock_native {
namespace {
constexpr const char* kBindingsClassName = "com/example/coblaxexamlock/nativebridge/NativeBridgeBindings";
constexpr const char* kClipboardInputClassName = "com/example/coblaxexamlock/ClipboardNormalizedItemInput";
constexpr const char* kClipboardResultClassName = "com/example/coblaxexamlock/NativeClipboardSnapshotCore";
constexpr const char* kLogTag = "examlock_native";

struct ClipboardInputFieldIds {
    jfieldID normalized_text;
    jfieldID normalized_html_plain_text;
    jfieldID normalized_uri;
    jfieldID normalized_intent_action;
    jfieldID normalized_intent_data;
    jfieldID normalized_intent_component;
    jfieldID raw_text;
    jfieldID raw_html;
    jfieldID raw_uri;
    jfieldID raw_intent_action;
    jfieldID raw_intent_data;
    jfieldID raw_intent_component;
    jfieldID has_text;
    jfieldID has_html;
    jfieldID has_uri;
    jfieldID has_intent;
};

ClipboardInputFieldIds ResolveClipboardInputFieldIds(JNIEnv* env, jclass item_class) {
    return ClipboardInputFieldIds{
        env->GetFieldID(item_class, "normalizedText", "Ljava/lang/String;"),
        env->GetFieldID(item_class, "normalizedHtmlPlainText", "Ljava/lang/String;"),
        env->GetFieldID(item_class, "normalizedUri", "Ljava/lang/String;"),
        env->GetFieldID(item_class, "normalizedIntentAction", "Ljava/lang/String;"),
        env->GetFieldID(item_class, "normalizedIntentData", "Ljava/lang/String;"),
        env->GetFieldID(item_class, "normalizedIntentComponent", "Ljava/lang/String;"),
        env->GetFieldID(item_class, "rawText", "Ljava/lang/String;"),
        env->GetFieldID(item_class, "rawHtml", "Ljava/lang/String;"),
        env->GetFieldID(item_class, "rawUri", "Ljava/lang/String;"),
        env->GetFieldID(item_class, "rawIntentAction", "Ljava/lang/String;"),
        env->GetFieldID(item_class, "rawIntentData", "Ljava/lang/String;"),
        env->GetFieldID(item_class, "rawIntentComponent", "Ljava/lang/String;"),
        env->GetFieldID(item_class, "hasText", "Z"),
        env->GetFieldID(item_class, "hasHtml", "Z"),
        env->GetFieldID(item_class, "hasUri", "Z"),
        env->GetFieldID(item_class, "hasIntent", "Z"),
    };
}

std::string ReadStringField(JNIEnv* env, jobject object, jfieldID field_id) {
    if (field_id == nullptr) {
        return std::string();
    }
    auto* value = static_cast<jstring>(env->GetObjectField(object, field_id));
    std::string result = JStringToUtf8(env, value);
    if (value != nullptr) {
        env->DeleteLocalRef(value);
    }
    return result;
}

jint NativeReadTracerPid(JNIEnv*, jclass) {
    return static_cast<jint>(ReadTracerPidNative());
}

jobjectArray NativeScanProcMaps(JNIEnv* env, jclass, jobjectArray markers) {
    return StringsToJObjectArray(env, ScanProcMapsNative(JObjectArrayToStrings(env, markers)));
}

jstring NativeReadDexHash(JNIEnv* env, jclass, jstring apkPath) {
    return NewUtf8String(env, ReadDexHashNative(JStringToUtf8(env, apkPath)));
}

jstring NativeGetSystemProperty(JNIEnv* env, jclass, jstring key) {
    return NewUtf8String(env, ReadSystemPropertyNative(JStringToUtf8(env, key)));
}

jstring NativeDecodeBase64Xor(JNIEnv* env, jclass, jstring obfuscated) {
    return NewUtf8String(env, DecodeBase64XorNative(JStringToUtf8(env, obfuscated)));
}

jbyteArray NativeEncryptQrPayload(JNIEnv* env, jclass, jbyteArray plaintext) {
    return VectorToJByteArray(env, EncryptQrPayloadNative(env, JByteArrayToVector(env, plaintext)));
}

jbyteArray NativeDecryptQrPayload(JNIEnv* env, jclass, jbyteArray packed) {
    return VectorToJByteArray(env, DecryptQrPayloadNative(env, JByteArrayToVector(env, packed)));
}

jobject NativeBuildClipboardSnapshotCore(JNIEnv* env, jclass, jint mode, jobjectArray items) {
    return NewClipboardSnapshotCoreResult(
        env,
        BuildClipboardSnapshotCoreNative(static_cast<int>(mode), JObjectArrayToClipboardInputs(env, items))
    );
}

jboolean NativeVerifyAdminSecret(JNIEnv* env, jclass, jstring packageName, jstring dataDir, jstring androidId, jstring signingFingerprint, jstring candidate) {
    return VerifyAdminSecretNative(
        JStringToUtf8(env, packageName),
        JStringToUtf8(env, dataDir),
        JStringToUtf8(env, androidId),
        JStringToUtf8(env, signingFingerprint),
        JStringToUtf8(env, candidate)
    ) ? JNI_TRUE : JNI_FALSE;
}

jstring NativeBuildBypassDeviceBinding(JNIEnv* env, jclass, jstring packageName, jstring dataDir, jstring androidId, jstring signingFingerprint, jint versionCode) {
    return NewUtf8String(
        env,
        BuildBypassDeviceBindingNative(
            JStringToUtf8(env, packageName),
            JStringToUtf8(env, dataDir),
            JStringToUtf8(env, androidId),
            JStringToUtf8(env, signingFingerprint),
            static_cast<int>(versionCode)
        )
    );
}

jstring NativeComputeBypassMac(JNIEnv* env, jclass, jstring payload, jstring deviceBinding) {
    return NewUtf8String(env, ComputeBypassMacNative(JStringToUtf8(env, payload), JStringToUtf8(env, deviceBinding)));
}

jboolean NativeIsPointInPolygon(JNIEnv* env, jclass, jdouble pointLat, jdouble pointLng, jdoubleArray polygonLatitudes, jdoubleArray polygonLongitudes) {
    return IsPointInPolygonNative(pointLat, pointLng, JDoubleArrayToVector(env, polygonLatitudes), JDoubleArrayToVector(env, polygonLongitudes)) ? JNI_TRUE : JNI_FALSE;
}

jboolean NativeIsSelfIntersectingPolygon(JNIEnv* env, jclass, jdoubleArray polygonLatitudes, jdoubleArray polygonLongitudes) {
    return IsSelfIntersectingPolygonNative(JDoubleArrayToVector(env, polygonLatitudes), JDoubleArrayToVector(env, polygonLongitudes)) ? JNI_TRUE : JNI_FALSE;
}

jdouble NativeDistancePointToSegmentMeters(JNIEnv*, jclass, jdouble pointLat, jdouble pointLng, jdouble startLat, jdouble startLng, jdouble endLat, jdouble endLng) {
    return DistancePointToSegmentMetersNative(pointLat, pointLng, startLat, startLng, endLat, endLng);
}

jdouble NativeClosestCircleDistanceMeters(JNIEnv* env, jclass, jdouble locationLat, jdouble locationLng, jdoubleArray centerLatitudes, jdoubleArray centerLongitudes) {
    return ClosestCircleDistanceMetersNative(locationLat, locationLng, JDoubleArrayToVector(env, centerLatitudes), JDoubleArrayToVector(env, centerLongitudes));
}

const JNINativeMethod kMethods[] = {
    {"nativeReadTracerPid", "()I", reinterpret_cast<void*>(NativeReadTracerPid)},
    {"nativeScanProcMaps", "([Ljava/lang/String;)[Ljava/lang/String;", reinterpret_cast<void*>(NativeScanProcMaps)},
    {"nativeReadDexHash", "(Ljava/lang/String;)Ljava/lang/String;", reinterpret_cast<void*>(NativeReadDexHash)},
    {"nativeGetSystemProperty", "(Ljava/lang/String;)Ljava/lang/String;", reinterpret_cast<void*>(NativeGetSystemProperty)},
    {"nativeDecodeBase64Xor", "(Ljava/lang/String;)Ljava/lang/String;", reinterpret_cast<void*>(NativeDecodeBase64Xor)},
    {"nativeEncryptQrPayload", "([B)[B", reinterpret_cast<void*>(NativeEncryptQrPayload)},
    {"nativeDecryptQrPayload", "([B)[B", reinterpret_cast<void*>(NativeDecryptQrPayload)},
    {"nativeBuildClipboardSnapshotCore", "(I[Lcom/example/coblaxexamlock/ClipboardNormalizedItemInput;)Lcom/example/coblaxexamlock/NativeClipboardSnapshotCore;", reinterpret_cast<void*>(NativeBuildClipboardSnapshotCore)},
    {"nativeVerifyAdminSecret", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", reinterpret_cast<void*>(NativeVerifyAdminSecret)},
    {"nativeBuildBypassDeviceBinding", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/String;", reinterpret_cast<void*>(NativeBuildBypassDeviceBinding)},
    {"nativeComputeBypassMac", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", reinterpret_cast<void*>(NativeComputeBypassMac)},
    {"nativeIsPointInPolygon", "(DD[D[D)Z", reinterpret_cast<void*>(NativeIsPointInPolygon)},
    {"nativeIsSelfIntersectingPolygon", "([D[D)Z", reinterpret_cast<void*>(NativeIsSelfIntersectingPolygon)},
    {"nativeDistancePointToSegmentMeters", "(DDDDDD)D", reinterpret_cast<void*>(NativeDistancePointToSegmentMeters)},
    {"nativeClosestCircleDistanceMeters", "(DD[D[D)D", reinterpret_cast<void*>(NativeClosestCircleDistanceMeters)},
};
}  // namespace

std::string JStringToUtf8(JNIEnv* env, jstring value) {
    if (value == nullptr) {
        return std::string();
    }
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) {
        return std::string();
    }
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

jstring NewUtf8String(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}

std::vector<std::string> JObjectArrayToStrings(JNIEnv* env, jobjectArray array) {
    std::vector<std::string> values;
    if (array == nullptr) {
        return values;
    }
    const jsize length = env->GetArrayLength(array);
    values.reserve(static_cast<size_t>(length));
    for (jsize index = 0; index < length; ++index) {
        auto* element = static_cast<jstring>(env->GetObjectArrayElement(array, index));
        values.push_back(JStringToUtf8(env, element));
        env->DeleteLocalRef(element);
    }
    return values;
}

jobjectArray StringsToJObjectArray(JNIEnv* env, const std::vector<std::string>& values) {
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray array = env->NewObjectArray(static_cast<jsize>(values.size()), stringClass, nullptr);
    for (size_t index = 0; index < values.size(); ++index) {
        env->SetObjectArrayElement(array, static_cast<jsize>(index), NewUtf8String(env, values[index]));
    }
    env->DeleteLocalRef(stringClass);
    return array;
}

std::vector<uint8_t> JByteArrayToVector(JNIEnv* env, jbyteArray array) {
    std::vector<uint8_t> values;
    if (array == nullptr) {
        return values;
    }
    const jsize length = env->GetArrayLength(array);
    values.resize(static_cast<size_t>(length));
    env->GetByteArrayRegion(array, 0, length, reinterpret_cast<jbyte*>(values.data()));
    return values;
}

jbyteArray VectorToJByteArray(JNIEnv* env, const std::vector<uint8_t>& values) {
    jbyteArray array = env->NewByteArray(static_cast<jsize>(values.size()));
    if (array == nullptr || values.empty()) {
        return array;
    }
    env->SetByteArrayRegion(array, 0, static_cast<jsize>(values.size()), reinterpret_cast<const jbyte*>(values.data()));
    return array;
}

std::vector<double> JDoubleArrayToVector(JNIEnv* env, jdoubleArray array) {
    std::vector<double> values;
    if (array == nullptr) {
        return values;
    }
    const jsize length = env->GetArrayLength(array);
    values.resize(static_cast<size_t>(length));
    env->GetDoubleArrayRegion(array, 0, length, values.data());
    return values;
}

std::vector<ClipboardNormalizedItemCoreInput> JObjectArrayToClipboardInputs(JNIEnv* env, jobjectArray array) {
    std::vector<ClipboardNormalizedItemCoreInput> values;
    if (array == nullptr) {
        return values;
    }

    jclass itemClass = env->FindClass(kClipboardInputClassName);
    if (itemClass == nullptr) {
        return values;
    }

    const ClipboardInputFieldIds fields = ResolveClipboardInputFieldIds(env, itemClass);
    const jsize length = env->GetArrayLength(array);
    values.reserve(static_cast<size_t>(length));
    for (jsize index = 0; index < length; ++index) {
        auto* itemObject = env->GetObjectArrayElement(array, index);
        ClipboardNormalizedItemCoreInput item;
        item.normalized_text = ReadStringField(env, itemObject, fields.normalized_text);
        item.normalized_html_plain_text = ReadStringField(env, itemObject, fields.normalized_html_plain_text);
        item.normalized_uri = ReadStringField(env, itemObject, fields.normalized_uri);
        item.normalized_intent_action = ReadStringField(env, itemObject, fields.normalized_intent_action);
        item.normalized_intent_data = ReadStringField(env, itemObject, fields.normalized_intent_data);
        item.normalized_intent_component = ReadStringField(env, itemObject, fields.normalized_intent_component);
        item.raw_text = ReadStringField(env, itemObject, fields.raw_text);
        item.raw_html = ReadStringField(env, itemObject, fields.raw_html);
        item.raw_uri = ReadStringField(env, itemObject, fields.raw_uri);
        item.raw_intent_action = ReadStringField(env, itemObject, fields.raw_intent_action);
        item.raw_intent_data = ReadStringField(env, itemObject, fields.raw_intent_data);
        item.raw_intent_component = ReadStringField(env, itemObject, fields.raw_intent_component);
        item.has_text = fields.has_text != nullptr && env->GetBooleanField(itemObject, fields.has_text) == JNI_TRUE;
        item.has_html = fields.has_html != nullptr && env->GetBooleanField(itemObject, fields.has_html) == JNI_TRUE;
        item.has_uri = fields.has_uri != nullptr && env->GetBooleanField(itemObject, fields.has_uri) == JNI_TRUE;
        item.has_intent = fields.has_intent != nullptr && env->GetBooleanField(itemObject, fields.has_intent) == JNI_TRUE;
        values.push_back(std::move(item));
        env->DeleteLocalRef(itemObject);
    }

    env->DeleteLocalRef(itemClass);
    return values;
}

jobject NewClipboardSnapshotCoreResult(JNIEnv* env, const ClipboardSnapshotCoreResult& result) {
    jclass resultClass = env->FindClass(kClipboardResultClassName);
    if (resultClass == nullptr) {
        return nullptr;
    }

    jmethodID constructor = env->GetMethodID(
        resultClass,
        "<init>",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IZZZZZ)V"
    );
    if (constructor == nullptr) {
        env->DeleteLocalRef(resultClass);
        return nullptr;
    }

    jstring decisionFingerprint = NewUtf8String(env, result.decision_fingerprint);
    jstring semanticSignature = NewUtf8String(env, result.semantic_signature);
    jstring rawSignature = NewUtf8String(env, result.raw_signature);

    jobject object = env->NewObject(
        resultClass,
        constructor,
        decisionFingerprint,
        semanticSignature,
        rawSignature,
        static_cast<jint>(result.item_count),
        result.has_text ? JNI_TRUE : JNI_FALSE,
        result.has_html ? JNI_TRUE : JNI_FALSE,
        result.has_uri ? JNI_TRUE : JNI_FALSE,
        result.has_intent ? JNI_TRUE : JNI_FALSE,
        result.is_empty ? JNI_TRUE : JNI_FALSE
    );

    env->DeleteLocalRef(decisionFingerprint);
    env->DeleteLocalRef(semanticSignature);
    env->DeleteLocalRef(rawSignature);
    env->DeleteLocalRef(resultClass);
    return object;
}

}  // namespace examlock_native

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK || env == nullptr) {
        return JNI_ERR;
    }
    jclass bindingsClass = env->FindClass(examlock_native::kBindingsClassName);
    if (bindingsClass == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, examlock_native::kLogTag, "Failed to find NativeBridgeBindings class.");
        return JNI_ERR;
    }
    if (env->RegisterNatives(bindingsClass, examlock_native::kMethods, sizeof(examlock_native::kMethods) / sizeof(examlock_native::kMethods[0])) != JNI_OK) {
        __android_log_print(ANDROID_LOG_ERROR, examlock_native::kLogTag, "RegisterNatives failed.");
        env->DeleteLocalRef(bindingsClass);
        return JNI_ERR;
    }
    env->DeleteLocalRef(bindingsClass);
    return JNI_VERSION_1_6;
}
