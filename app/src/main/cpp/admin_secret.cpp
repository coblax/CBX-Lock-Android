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

constexpr int kAdminPbkdf2Iterations = 150000;
constexpr size_t kAdminVerifierLength = 32U;
constexpr uint8_t kPbkdf2InnerPadXor = 0x36U;
constexpr uint8_t kPbkdf2OuterPadXor = 0x5CU;

constexpr std::array<uint8_t, 11> kAdminSaltPartOne = {
    103U, 107U, 102U, 104U, 101U, 124U, 123U, 101U, 96U, 105U, 109U
};
constexpr std::array<uint8_t, 11> kAdminSaltPartTwo = {
    9U, 24U, 6U, 18U, 19U, 15U, 24U, 17U, 116U, 24U, 23U
};
constexpr std::array<uint8_t, 10> kAdminSaltPartThree = {
    44U, 37U, 42U, 40U, 92U, 49U, 92U, 94U, 92U, 88U
};
constexpr uint8_t kAdminSaltXorKeyPartOne = 0x24U;
constexpr uint8_t kAdminSaltXorKeyPartTwo = 0x47U;
constexpr uint8_t kAdminSaltXorKeyPartThree = 0x6EU;

constexpr std::array<uint8_t, 16> kExpectedVerifierPartOne = {
    243U, 99U, 169U, 118U, 8U, 24U, 225U, 12U, 161U, 240U, 49U, 134U, 54U, 37U, 180U, 23U
};
constexpr std::array<uint8_t, 16> kExpectedVerifierPartTwo = {
    14U, 70U, 181U, 249U, 88U, 152U, 45U, 48U, 236U, 107U, 157U, 180U, 114U, 242U, 199U, 194U
};
constexpr uint8_t kExpectedVerifierXorKeyPartOne = 0x31U;
constexpr uint8_t kExpectedVerifierXorKeyPartTwo = 0x52U;

constexpr std::array<uint8_t, 3> kMasterKeyPartOne = {0x5f, 0x52, 0x4b};
constexpr std::array<uint8_t, 3> kMasterKeyPartTwo = {0x50, 0x5c, 0x51};
constexpr std::array<uint8_t, 4> kMasterKeyPartThree = {0x5f, 0x5c, 0x50, 0x58};
constexpr uint8_t kMasterKeyXorKey = 0x33U;

template <size_t N>
void AppendDecodedBytes(std::vector<uint8_t>* destination, const std::array<uint8_t, N>& fragment, uint8_t xor_key) {
    destination->reserve(destination->size() + N);
    for (const uint8_t value : fragment) {
        destination->push_back(static_cast<uint8_t>(value ^ xor_key));
    }
}

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

bool ConstantTimeEquals(const std::string& left, const std::string& right) {
    if (left.size() != right.size()) {
        return false;
    }
    uint8_t diff = 0U;
    for (size_t index = 0; index < left.size(); ++index) {
        diff |= static_cast<uint8_t>(left[index] ^ right[index]);
    }
    return diff == 0U;
}

std::vector<uint8_t> HmacSha256Bytes(const std::vector<uint8_t>& key, const std::vector<uint8_t>& data) {
    constexpr size_t kBlockSize = 64U;
    std::vector<uint8_t> normalizedKey = key;
    if (normalizedKey.size() > kBlockSize) {
        Sha256 keyHasher;
        keyHasher.Update(normalizedKey.data(), normalizedKey.size());
        const auto keyDigest = keyHasher.FinalBytes();
        normalizedKey.assign(keyDigest.begin(), keyDigest.end());
    }
    normalizedKey.resize(kBlockSize, 0U);

    std::array<uint8_t, kBlockSize> innerPad{};
    std::array<uint8_t, kBlockSize> outerPad{};
    for (size_t index = 0; index < kBlockSize; ++index) {
        innerPad[index] = static_cast<uint8_t>(normalizedKey[index] ^ kPbkdf2InnerPadXor);
        outerPad[index] = static_cast<uint8_t>(normalizedKey[index] ^ kPbkdf2OuterPadXor);
    }

    Sha256 innerHasher;
    innerHasher.Update(innerPad.data(), innerPad.size());
    if (!data.empty()) {
        innerHasher.Update(data.data(), data.size());
    }
    const auto innerDigest = innerHasher.FinalBytes();

    Sha256 outerHasher;
    outerHasher.Update(outerPad.data(), outerPad.size());
    outerHasher.Update(innerDigest.data(), innerDigest.size());
    const auto outerDigest = outerHasher.FinalBytes();

    return std::vector<uint8_t>(outerDigest.begin(), outerDigest.end());
}

std::vector<uint8_t> Pbkdf2HmacSha256(
    const std::string& password,
    const std::vector<uint8_t>& salt,
    int iterations,
    size_t outputLength
) {
    const std::vector<uint8_t> passwordBytes(password.begin(), password.end());
    const size_t hashLength = 32U;
    const size_t blockCount = (outputLength + hashLength - 1U) / hashLength;
    std::vector<uint8_t> derived;
    derived.reserve(blockCount * hashLength);

    for (size_t blockIndex = 1U; blockIndex <= blockCount; ++blockIndex) {
        std::vector<uint8_t> blockInput = salt;
        blockInput.push_back(static_cast<uint8_t>((blockIndex >> 24U) & 0xFFU));
        blockInput.push_back(static_cast<uint8_t>((blockIndex >> 16U) & 0xFFU));
        blockInput.push_back(static_cast<uint8_t>((blockIndex >> 8U) & 0xFFU));
        blockInput.push_back(static_cast<uint8_t>(blockIndex & 0xFFU));

        std::vector<uint8_t> u = HmacSha256Bytes(passwordBytes, blockInput);
        std::vector<uint8_t> block = u;
        for (int iteration = 1; iteration < iterations; ++iteration) {
            u = HmacSha256Bytes(passwordBytes, u);
            for (size_t byteIndex = 0; byteIndex < block.size(); ++byteIndex) {
                block[byteIndex] = static_cast<uint8_t>(block[byteIndex] ^ u[byteIndex]);
            }
        }
        derived.insert(derived.end(), block.begin(), block.end());
    }

    derived.resize(outputLength);
    return derived;
}

std::vector<uint8_t> BuildAdminVerifierSalt() {
    std::vector<uint8_t> salt;
    AppendDecodedBytes(&salt, kAdminSaltPartOne, kAdminSaltXorKeyPartOne);
    AppendDecodedBytes(&salt, kAdminSaltPartTwo, kAdminSaltXorKeyPartTwo);
    AppendDecodedBytes(&salt, kAdminSaltPartThree, kAdminSaltXorKeyPartThree);
    return salt;
}

std::vector<uint8_t> BuildExpectedVerifierBytes() {
    std::vector<uint8_t> verifier;
    AppendDecodedBytes(&verifier, kExpectedVerifierPartOne, kExpectedVerifierXorKeyPartOne);
    AppendDecodedBytes(&verifier, kExpectedVerifierPartTwo, kExpectedVerifierXorKeyPartTwo);
    return verifier;
}

std::string MasterKey() {
    return DecodeObfuscated(kMasterKeyPartOne.data(), kMasterKeyPartOne.size(), kMasterKeyXorKey) +
        DecodeObfuscated(kMasterKeyPartTwo.data(), kMasterKeyPartTwo.size(), kMasterKeyXorKey) +
        DecodeObfuscated(kMasterKeyPartThree.data(), kMasterKeyPartThree.size(), kMasterKeyXorKey);
}

std::string VerifierHex(const std::string& candidate) {
    const std::vector<uint8_t> verifier = Pbkdf2HmacSha256(
        candidate,
        BuildAdminVerifierSalt(),
        kAdminPbkdf2Iterations,
        kAdminVerifierLength
    );
    return Sha256::BytesToHexUpper(verifier.data(), verifier.size());
}

std::string ExpectedVerifierHex() {
    static const std::string kExpectedVerifierHex = []() {
        const std::vector<uint8_t> verifier = BuildExpectedVerifierBytes();
        return Sha256::BytesToHexUpper(verifier.data(), verifier.size());
    }();
    return kExpectedVerifierHex;
}

}  // namespace

bool VerifyAdminSecretNative(
    const std::string& packageName,
    const std::string& dataDir,
    const std::string& androidId,
    const std::string& signingFingerprint,
    const std::string& candidate
) {
    if (candidate.empty()) {
        return false;
    }
    const std::string bindingSalt = LowercaseAscii(
        packageName + "|" + dataDir + "|" + androidId + "|" + signingFingerprint
    );
    const std::string masterKey = MasterKey();
    const std::string expected = ComputeHmacSha256HexUpper(
        masterKey,
        bindingSalt + "|" + ExpectedVerifierHex()
    );
    const std::string actual = ComputeHmacSha256HexUpper(
        masterKey,
        bindingSalt + "|" + VerifierHex(candidate)
    );
    return ConstantTimeEquals(expected, actual);
}

}  // namespace examlock_native
