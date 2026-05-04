#include "examlock_native.h"

#include <algorithm>
#include <cmath>
#include <limits>
#include <utility>
#include <vector>

namespace examlock_native {
namespace {
constexpr double kOrientationEpsilon = 1e-12;
constexpr double kSegmentEpsilon = 1e-10;
constexpr double kEarthRadiusMeters = 6371000.0;

struct Point {
    double latitude;
    double longitude;
};

Point MakePoint(const std::vector<double>& latitudes, const std::vector<double>& longitudes, size_t index) {
    return Point{latitudes[index], longitudes[index]};
}

int Orientation(const Point& first, const Point& second, const Point& third) {
    const double value = (second.longitude - first.longitude) * (third.latitude - second.latitude) -
        (second.latitude - first.latitude) * (third.longitude - second.longitude);
    if (std::abs(value) < kOrientationEpsilon) {
        return 0;
    }
    return value > 0.0 ? 1 : 2;
}

bool IsPointOnSegment(const Point& point, const Point& segmentStart, const Point& segmentEnd) {
    const double cross = (point.latitude - segmentStart.latitude) * (segmentEnd.longitude - segmentStart.longitude) -
        (point.longitude - segmentStart.longitude) * (segmentEnd.latitude - segmentStart.latitude);
    if (std::abs(cross) > kSegmentEpsilon) {
        return false;
    }
    return point.latitude >= std::min(segmentStart.latitude, segmentEnd.latitude) - kSegmentEpsilon &&
        point.latitude <= std::max(segmentStart.latitude, segmentEnd.latitude) + kSegmentEpsilon &&
        point.longitude >= std::min(segmentStart.longitude, segmentEnd.longitude) - kSegmentEpsilon &&
        point.longitude <= std::max(segmentStart.longitude, segmentEnd.longitude) + kSegmentEpsilon;
}

bool SegmentsIntersect(const Point& firstStart, const Point& firstEnd, const Point& secondStart, const Point& secondEnd) {
    const int o1 = Orientation(firstStart, firstEnd, secondStart);
    const int o2 = Orientation(firstStart, firstEnd, secondEnd);
    const int o3 = Orientation(secondStart, secondEnd, firstStart);
    const int o4 = Orientation(secondStart, secondEnd, firstEnd);

    if (o1 != o2 && o3 != o4) {
        return true;
    }
    return (o1 == 0 && IsPointOnSegment(secondStart, firstStart, firstEnd)) ||
        (o2 == 0 && IsPointOnSegment(secondEnd, firstStart, firstEnd)) ||
        (o3 == 0 && IsPointOnSegment(firstStart, secondStart, secondEnd)) ||
        (o4 == 0 && IsPointOnSegment(firstEnd, secondStart, secondEnd));
}

std::pair<double, double> ToPlanarMeters(const Point& point, double referenceLat, double referenceLng) {
    const double latitudeRadians = point.latitude * M_PI / 180.0;
    const double referenceLatRadians = referenceLat * M_PI / 180.0;
    const double deltaLatRadians = latitudeRadians - referenceLatRadians;
    const double deltaLngRadians = (point.longitude - referenceLng) * M_PI / 180.0;
    const double x = deltaLngRadians * std::cos((latitudeRadians + referenceLatRadians) / 2.0) * kEarthRadiusMeters;
    const double y = deltaLatRadians * kEarthRadiusMeters;
    return {x, y};
}

Point MakeClosestPoint(double lat, double lng) {
    return Point{lat, lng};
}

double HaversineDistanceMeters(const Point& first, const Point& second) {
    const double firstLat = first.latitude * M_PI / 180.0;
    const double secondLat = second.latitude * M_PI / 180.0;
    const double deltaLat = (second.latitude - first.latitude) * M_PI / 180.0;
    const double deltaLng = (second.longitude - first.longitude) * M_PI / 180.0;
    const double sinLat = std::sin(deltaLat / 2.0);
    const double sinLng = std::sin(deltaLng / 2.0);
    const double a = sinLat * sinLat + std::cos(firstLat) * std::cos(secondLat) * sinLng * sinLng;
    const double c = 2.0 * std::atan2(std::sqrt(a), std::sqrt(std::max(0.0, 1.0 - a)));
    return kEarthRadiusMeters * c;
}
}  // namespace

bool IsPointInPolygonNative(double pointLat, double pointLng, const std::vector<double>& polygonLatitudes, const std::vector<double>& polygonLongitudes) {
    if (polygonLatitudes.size() != polygonLongitudes.size() || polygonLatitudes.size() < 3U) {
        return false;
    }
    const Point point{pointLat, pointLng};
    bool inside = false;
    size_t previousIndex = polygonLatitudes.size() - 1U;
    for (size_t currentIndex = 0; currentIndex < polygonLatitudes.size(); ++currentIndex) {
        const Point current = MakePoint(polygonLatitudes, polygonLongitudes, currentIndex);
        const Point previous = MakePoint(polygonLatitudes, polygonLongitudes, previousIndex);
        if (IsPointOnSegment(point, previous, current)) {
            return true;
        }
        const bool intersects = ((current.latitude > point.latitude) != (previous.latitude > point.latitude)) &&
            (point.longitude < (previous.longitude - current.longitude) *
                (point.latitude - current.latitude) /
                (previous.latitude - current.latitude) + current.longitude);
        if (intersects) {
            inside = !inside;
        }
        previousIndex = currentIndex;
    }
    return inside;
}

bool IsSelfIntersectingPolygonNative(const std::vector<double>& polygonLatitudes, const std::vector<double>& polygonLongitudes) {
    if (polygonLatitudes.size() != polygonLongitudes.size() || polygonLatitudes.size() < 4U) {
        return false;
    }
    for (size_t firstIndex = 0; firstIndex < polygonLatitudes.size(); ++firstIndex) {
        const Point firstStart = MakePoint(polygonLatitudes, polygonLongitudes, firstIndex);
        const Point firstEnd = MakePoint(polygonLatitudes, polygonLongitudes, (firstIndex + 1U) % polygonLatitudes.size());
        for (size_t secondIndex = firstIndex + 1U; secondIndex < polygonLatitudes.size(); ++secondIndex) {
            const bool adjacent =
                secondIndex == firstIndex ||
                secondIndex == (firstIndex + 1U) % polygonLatitudes.size() ||
                firstIndex == (secondIndex + 1U) % polygonLatitudes.size();
            if (adjacent) {
                continue;
            }
            const Point secondStart = MakePoint(polygonLatitudes, polygonLongitudes, secondIndex);
            const Point secondEnd = MakePoint(polygonLatitudes, polygonLongitudes, (secondIndex + 1U) % polygonLatitudes.size());
            if (SegmentsIntersect(firstStart, firstEnd, secondStart, secondEnd)) {
                return true;
            }
        }
    }
    return false;
}

double DistancePointToSegmentMetersNative(double pointLat, double pointLng, double startLat, double startLng, double endLat, double endLng) {
    const Point point{pointLat, pointLng};
    const Point segmentStart{startLat, startLng};
    const Point segmentEnd{endLat, endLng};
    const auto pointXY = ToPlanarMeters(point, point.latitude, point.longitude);
    const auto startXY = ToPlanarMeters(segmentStart, point.latitude, point.longitude);
    const auto endXY = ToPlanarMeters(segmentEnd, point.latitude, point.longitude);
    const double segmentDx = endXY.first - startXY.first;
    const double segmentDy = endXY.second - startXY.second;
    const double segmentLengthSquared = segmentDx * segmentDx + segmentDy * segmentDy;
    if (segmentLengthSquared <= 1e-6) {
        const double dx = pointXY.first - startXY.first;
        const double dy = pointXY.second - startXY.second;
        return std::sqrt(dx * dx + dy * dy);
    }
    const double projection = ((pointXY.first - startXY.first) * segmentDx + (pointXY.second - startXY.second) * segmentDy) / segmentLengthSquared;
    const double clampedProjection = std::min(1.0, std::max(0.0, projection));
    const double projectedX = startXY.first + clampedProjection * segmentDx;
    const double projectedY = startXY.second + clampedProjection * segmentDy;
    const double dx = pointXY.first - projectedX;
    const double dy = pointXY.second - projectedY;
    return std::sqrt(dx * dx + dy * dy);
}

double ClosestCircleDistanceMetersNative(double locationLat, double locationLng, const std::vector<double>& centerLatitudes, const std::vector<double>& centerLongitudes) {
    if (centerLatitudes.empty() || centerLatitudes.size() != centerLongitudes.size()) {
        return std::numeric_limits<double>::quiet_NaN();
    }
    const Point location{locationLat, locationLng};
    double best = std::numeric_limits<double>::infinity();
    for (size_t index = 0; index < centerLatitudes.size(); ++index) {
        const double distance = HaversineDistanceMeters(location, MakePoint(centerLatitudes, centerLongitudes, index));
        if (distance < best) {
            best = distance;
        }
    }
    return std::isfinite(best) ? best : std::numeric_limits<double>::quiet_NaN();
}

}  // namespace examlock_native
