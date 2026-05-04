#include "examlock_native.h"
#include "sha256.h"

#include <array>
#include <stdexcept>
#include <string>
#include <vector>

namespace examlock_native {
namespace {
constexpr size_t kIvLength = 12U;
constexpr jint kAuthTagLengthBits = 128;
constexpr uint8_t kQrSeedXorKeyPartOne = 0x23U;
constexpr uint8_t kQrSeedXorKeyPartTwo = 0x47U;
constexpr uint8_t kQrSeedXorKeyPartThree = 0x6DU;
constexpr std::array<uint8_t, 14> kQrSeedFragmentA = {
    96U, 108U, 97U, 111U, 98U, 123U, 124U, 102U, 123U, 98U, 110U, 124U, 111U, 108U
};
constexpr std::array<uint8_t, 14> kQrSeedFragmentB = {
    4U, 12U, 24U, 8U, 1U, 1U, 11U, 14U, 9U, 2U, 24U, 22U, 21U, 24U
};
constexpr std::array<uint8_t, 14> kQrSeedFragmentC = {
    62U, 40U, 46U, 63U, 40U, 57U, 50U, 59U, 95U, 50U, 95U, 93U, 95U, 91U
};

void ThrowJavaRuntimeException(JNIEnv* env, const char* message) {
    jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
    if (exceptionClass != nullptr) {
        env->ThrowNew(exceptionClass, message);
        env->DeleteLocalRef(exceptionClass);
    }
}

std::vector<uint8_t> BuildQrSecretSeedBytes() {
    std::vector<uint8_t> seed;
    seed.reserve(kQrSeedFragmentA.size() + kQrSeedFragmentB.size() + kQrSeedFragmentC.size());
    const auto append_fragment = [&seed](const auto& fragment, uint8_t xorKey) {
        for (const auto encoded : fragment) {
            seed.push_back(static_cast<uint8_t>(encoded ^ xorKey));
        }
    };
    append_fragment(kQrSeedFragmentA, kQrSeedXorKeyPartOne);
    append_fragment(kQrSeedFragmentB, kQrSeedXorKeyPartTwo);
    append_fragment(kQrSeedFragmentC, kQrSeedXorKeyPartThree);
    return seed;
}

std::vector<uint8_t> DeriveQrKeyBytes() {
    const std::vector<uint8_t> seed = BuildQrSecretSeedBytes();
    Sha256 sha256;
    sha256.Update(seed.data(), seed.size());
    const auto digest = sha256.FinalBytes();
    return std::vector<uint8_t>(digest.begin(), digest.end());
}

jobject BuildCipher(JNIEnv* env, jint mode, jbyteArray key_bytes, jbyteArray iv_bytes) {
    jclass cipherClass = env->FindClass("javax/crypto/Cipher");
    jclass secretKeySpecClass = env->FindClass("javax/crypto/spec/SecretKeySpec");
    jclass gcmParameterSpecClass = env->FindClass("javax/crypto/spec/GCMParameterSpec");
    if (cipherClass == nullptr || secretKeySpecClass == nullptr || gcmParameterSpecClass == nullptr) {
        ThrowJavaRuntimeException(env, "QR crypto classes are unavailable.");
        return nullptr;
    }

    jmethodID cipherGetInstance = env->GetStaticMethodID(
        cipherClass,
        "getInstance",
        "(Ljava/lang/String;)Ljavax/crypto/Cipher;"
    );
    jmethodID secretKeySpecCtor = env->GetMethodID(secretKeySpecClass, "<init>", "([BLjava/lang/String;)V");
    jmethodID gcmParameterSpecCtor = env->GetMethodID(gcmParameterSpecClass, "<init>", "(I[B)V");
    jmethodID cipherInit = env->GetMethodID(
        cipherClass,
        "init",
        "(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V"
    );
    if (
        cipherGetInstance == nullptr ||
        secretKeySpecCtor == nullptr ||
        gcmParameterSpecCtor == nullptr ||
        cipherInit == nullptr
    ) {
        ThrowJavaRuntimeException(env, "QR crypto methods are unavailable.");
        env->DeleteLocalRef(cipherClass);
        env->DeleteLocalRef(secretKeySpecClass);
        env->DeleteLocalRef(gcmParameterSpecClass);
        return nullptr;
    }

    jstring aesTransformation = env->NewStringUTF("AES/GCM/NoPadding");
    jstring aesAlgorithm = env->NewStringUTF("AES");
    jobject cipher = env->CallStaticObjectMethod(cipherClass, cipherGetInstance, aesTransformation);
    jobject secretKeySpec = env->NewObject(secretKeySpecClass, secretKeySpecCtor, key_bytes, aesAlgorithm);
    jobject gcmParameterSpec = env->NewObject(gcmParameterSpecClass, gcmParameterSpecCtor, kAuthTagLengthBits, iv_bytes);

    env->CallVoidMethod(cipher, cipherInit, mode, secretKeySpec, gcmParameterSpec);

    env->DeleteLocalRef(aesTransformation);
    env->DeleteLocalRef(aesAlgorithm);
    env->DeleteLocalRef(secretKeySpec);
    env->DeleteLocalRef(gcmParameterSpec);
    env->DeleteLocalRef(cipherClass);
    env->DeleteLocalRef(secretKeySpecClass);
    env->DeleteLocalRef(gcmParameterSpecClass);
    return cipher;
}

std::vector<uint8_t> EncryptWithJavaCipher(JNIEnv* env, const std::vector<uint8_t>& plaintext) {
    jclass secureRandomClass = env->FindClass("java/security/SecureRandom");
    if (secureRandomClass == nullptr) {
        ThrowJavaRuntimeException(env, "SecureRandom is unavailable.");
        return {};
    }

    jmethodID secureRandomCtor = env->GetMethodID(secureRandomClass, "<init>", "()V");
    jmethodID nextBytes = env->GetMethodID(secureRandomClass, "nextBytes", "([B)V");
    if (secureRandomCtor == nullptr || nextBytes == nullptr) {
        ThrowJavaRuntimeException(env, "SecureRandom methods are unavailable.");
        env->DeleteLocalRef(secureRandomClass);
        return {};
    }

    jobject secureRandom = env->NewObject(secureRandomClass, secureRandomCtor);
    jbyteArray ivBytes = env->NewByteArray(static_cast<jsize>(kIvLength));
    env->CallVoidMethod(secureRandom, nextBytes, ivBytes);

    const std::vector<uint8_t> key = DeriveQrKeyBytes();
    jbyteArray keyBytes = VectorToJByteArray(env, key);
    jobject cipher = BuildCipher(env, 1, keyBytes, ivBytes);
    if (cipher == nullptr) {
        env->DeleteLocalRef(secureRandom);
        env->DeleteLocalRef(secureRandomClass);
        env->DeleteLocalRef(ivBytes);
        env->DeleteLocalRef(keyBytes);
        return {};
    }

    jclass cipherClass = env->FindClass("javax/crypto/Cipher");
    jmethodID doFinal = env->GetMethodID(cipherClass, "doFinal", "([B)[B");
    jbyteArray plaintextBytes = VectorToJByteArray(env, plaintext);
    jbyteArray encryptedBytes = static_cast<jbyteArray>(env->CallObjectMethod(cipher, doFinal, plaintextBytes));

    const std::vector<uint8_t> iv = JByteArrayToVector(env, ivBytes);
    const std::vector<uint8_t> encrypted = JByteArrayToVector(env, encryptedBytes);
    std::vector<uint8_t> packed;
    packed.reserve(iv.size() + encrypted.size());
    packed.insert(packed.end(), iv.begin(), iv.end());
    packed.insert(packed.end(), encrypted.begin(), encrypted.end());

    env->DeleteLocalRef(secureRandom);
    env->DeleteLocalRef(secureRandomClass);
    env->DeleteLocalRef(ivBytes);
    env->DeleteLocalRef(keyBytes);
    env->DeleteLocalRef(cipher);
    env->DeleteLocalRef(cipherClass);
    env->DeleteLocalRef(plaintextBytes);
    if (encryptedBytes != nullptr) {
        env->DeleteLocalRef(encryptedBytes);
    }
    return packed;
}

std::vector<uint8_t> DecryptWithJavaCipher(JNIEnv* env, const std::vector<uint8_t>& packed) {
    if (packed.size() <= kIvLength) {
        ThrowJavaRuntimeException(env, "QR payload is incomplete.");
        return {};
    }

    const std::vector<uint8_t> iv(packed.begin(), packed.begin() + kIvLength);
    const std::vector<uint8_t> encrypted(packed.begin() + kIvLength, packed.end());
    const std::vector<uint8_t> key = DeriveQrKeyBytes();

    jbyteArray ivBytes = VectorToJByteArray(env, iv);
    jbyteArray keyBytes = VectorToJByteArray(env, key);
    jobject cipher = BuildCipher(env, 2, keyBytes, ivBytes);
    if (cipher == nullptr) {
        env->DeleteLocalRef(ivBytes);
        env->DeleteLocalRef(keyBytes);
        return {};
    }

    jclass cipherClass = env->FindClass("javax/crypto/Cipher");
    jmethodID doFinal = env->GetMethodID(cipherClass, "doFinal", "([B)[B");
    jbyteArray encryptedBytes = VectorToJByteArray(env, encrypted);
    jbyteArray plaintextBytes = static_cast<jbyteArray>(env->CallObjectMethod(cipher, doFinal, encryptedBytes));
    std::vector<uint8_t> plaintext = JByteArrayToVector(env, plaintextBytes);

    env->DeleteLocalRef(ivBytes);
    env->DeleteLocalRef(keyBytes);
    env->DeleteLocalRef(cipher);
    env->DeleteLocalRef(cipherClass);
    env->DeleteLocalRef(encryptedBytes);
    if (plaintextBytes != nullptr) {
        env->DeleteLocalRef(plaintextBytes);
    }
    return plaintext;
}
}  // namespace

std::vector<uint8_t> EncryptQrPayloadNative(JNIEnv* env, const std::vector<uint8_t>& plaintext) {
    return EncryptWithJavaCipher(env, plaintext);
}

std::vector<uint8_t> DecryptQrPayloadNative(JNIEnv* env, const std::vector<uint8_t>& packed) {
    return DecryptWithJavaCipher(env, packed);
}

}  // namespace examlock_native
