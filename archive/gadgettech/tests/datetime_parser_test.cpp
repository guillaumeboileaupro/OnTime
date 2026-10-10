#include "../ProchainMetro/src/transport/datetime_parser.h"
#include "fixtures/datetime_cases.h"

#include <cassert>
#include <stddef.h>

int main() {
  using namespace transport;

  DateTimeResult epoch = parseDateTime("1970-01-01T00:00:00Z");
  assert(epoch.ok);
  assert(epoch.epochSeconds == 0);
  assert(epoch.error == DateTimeError::None);

  DateTimeResult beforeEpoch = parseDateTime("1969-12-31T23:59:59Z");
  assert(beforeEpoch.ok);
  assert(beforeEpoch.epochSeconds == -1);

  DateTimeResult leapDay = parseDateTime("2024-02-29T12:34:56Z");
  assert(leapDay.ok);
  assert(leapDay.epochSeconds == 1709210096);
  DateTimeResult leapCentury = parseDateTime("2000-02-29T12:34:56Z");
  assert(leapCentury.ok);
  assert(leapCentury.epochSeconds == 951827696);

  for (size_t i = 0;
       i < sizeof(kEquivalentDateTimes) / sizeof(kEquivalentDateTimes[0]); ++i) {
    const DateTimeResult first = parseDateTime(kEquivalentDateTimes[i].first);
    const DateTimeResult second = parseDateTime(kEquivalentDateTimes[i].second);
    assert(first.ok);
    assert(second.ok);
    assert(first.epochSeconds == second.epochSeconds);
  }

  const DateTimeResult autumnSummer =
    parseDateTime("2026-10-25T02:30:00+02:00");
  const DateTimeResult autumnWinter =
    parseDateTime("2026-10-25T02:30:00+01:00");
  assert(autumnSummer.ok && autumnWinter.ok);
  assert(autumnWinter.epochSeconds == 1792891800);
  assert(autumnWinter.epochSeconds - autumnSummer.epochSeconds == 3600);

  const DateTimeResult springBefore =
    parseDateTime("2026-03-29T01:30:00+01:00");
  assert(springBefore.ok);
  assert(springBefore.epochSeconds == 1774744200);

  for (size_t i = 0;
       i < sizeof(kInvalidDateTimes) / sizeof(kInvalidDateTimes[0]); ++i) {
    const DateTimeResult result = parseDateTime(kInvalidDateTimes[i].value);
    assert(!result.ok);
    assert(result.epochSeconds == 0); // An error never exposes an invented time.
    assert(result.error == kInvalidDateTimes[i].error);
  }
}
