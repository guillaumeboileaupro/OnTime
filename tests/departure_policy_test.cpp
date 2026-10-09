#include "../ProchainMetro/src/transport/departure_policy.h"
#include <cassert>

int main() {
  using namespace transport;
  const int unsorted[] = {14, 3, 8, 6, -1};
  assert(firstReachable(unsorted, 5, 6) == 6);
  assert(firstReachable(unsorted, 5, 7) == 8);
  assert(firstReachable(unsorted, 5, 15) == -1);
  assert(firstReachable(nullptr, 0, 5) == -1);
  assert(firstReachable(unsorted, 5, -1) == -1);
  const int missed[] = {1, 2};
  assert(firstReachable(missed, 2, 5) == -1); // No invented third service.
  Departure d{"idfm", "journey-1", "stop-1", "line-1", "Terminus", Mode::Rer,
              86460, 86340, Quality::Realtime, false};
  assert(canRecommend(d, 86390, 60, 10, 120)); // Across midnight, exact boundary.
  assert(!canRecommend(d, 86390, 60, 11, 120));
  assert(!canRecommend(d, 86390, 60, 10, 49));
  d.cancelled = true;
  assert(!canRecommend(d, 86390, 0, 0, 120));
  d.cancelled = false; d.quality = Quality::Estimated;
  assert(!canRecommend(d, 86390, 0, 0, 120));
  d.quality = Quality::Scheduled;
  assert(canRecommend(d, 86390, 0, 0, 120)); // Label scheduled in UI.
  d.fetchedEpochSeconds = 86400;
  assert(!canRecommend(d, 86390, 0, 0, 120));

  const Departure departures[] = {
    {"idfm", "journey-late", "stop-1", "line-1", "Terminus", Mode::Rer,
     87000, 86380, Quality::Realtime, false},
    {"idfm", "journey-cancelled", "stop-1", "line-1", "Terminus", Mode::Rer,
     86500, 86380, Quality::Realtime, true},
    {"idfm", "journey-first", "stop-1", "line-1", "Terminus", Mode::Rer,
     86600, 86380, Quality::Scheduled, false},
  };
  Selection selected = selectNextDeparture(departures, 3, 86400, 60, 20, 120);
  assert(selected.status == Status::Available);
  assert(selected.departure == &departures[2]);

  const Departure missedDepartures[] = {
    {"idfm", "journey-missed-1", "stop-1", "line-1", "Terminus", Mode::Rer,
     86410, 86380, Quality::Realtime, false},
    {"idfm", "journey-missed-2", "stop-1", "line-1", "Terminus", Mode::Rer,
     86420, 86380, Quality::Realtime, false},
  };
  selected = selectNextDeparture(missedDepartures, 2, 86400, 60, 0, 120);
  assert(selected.status == Status::Empty);
  assert(selected.departure == nullptr); // No invented service after known ones.

  Departure stale = departures[0];
  stale.fetchedEpochSeconds = 86000;
  selected = selectNextDeparture(&stale, 1, 86400, 0, 0, 120);
  assert(selected.status == Status::Stale);
  assert(selected.departure == nullptr);

  const Departure mixedFreshness[] = {
    stale,
    {"idfm", "journey-fresh-missed", "stop-1", "line-1", "Terminus",
     Mode::Rer, 86390, 86390, Quality::Realtime, false},
  };
  selected = selectNextDeparture(mixedFreshness, 2, 86400, 0, 0, 120);
  assert(selected.status == Status::Empty); // Fresh data takes precedence.
  assert(selected.departure == nullptr);

  Departure allCancelled[] = {departures[0], departures[2]};
  allCancelled[0].cancelled = true;
  allCancelled[1].cancelled = true;
  selected = selectNextDeparture(allCancelled, 2, 86400, 0, 0, 120);
  assert(selected.status == Status::Empty);
  assert(selected.departure == nullptr);

  Departure estimated = departures[0];
  estimated.quality = Quality::Estimated;
  selected = selectNextDeparture(&estimated, 1, 86400, 0, 0, 120);
  assert(selected.status == Status::Empty);
  assert(selected.departure == nullptr);

  Departure futureFetch = departures[0];
  futureFetch.fetchedEpochSeconds = 86401;
  selected = selectNextDeparture(&futureFetch, 1, 86400, 0, 0, 120);
  assert(selected.status == Status::Error);
  assert(selected.departure == nullptr);

  Departure exactThreshold = departures[0];
  exactThreshold.departureEpochSeconds = 86490;
  selected = selectNextDeparture(&exactThreshold, 1, 86400, 60, 30, 120);
  assert(selected.status == Status::Available);
  assert(selected.departure == &exactThreshold);
  exactThreshold.departureEpochSeconds = 86489;
  selected = selectNextDeparture(&exactThreshold, 1, 86400, 60, 30, 120);
  assert(selected.status == Status::Empty);
  assert(selected.departure == nullptr);

  selected = selectNextDeparture(nullptr, 0, 86400, 0, 0, 120);
  assert(selected.status == Status::Empty);
  assert(selected.departure == nullptr);
  selected = selectNextDeparture(nullptr, 1, 86400, 0, 0, 120);
  assert(selected.status == Status::Error);
  assert(selected.departure == nullptr);
  selected = selectNextDeparture(nullptr, 0, 86400, 0, 0, 120, Status::Error);
  assert(selected.status == Status::Error);
  assert(selected.departure == nullptr);
  selected = selectNextDeparture(departures, 3, 86400, -1, 0, 120);
  assert(selected.status == Status::Error);
  assert(selected.departure == nullptr);

  Departure missingIdentity = departures[0];
  missingIdentity.provider = nullptr;
  selected = selectNextDeparture(&missingIdentity, 1, 86400, 0, 0, 120);
  assert(selected.status == Status::Error);
  assert(selected.departure == nullptr);
}
