#include "sha256.h"

#include <algorithm>
#include <iomanip>
#include <sstream>

namespace examlock_native {
namespace {
constexpr std::array<uint32_t, 64> kConstants = {
    0x428a2f98U, 0x71374491U, 0xb5c0fbcfU, 0xe9b5dba5U, 0x3956c25bU, 0x59f111f1U, 0x923f82a4U, 0xab1c5ed5U,
    0xd807aa98U, 0x12835b01U, 0x243185beU, 0x550c7dc3U, 0x72be5d74U, 0x80deb1feU, 0x9bdc06a7U, 0xc19bf174U,
    0xe49b69c1U, 0xefbe4786U, 0x0fc19dc6U, 0x240ca1ccU, 0x2de92c6fU, 0x4a7484aaU, 0x5cb0a9dcU, 0x76f988daU,
    0x983e5152U, 0xa831c66dU, 0xb00327c8U, 0xbf597fc7U, 0xc6e00bf3U, 0xd5a79147U, 0x06ca6351U, 0x14292967U,
    0x27b70a85U, 0x2e1b2138U, 0x4d2c6dfcU, 0x53380d13U, 0x650a7354U, 0x766a0abbU, 0x81c2c92eU, 0x92722c85U,
    0xa2bfe8a1U, 0xa81a664bU, 0xc24b8b70U, 0xc76c51a3U, 0xd192e819U, 0xd6990624U, 0xf40e3585U, 0x106aa070U,
    0x19a4c116U, 0x1e376c08U, 0x2748774cU, 0x34b0bcb5U, 0x391c0cb3U, 0x4ed8aa4aU, 0x5b9cca4fU, 0x682e6ff3U,
    0x748f82eeU, 0x78a5636fU, 0x84c87814U, 0x8cc70208U, 0x90befffaU, 0xa4506cebU, 0xbef9a3f7U, 0xc67178f2U,
};

inline uint32_t RotateRight(uint32_t value, uint32_t shift) {
    return (value >> shift) | (value << (32U - shift));
}

inline uint32_t Choose(uint32_t x, uint32_t y, uint32_t z) {
    return (x & y) ^ (~x & z);
}

inline uint32_t Majority(uint32_t x, uint32_t y, uint32_t z) {
    return (x & y) ^ (x & z) ^ (y & z);
}

inline uint32_t BigSigma0(uint32_t x) {
    return RotateRight(x, 2U) ^ RotateRight(x, 13U) ^ RotateRight(x, 22U);
}

inline uint32_t BigSigma1(uint32_t x) {
    return RotateRight(x, 6U) ^ RotateRight(x, 11U) ^ RotateRight(x, 25U);
}

inline uint32_t SmallSigma0(uint32_t x) {
    return RotateRight(x, 7U) ^ RotateRight(x, 18U) ^ (x >> 3U);
}

inline uint32_t SmallSigma1(uint32_t x) {
    return RotateRight(x, 17U) ^ RotateRight(x, 19U) ^ (x >> 10U);
}
}  // namespace

Sha256::Sha256()
    : bit_count_(0),
      state_{0x6a09e667U, 0xbb67ae85U, 0x3c6ef372U, 0xa54ff53aU, 0x510e527fU, 0x9b05688cU, 0x1f83d9abU, 0x5be0cd19U},
      buffer_{},
      buffer_length_(0) {}

void Sha256::Update(const uint8_t* data, size_t length) {
    if (data == nullptr || length == 0U) {
        return;
    }
    bit_count_ += static_cast<uint64_t>(length) * 8ULL;
    size_t offset = 0;
    while (offset < length) {
        const size_t copy = std::min(length - offset, buffer_.size() - buffer_length_);
        std::copy(data + offset, data + offset + copy, buffer_.begin() + static_cast<std::ptrdiff_t>(buffer_length_));
        buffer_length_ += copy;
        offset += copy;
        if (buffer_length_ == buffer_.size()) {
            Transform(buffer_.data());
            buffer_length_ = 0;
        }
    }
}

std::string Sha256::FinalHexUpper() {
    const auto bytes = FinalBytes();
    return BytesToHexUpper(bytes.data(), bytes.size());
}

std::array<uint8_t, 32> Sha256::FinalBytes() {
    std::array<uint8_t, 64> final_block = buffer_;
    size_t final_length = buffer_length_;
    final_block[final_length++] = 0x80U;

    if (final_length > 56U) {
        std::fill(final_block.begin() + static_cast<std::ptrdiff_t>(final_length), final_block.end(), 0U);
        Transform(final_block.data());
        final_block.fill(0U);
        final_length = 0U;
    }

    std::fill(final_block.begin() + static_cast<std::ptrdiff_t>(final_length), final_block.begin() + 56, 0U);
    for (size_t index = 0; index < 8; ++index) {
        final_block[63 - index] = static_cast<uint8_t>((bit_count_ >> (index * 8U)) & 0xFFU);
    }
    Transform(final_block.data());

    std::array<uint8_t, 32> digest{};
    for (size_t index = 0; index < state_.size(); ++index) {
        const uint32_t word = state_[index];
        const size_t base = index * 4U;
        digest[base] = static_cast<uint8_t>((word >> 24U) & 0xFFU);
        digest[base + 1U] = static_cast<uint8_t>((word >> 16U) & 0xFFU);
        digest[base + 2U] = static_cast<uint8_t>((word >> 8U) & 0xFFU);
        digest[base + 3U] = static_cast<uint8_t>(word & 0xFFU);
    }
    return digest;
}

std::string Sha256::BytesToHexUpper(const uint8_t* data, size_t length) {
    std::ostringstream stream;
    stream << std::uppercase << std::hex << std::setfill('0');
    for (size_t index = 0; index < length; ++index) {
        stream << std::setw(2) << static_cast<unsigned int>(data[index]);
    }
    return stream.str();
}

void Sha256::Transform(const uint8_t block[64]) {
    std::array<uint32_t, 64> message_schedule{};
    for (size_t index = 0; index < 16; ++index) {
        const size_t base = index * 4;
        message_schedule[index] =
            (static_cast<uint32_t>(block[base]) << 24U) |
            (static_cast<uint32_t>(block[base + 1]) << 16U) |
            (static_cast<uint32_t>(block[base + 2]) << 8U) |
            (static_cast<uint32_t>(block[base + 3]));
    }
    for (size_t index = 16; index < 64; ++index) {
        message_schedule[index] = SmallSigma1(message_schedule[index - 2]) +
            message_schedule[index - 7] +
            SmallSigma0(message_schedule[index - 15]) +
            message_schedule[index - 16];
    }

    uint32_t a = state_[0];
    uint32_t b = state_[1];
    uint32_t c = state_[2];
    uint32_t d = state_[3];
    uint32_t e = state_[4];
    uint32_t f = state_[5];
    uint32_t g = state_[6];
    uint32_t h = state_[7];

    for (size_t index = 0; index < 64; ++index) {
        const uint32_t t1 = h + BigSigma1(e) + Choose(e, f, g) + kConstants[index] + message_schedule[index];
        const uint32_t t2 = BigSigma0(a) + Majority(a, b, c);
        h = g;
        g = f;
        f = e;
        e = d + t1;
        d = c;
        c = b;
        b = a;
        a = t1 + t2;
    }

    state_[0] += a;
    state_[1] += b;
    state_[2] += c;
    state_[3] += d;
    state_[4] += e;
    state_[5] += f;
    state_[6] += g;
    state_[7] += h;
}

}  // namespace examlock_native
