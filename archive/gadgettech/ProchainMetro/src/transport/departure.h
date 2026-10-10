#pragma once

namespace transport {

enum class Mode { Train, Rer, Metro, Tram };
enum class Quality { Realtime, Scheduled, Estimated };
enum class Status { Available, Empty, Stale, Error };

// Provider-qualified IDs: IDFM and SNCF identifiers must not be interchanged.
// Missing provider data stays missing; the domain never derives a new service.
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

struct Selection {
  Status status;
  const Departure* departure;
};

} // namespace transport
