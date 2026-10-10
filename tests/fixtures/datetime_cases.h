#pragma once

struct EquivalentDateTimeFixture {
  const char* name;
  const char* first;
  const char* second;
};

static const EquivalentDateTimeFixture kEquivalentDateTimes[] = {
  {"midnight and date change", "2026-01-01T00:15:00+01:00",
   "2025-12-31T23:15:00Z"},
  {"same instant with a negative offset", "2026-07-01T12:00:00-04:00",
   "2026-07-01T16:00:00Z"},
  {"fractional seconds preserve the containing second",
   "2026-07-01T16:00:00.123Z", "2026-07-01T18:00:00.999+02:00"},
  {"Paris spring transition before jump", "2026-03-29T01:30:00+01:00",
   "2026-03-29T00:30:00Z"},
  {"Paris spring transition after jump", "2026-03-29T03:30:00+02:00",
   "2026-03-29T01:30:00Z"},
  {"Paris autumn first 02:30", "2026-10-25T02:30:00+02:00",
   "2026-10-25T00:30:00Z"},
  {"Paris autumn second 02:30", "2026-10-25T02:30:00+01:00",
   "2026-10-25T01:30:00Z"},
};

struct InvalidDateTimeFixture {
  const char* value;
  transport::DateTimeError error;
};

static const InvalidDateTimeFixture kInvalidDateTimes[] = {
  {nullptr, transport::DateTimeError::InvalidFormat},
  {"", transport::DateTimeError::InvalidFormat},
  {"2026-01-01 00:00:00Z", transport::DateTimeError::InvalidFormat},
  {"2026-01-01T00:00:00", transport::DateTimeError::InvalidOffset},
  {"2026-01-01T00:00:00z", transport::DateTimeError::InvalidOffset},
  {"2026-01-01T00:00:00.Z", transport::DateTimeError::InvalidFormat},
  {"2026-02-29T00:00:00Z", transport::DateTimeError::InvalidDate},
  {"2024-02-30T00:00:00Z", transport::DateTimeError::InvalidDate},
  {"1900-02-29T00:00:00Z", transport::DateTimeError::InvalidDate},
  {"2100-02-29T00:00:00Z", transport::DateTimeError::InvalidDate},
  {"2026-13-01T00:00:00Z", transport::DateTimeError::InvalidDate},
  {"2026-01-01T24:00:00Z", transport::DateTimeError::InvalidDate},
  {"2026-01-01T00:60:00Z", transport::DateTimeError::InvalidDate},
  {"2026-01-01T00:00:60Z", transport::DateTimeError::InvalidDate},
  {"2026-01-01T00:00:00+15:00", transport::DateTimeError::InvalidOffset},
  {"2026-01-01T00:00:00+14:01", transport::DateTimeError::InvalidOffset},
  {"2026-01-01T00:00:00+01", transport::DateTimeError::InvalidOffset},
};
