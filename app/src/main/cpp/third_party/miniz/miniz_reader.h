#pragma once

#include <string>

namespace examlock_native::miniz_reader {

bool ComputeCombinedDexSha256(const std::string& apkPath, std::string* outHexHash);

}  // namespace examlock_native::miniz_reader
