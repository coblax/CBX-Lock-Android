#include "examlock_native.h"

#include <algorithm>
#include <cctype>
#include <fstream>
#include <set>
#include <sstream>
#include <string>
#include <vector>

namespace examlock_native {
namespace {
std::string ToLowerAscii(const std::string& value) {
    std::string result = value;
    std::transform(result.begin(), result.end(), result.begin(), [](unsigned char ch) {
        return static_cast<char>(std::tolower(ch));
    });
    return result;
}
}  // namespace

int ReadTracerPidNative() {
    std::ifstream statusFile("/proc/self/status");
    if (!statusFile.is_open()) {
        return 0;
    }
    std::string line;
    while (std::getline(statusFile, line)) {
        if (line.rfind("TracerPid:", 0) == 0) {
            std::istringstream stream(line.substr(10));
            int tracerPid = 0;
            stream >> tracerPid;
            return tracerPid;
        }
    }
    return 0;
}

std::vector<std::string> ScanProcMapsNative(const std::vector<std::string>& markers) {
    std::ifstream mapsFile("/proc/self/maps");
    if (!mapsFile.is_open()) {
        return {};
    }
    std::vector<std::string> lowerMarkers;
    lowerMarkers.reserve(markers.size());
    for (const auto& marker : markers) {
        lowerMarkers.push_back(ToLowerAscii(marker));
    }

    std::set<size_t> foundIndexes;
    std::string line;
    while (std::getline(mapsFile, line)) {
        std::string lowerLine = ToLowerAscii(line);
        for (size_t index = 0; index < lowerMarkers.size(); ++index) {
            if (foundIndexes.count(index) == 0U && lowerLine.find(lowerMarkers[index]) != std::string::npos) {
                foundIndexes.insert(index);
            }
        }
        if (foundIndexes.size() == markers.size()) {
            break;
        }
    }

    std::vector<std::string> results;
    results.reserve(foundIndexes.size());
    for (size_t index = 0; index < markers.size(); ++index) {
        if (foundIndexes.count(index) != 0U) {
            results.push_back(markers[index]);
        }
    }
    return results;
}

}  // namespace examlock_native
