#include "examlock_native.h"
#include "sha256.h"

#include <algorithm>
#include <array>
#include <cctype>
#include <cstdint>
#include <string>
#include <vector>

namespace examlock_native {
namespace {
constexpr std::array<uint8_t, 15> kBypassPepper = {0x46, 0x43, 0x4a, 0x4e, 0x49, 0x0a, 0x45, 0x5e, 0x57, 0x46, 0x54, 0x54, 0x0a, 0x51, 0x16};
constexpr uint8_t kBypassPepperXorKey = 0x27;

std::string DecodeObfuscated(const uint8_t* values, size_t length, uint8_t key) {
    std::string decoded;
    decoded.resize(length);
    for (size_t index = 0; index < length; ++index) {
        decoded[index] = static_cast<char>(values[index] ^ key);
    }
    return decoded;
}

std::string LowercaseAscii(std::string value) {
    std::transform(value.begin(), value.end(), value.begin(), [](unsigned char ch) {
        return static_cast<char>(std::tolower(ch));
    });
    return value;
}

std::vector<uint8_t> HmacSha256Bytes(const std::string& key, const std::string& data) {
    constexpr size_t kBlockSize = 64U;
    std::vector<uint8_t> normalizedKey(key.begin(), key.end());
    if (normalizedKey.size() > kBlockSize) {
        Sha256 keyHasher;
        keyHasher.Update(reinterpret_cast<const uint8_t*>(key.data()), key.size());
        const auto keyDigest = keyHasher.FinalBytes();
        normalizedKey.assign(keyDigest.begin(), keyDigest.end());
    }
    normalizedKey.resize(kBlockSize, 0U);

    std::array<uint8_t, kBlockSize> innerPad{};
    std::array<uint8_t, kBlockSize> outerPad{};
    for (size_t index = 0; index < kBlockSize; ++index) {
        innerPad[index] = static_cast<uint8_t>(normalizedKey[index] ^ 0x36U);
        outerPad[index] = static_cast<uint8_t>(normalizedKey[index] ^ 0x5cU);
    }

    Sha256 innerHasher;
    innerHasher.Update(innerPad.data(), innerPad.size());
    innerHasher.Update(reinterpret_cast<const uint8_t*>(data.data()), data.size());
    const auto innerDigest = innerHasher.FinalBytes();

    Sha256 outerHasher;
    outerHasher.Update(outerPad.data(), outerPad.size());
    outerHasher.Update(innerDigest.data(), innerDigest.size());
    const auto outerDigest = outerHasher.FinalBytes();

    return std::vector<uint8_t>(outerDigest.begin(), outerDigest.end());
}

std::string BypassPepper() {
    return DecodeObfuscated(kBypassPepper.data(), kBypassPepper.size(), kBypassPepperXorKey);
}
}  // namespace

std::string ComputeHmacSha256HexUpper(const std::string& key, const std::string& data) {
    const auto digest = HmacSha256Bytes(key, data);
    return Sha256::BytesToHexUpper(digest.data(), digest.size());
}

std::string BuildBypassDeviceBindingNative(const std::string& packageName, const std::string& dataDir, const std::string& androidId, const std::string& signingFingerprint, int versionCode) {
    static_cast<void>(versionCode);
    const std::string material = LowercaseAscii(
        packageName + "|" + dataDir + "|" + androidId + "|" + signingFingerprint + "|" + BypassPepper());
    Sha256 hasher;
    hasher.Update(reinterpret_cast<const uint8_t*>(material.data()), material.size());
    return hasher.FinalHexUpper();
}

std::string ComputeBypassMacNative(const std::string& payload, const std::string& deviceBinding) {
    if (payload.empty() || deviceBinding.empty()) {
        return std::string();
    }
    const std::string keyMaterial = deviceBinding + "|" + BypassPepper();
    return ComputeHmacSha256HexUpper(keyMaterial, payload);
}

}  // namespace examlock_native
