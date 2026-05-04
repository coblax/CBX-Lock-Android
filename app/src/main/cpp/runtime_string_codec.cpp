#include "examlock_native.h"

#include <algorithm>
#include <array>
#include <cctype>
#include <cstdint>
#include <string>
#include <vector>

namespace examlock_native {
namespace {

constexpr std::array<uint8_t, 4> kKeyFragments = {
    static_cast<uint8_t>(0x19U),
    static_cast<uint8_t>(0x2FU),
    static_cast<uint8_t>(0x43U),
    static_cast<uint8_t>(0x06U),
};

uint8_t ResolveRuntimeStringXorKey() {
    uint8_t key = 0U;
    for (const uint8_t fragment : kKeyFragments) {
        key = static_cast<uint8_t>(key ^ fragment);
    }
    return key;
}

int DecodeBase64Char(const char value) {
    if (value >= 'A' && value <= 'Z') {
        return value - 'A';
    }
    if (value >= 'a' && value <= 'z') {
        return value - 'a' + 26;
    }
    if (value >= '0' && value <= '9') {
        return value - '0' + 52;
    }
    if (value == '+') {
        return 62;
    }
    if (value == '/') {
        return 63;
    }
    return -1;
}

std::vector<uint8_t> DecodeBase64(const std::string& input) {
    std::string normalized;
    normalized.reserve(input.size());
    for (const char value : input) {
        if (std::isspace(static_cast<unsigned char>(value)) == 0) {
            normalized.push_back(value);
        }
    }

    if (normalized.empty()) {
        return {};
    }
    if ((normalized.size() % 4U) != 0U) {
        return {};
    }

    std::vector<uint8_t> output;
    output.reserve((normalized.size() / 4U) * 3U);
    for (size_t index = 0; index < normalized.size(); index += 4U) {
        const char c0 = normalized[index];
        const char c1 = normalized[index + 1U];
        const char c2 = normalized[index + 2U];
        const char c3 = normalized[index + 3U];

        const int b0 = DecodeBase64Char(c0);
        const int b1 = DecodeBase64Char(c1);
        const int b2 = (c2 == '=') ? 0 : DecodeBase64Char(c2);
        const int b3 = (c3 == '=') ? 0 : DecodeBase64Char(c3);

        if (b0 < 0 || b1 < 0 || (c2 != '=' && b2 < 0) || (c3 != '=' && b3 < 0)) {
            return {};
        }
        if (c2 == '=' && c3 != '=') {
            return {};
        }
        if ((c2 == '=' || c3 == '=') && (index + 4U) != normalized.size()) {
            return {};
        }

        const uint32_t block =
            (static_cast<uint32_t>(b0) << 18U) |
            (static_cast<uint32_t>(b1) << 12U) |
            (static_cast<uint32_t>(b2) << 6U) |
            static_cast<uint32_t>(b3);

        output.push_back(static_cast<uint8_t>((block >> 16U) & 0xFFU));
        if (c2 != '=') {
            output.push_back(static_cast<uint8_t>((block >> 8U) & 0xFFU));
        }
        if (c3 != '=') {
            output.push_back(static_cast<uint8_t>(block & 0xFFU));
        }
    }
    return output;
}

}  // namespace

std::string DecodeBase64XorNative(const std::string& obfuscated) {
    if (obfuscated.empty()) {
        return std::string();
    }

    std::vector<uint8_t> bytes = DecodeBase64(obfuscated);
    if (bytes.empty() && !obfuscated.empty()) {
        const bool all_whitespace = std::all_of(
            obfuscated.begin(),
            obfuscated.end(),
            [](const char value) { return std::isspace(static_cast<unsigned char>(value)) != 0; }
        );
        if (!all_whitespace) {
            return std::string();
        }
    }

    const uint8_t xor_key = ResolveRuntimeStringXorKey();
    for (uint8_t& value : bytes) {
        value = static_cast<uint8_t>(value ^ xor_key);
    }
    return std::string(bytes.begin(), bytes.end());
}

}  // namespace examlock_native
