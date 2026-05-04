#include "miniz_reader.h"

#include "../../sha256.h"

#include <algorithm>
#include <cstdint>
#include <fstream>
#include <limits>
#include <string>
#include <vector>
#include <zlib.h>

namespace examlock_native::miniz_reader {
namespace {
struct ZipEntryInfo {
    std::string name;
    uint16_t compression_method;
    uint32_t compressed_size;
    uint32_t uncompressed_size;
    uint32_t local_header_offset;
};

constexpr uint32_t kEocdSignature = 0x06054b50U;
constexpr uint32_t kCentralDirectorySignature = 0x02014b50U;
constexpr uint32_t kLocalHeaderSignature = 0x04034b50U;

uint16_t ReadUInt16(const std::vector<uint8_t>& data, size_t offset) {
    return static_cast<uint16_t>(data[offset]) |
        (static_cast<uint16_t>(data[offset + 1]) << 8U);
}

uint32_t ReadUInt32(const std::vector<uint8_t>& data, size_t offset) {
    return static_cast<uint32_t>(data[offset]) |
        (static_cast<uint32_t>(data[offset + 1]) << 8U) |
        (static_cast<uint32_t>(data[offset + 2]) << 16U) |
        (static_cast<uint32_t>(data[offset + 3]) << 24U);
}

bool ReadFileBytes(const std::string& path, std::vector<uint8_t>* outBytes) {
    std::ifstream stream(path, std::ios::binary | std::ios::ate);
    if (!stream.is_open()) {
        return false;
    }
    const auto fileSize = stream.tellg();
    if (fileSize <= 0) {
        return false;
    }
    if (fileSize > static_cast<std::streamoff>(std::numeric_limits<size_t>::max())) {
        return false;
    }
    outBytes->resize(static_cast<size_t>(fileSize));
    stream.seekg(0, std::ios::beg);
    stream.read(reinterpret_cast<char*>(outBytes->data()), fileSize);
    return stream.good();
}

bool FindEndOfCentralDirectory(const std::vector<uint8_t>& zipBytes, size_t* outOffset) {
    if (zipBytes.size() < 22U) {
        return false;
    }
    const size_t minSearch = (zipBytes.size() > (22U + 0xFFFFU)) ? zipBytes.size() - (22U + 0xFFFFU) : 0U;
    for (size_t cursor = zipBytes.size() - 22U + 1U; cursor-- > minSearch;) {
        if (ReadUInt32(zipBytes, cursor) == kEocdSignature) {
            *outOffset = cursor;
            return true;
        }
        if (cursor == 0U) {
            break;
        }
    }
    return false;
}

bool ParseDexEntries(const std::vector<uint8_t>& zipBytes, std::vector<ZipEntryInfo>* outEntries) {
    size_t eocdOffset = 0U;
    if (!FindEndOfCentralDirectory(zipBytes, &eocdOffset)) {
        return false;
    }

    const uint32_t centralDirectorySize = ReadUInt32(zipBytes, eocdOffset + 12U);
    const uint32_t centralDirectoryOffset = ReadUInt32(zipBytes, eocdOffset + 16U);
    if (centralDirectoryOffset + centralDirectorySize > zipBytes.size()) {
        return false;
    }

    size_t cursor = centralDirectoryOffset;
    const size_t end = centralDirectoryOffset + centralDirectorySize;
    while (cursor + 46U <= end) {
        if (ReadUInt32(zipBytes, cursor) != kCentralDirectorySignature) {
            return false;
        }
        const uint16_t compressionMethod = ReadUInt16(zipBytes, cursor + 10U);
        const uint32_t compressedSize = ReadUInt32(zipBytes, cursor + 20U);
        const uint32_t uncompressedSize = ReadUInt32(zipBytes, cursor + 24U);
        const uint16_t fileNameLength = ReadUInt16(zipBytes, cursor + 28U);
        const uint16_t extraLength = ReadUInt16(zipBytes, cursor + 30U);
        const uint16_t commentLength = ReadUInt16(zipBytes, cursor + 32U);
        const uint32_t localHeaderOffset = ReadUInt32(zipBytes, cursor + 42U);
        const size_t recordSize = 46U + fileNameLength + extraLength + commentLength;
        if (cursor + recordSize > end) {
            return false;
        }
        const std::string entryName(
            reinterpret_cast<const char*>(zipBytes.data() + cursor + 46U),
            fileNameLength
        );
        if (entryName.size() >= 4U && entryName.compare(entryName.size() - 4U, 4U, ".dex") == 0) {
            outEntries->push_back(ZipEntryInfo{
                entryName,
                compressionMethod,
                compressedSize,
                uncompressedSize,
                localHeaderOffset,
            });
        }
        cursor += recordSize;
    }

    std::sort(outEntries->begin(), outEntries->end(), [](const ZipEntryInfo& left, const ZipEntryInfo& right) {
        return left.name < right.name;
    });
    return !outEntries->empty();
}

bool UpdateDigestWithEntry(
    const std::vector<uint8_t>& zipBytes,
    const ZipEntryInfo& entry,
    Sha256* digest
) {
    if (entry.local_header_offset + 30U > zipBytes.size()) {
        return false;
    }
    if (ReadUInt32(zipBytes, entry.local_header_offset) != kLocalHeaderSignature) {
        return false;
    }
    const uint16_t fileNameLength = ReadUInt16(zipBytes, entry.local_header_offset + 26U);
    const uint16_t extraLength = ReadUInt16(zipBytes, entry.local_header_offset + 28U);
    const size_t dataOffset = entry.local_header_offset + 30U + fileNameLength + extraLength;
    if (dataOffset + entry.compressed_size > zipBytes.size()) {
        return false;
    }

    const uint8_t* compressedData = zipBytes.data() + dataOffset;
    if (entry.compression_method == 0U) {
        digest->Update(compressedData, entry.compressed_size);
        return true;
    }
    if (entry.compression_method != 8U) {
        return false;
    }

    z_stream stream{};
    stream.next_in = const_cast<Bytef*>(reinterpret_cast<const Bytef*>(compressedData));
    stream.avail_in = entry.compressed_size;
    if (inflateInit2(&stream, -MAX_WBITS) != Z_OK) {
        return false;
    }

    std::vector<uint8_t> buffer(16U * 1024U);
    int inflateResult = Z_OK;
    while (inflateResult == Z_OK) {
        stream.next_out = reinterpret_cast<Bytef*>(buffer.data());
        stream.avail_out = static_cast<uInt>(buffer.size());
        inflateResult = inflate(&stream, Z_NO_FLUSH);
        if (inflateResult != Z_OK && inflateResult != Z_STREAM_END) {
            inflateEnd(&stream);
            return false;
        }
        const size_t produced = buffer.size() - stream.avail_out;
        if (produced > 0U) {
            digest->Update(buffer.data(), produced);
        }
    }
    inflateEnd(&stream);
    return inflateResult == Z_STREAM_END;
}
}  // namespace

bool ComputeCombinedDexSha256(const std::string& apkPath, std::string* outHexHash) {
    if (outHexHash == nullptr) {
        return false;
    }

    std::vector<uint8_t> zipBytes;
    if (!ReadFileBytes(apkPath, &zipBytes)) {
        return false;
    }

    std::vector<ZipEntryInfo> dexEntries;
    if (!ParseDexEntries(zipBytes, &dexEntries)) {
        return false;
    }

    Sha256 digest;
    for (const ZipEntryInfo& entry : dexEntries) {
        if (!UpdateDigestWithEntry(zipBytes, entry, &digest)) {
            return false;
        }
    }

    *outHexHash = digest.FinalHexUpper();
    return true;
}

}  // namespace examlock_native::miniz_reader
