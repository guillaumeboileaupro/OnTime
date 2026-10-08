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
}
