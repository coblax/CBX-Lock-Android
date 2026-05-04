#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <string>

namespace examlock_native {

class Sha256 {
public:
    Sha256();
    void Update(const uint8_t* data, size_t length);
    std::array<uint8_t, 32> FinalBytes();
    std::string FinalHexUpper();
    static std::string BytesToHexUpper(const uint8_t* data, size_t length);

private:
    void Transform(const uint8_t block[64]);

    uint64_t bit_count_;
    std::array<uint32_t, 8> state_;
    std::array<uint8_t, 64> buffer_;
    size_t buffer_length_;
};

}  // namespace examlock_native
