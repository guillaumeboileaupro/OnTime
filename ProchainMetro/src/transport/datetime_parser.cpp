#include "datetime_parser.h"

#include <stddef.h>

namespace transport {
namespace {

DateTimeResult failure(DateTimeError error) {
  return {false, 0, error};
}

bool isDigit(char value) {
  return value >= '0' && value <= '9';
}

bool readDigits(const char* value, size_t start, size_t count, int& result) {
  result = 0;
  for (size_t i = 0; i < count; ++i) {
    const char current = value[start + i];
    if (!isDigit(current)) return false;
    result = result * 10 + current - '0';
  }
  return true;
}

bool isLeapYear(int year) {
  return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
}

int daysInMonth(int year, int month) {
  static const int days[] = {31, 28, 31, 30, 31, 30,
                             31, 31, 30, 31, 30, 31};
  if (month == 2 && isLeapYear(year)) return 29;
  return days[month - 1];
}

long long daysBeforeYear(int year) {
  const long long previous = year - 1;
  return previous * 365 + previous / 4 - previous / 100 + previous / 400;
}

long long daysSinceEpoch(int year, int month, int day) {
  long long days = daysBeforeYear(year) - daysBeforeYear(1970);
  for (int currentMonth = 1; currentMonth < month; ++currentMonth)
    days += daysInMonth(year, currentMonth);
  return days + day - 1;
}

} // namespace

DateTimeResult parseDateTime(const char* value) {
  if (!value) return failure(DateTimeError::InvalidFormat);

  int year, month, day, hour, minute, second;
  if (!readDigits(value, 0, 4, year) || value[4] != '-' ||
      !readDigits(value, 5, 2, month) || value[7] != '-' ||
      !readDigits(value, 8, 2, day) || value[10] != 'T' ||
      !readDigits(value, 11, 2, hour) || value[13] != ':' ||
      !readDigits(value, 14, 2, minute) || value[16] != ':' ||
      !readDigits(value, 17, 2, second))
    return failure(DateTimeError::InvalidFormat);

  if (year < 1 || month < 1 || month > 12 || day < 1 ||
      day > daysInMonth(year, month) || hour > 23 || minute > 59 || second > 59)
    return failure(DateTimeError::InvalidDate);

  size_t position = 19;
  if (value[position] == '.') {
    ++position;
    const size_t fractionStart = position;
    while (isDigit(value[position])) ++position;
    if (position == fractionStart)
      return failure(DateTimeError::InvalidFormat);
  }

  int offsetSeconds = 0;
  if (value[position] == 'Z') {
    if (value[position + 1] != '\0')
      return failure(DateTimeError::InvalidFormat);
  } else if (value[position] == '+' || value[position] == '-') {
    const int direction = value[position] == '+' ? 1 : -1;
    int offsetHour, offsetMinute;
    if (!readDigits(value, position + 1, 2, offsetHour) ||
        value[position + 3] != ':' ||
        !readDigits(value, position + 4, 2, offsetMinute) ||
        value[position + 6] != '\0')
      return failure(DateTimeError::InvalidOffset);
    if (offsetHour > 14 || offsetMinute > 59 ||
        (offsetHour == 14 && offsetMinute != 0))
      return failure(DateTimeError::InvalidOffset);
    offsetSeconds = direction * (offsetHour * 3600 + offsetMinute * 60);
  } else {
    return failure(DateTimeError::InvalidOffset);
  }

  const long long localSeconds = daysSinceEpoch(year, month, day) * 86400 +
    hour * 3600 + minute * 60 + second;
  return {true, localSeconds - offsetSeconds, DateTimeError::None};
}

} // namespace transport
