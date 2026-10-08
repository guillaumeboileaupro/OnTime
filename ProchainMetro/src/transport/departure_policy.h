#pragma once
#include <stddef.h>

namespace transport {
// Minutes are supplied by the provider; never extrapolate a new service.
inline int firstReachable(const int* minutes, size_t count, int walkingMinutes) {
  if (!minutes || walkingMinutes < 0) return -1;
  int best = -1;
  for (size_t i = 0; i < count; ++i) {
    if (minutes[i] >= walkingMinutes && (best < 0 || minutes[i] < best))
      best = minutes[i];
  }
  return best;
}

enum class Mode { Train, Rer, Metro, Tram };
enum class Quality { Realtime, Scheduled, Estimated };
enum class Status { Available, Empty, Stale, Error };

// Provider-qualified IDs: IDFM and SNCF identifiers must not be interchanged.
struct Departure {
  const char* provider;
  const char* journeyId;
  const char* stopId;
  const char* lineId;
  const char* destination;
  Mode mode;
  long long departureEpochSeconds;
  long long fetchedEpochSeconds;
  Quality quality;
  bool cancelled;
};

// Epoch seconds handle date changes; freshness is an explicit product policy.
inline bool canRecommend(const Departure& d, long long now, int walkingSeconds,
                         int marginSeconds, long long maxAgeSeconds) {
  if (walkingSeconds < 0 || marginSeconds < 0 || maxAgeSeconds < 0 || d.cancelled)
    return false;
  if (d.quality == Quality::Estimated || d.fetchedEpochSeconds > now)
    return false;
  return now - d.fetchedEpochSeconds <= maxAgeSeconds &&
    d.departureEpochSeconds >= now + walkingSeconds + marginSeconds;
}
} // namespace transport
