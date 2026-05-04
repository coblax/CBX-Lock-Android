#include "examlock_native.h"
#include "third_party/miniz/miniz_reader.h"

#include <sys/system_properties.h>

namespace examlock_native {

std::string ReadDexHashNative(const std::string& apkPath) {
    std::string hexHash;
    if (!miniz_reader::ComputeCombinedDexSha256(apkPath, &hexHash)) {
        return std::string();
    }
    return hexHash;
}

std::string ReadSystemPropertyNative(const std::string& key) {
    char value[PROP_VALUE_MAX] = {0};
    const int length = __system_property_get(key.c_str(), value);
    if (length <= 0) {
        return std::string();
    }
    return std::string(value, static_cast<size_t>(length));
}

}  // namespace examlock_native
