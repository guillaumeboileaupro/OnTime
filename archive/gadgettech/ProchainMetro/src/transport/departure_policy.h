#pragma once
#include "departure.h"
#include <stddef.h>

namespace transport {
inline bool hasText(const char* value) {
  return value && value[0] != '\0';
}

inline bool isValidDeparture(const Departure& d) {
  if (!hasText(d.provider) || !hasText(d.journeyId) || !hasText(d.stopId) ||
      !hasText(d.lineId) || !hasText(d.destination) ||
      d.departureEpochSeconds < 0 || d.fetchedEpochSeconds < 0)
    return false;

  switch (d.mode) {
    case Mode::Train:
    case Mode::Rer:
    case Mode::Metro:
    case Mode::Tram:
      break;
    default:
      return false;
  }
  switch (d.quality) {
    case Quality::Realtime:
    case Quality::Scheduled:
    case Quality::Estimated:
      return true;
    default:
      return false;
  }
}

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

// Available points to an input item. Empty means fresh valid data contains no
// recommendable service. Stale means every valid item is expired. Error means
// invalid arguments or provider data. No status synthesizes a departure.
inline Selection selectNextDeparture(const Departure* departures, size_t count,
                                     long long now, int walkingSeconds,
                                     int marginSeconds, long long maxAgeSeconds,
                                     Status sourceStatus = Status::Available) {
  if (sourceStatus == Status::Error || sourceStatus == Status::Stale)
    return {sourceStatus, nullptr};
  if (sourceStatus == Status::Empty)
    return {Status::Empty, nullptr};
  if ((!departures && count > 0) || now < 0 || walkingSeconds < 0 ||
      marginSeconds < 0 || maxAgeSeconds < 0)
    return {Status::Error, nullptr};

  const Departure* selected = nullptr;
  bool sawFresh = false;
  for (size_t i = 0; i < count; ++i) {
    const Departure& candidate = departures[i];
    if (!isValidDeparture(candidate) || candidate.fetchedEpochSeconds > now)
      return {Status::Error, nullptr};
    if (now - candidate.fetchedEpochSeconds <= maxAgeSeconds)
      sawFresh = true;
    if (canRecommend(candidate, now, walkingSeconds, marginSeconds,
                     maxAgeSeconds) &&
        (!selected || candidate.departureEpochSeconds <
                        selected->departureEpochSeconds))
      selected = &candidate;
  }

  if (selected)
    return {Status::Available, selected};
  if (count == 0 || sawFresh)
    return {Status::Empty, nullptr};
  return {Status::Stale, nullptr};
}
} // namespace transport
