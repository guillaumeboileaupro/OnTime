#pragma once
#include "departure.h"
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

// Selects only among services supplied by the provider. The returned pointer
// always refers to an input item; Empty and Stale never synthesize a departure.
inline Selection selectNextDeparture(const Departure* departures, size_t count,
                                     long long now, int walkingSeconds,
                                     int marginSeconds, long long maxAgeSeconds,
                                     Status sourceStatus = Status::Available) {
  if (sourceStatus == Status::Error || sourceStatus == Status::Stale)
    return {sourceStatus, nullptr};
  if (sourceStatus == Status::Empty)
    return {Status::Empty, nullptr};
  if ((!departures && count > 0) || walkingSeconds < 0 || marginSeconds < 0 ||
      maxAgeSeconds < 0)
    return {Status::Error, nullptr};

  const Departure* selected = nullptr;
  bool sawStale = false;
  for (size_t i = 0; i < count; ++i) {
    const Departure& candidate = departures[i];
    if (candidate.fetchedEpochSeconds <= now &&
        now - candidate.fetchedEpochSeconds > maxAgeSeconds)
      sawStale = true;
    if (canRecommend(candidate, now, walkingSeconds, marginSeconds,
                     maxAgeSeconds) &&
        (!selected || candidate.departureEpochSeconds <
                        selected->departureEpochSeconds))
      selected = &candidate;
  }

  if (selected)
    return {Status::Available, selected};
  return {sawStale ? Status::Stale : Status::Empty, nullptr};
}
} // namespace transport
