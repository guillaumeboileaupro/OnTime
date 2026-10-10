#pragma once

namespace transport {

enum class DateTimeError {
  None,
  InvalidFormat,
  InvalidDate,
  InvalidOffset,
};

struct DateTimeResult {
  bool ok;
  long long epochSeconds;
  DateTimeError error;
};

// Parses YYYY-MM-DDTHH:MM:SS[.fraction](Z|+HH:MM|-HH:MM).
// Conversion uses Gregorian calendar arithmetic only and never the host timezone.
DateTimeResult parseDateTime(const char* value);

} // namespace transport
