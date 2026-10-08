#include "horloge.h"
#include <Arduino.h>

void horlogeRegler() {
  configTzTime("CET-1CEST,M3.5.0,M10.5.0/3", "pool.ntp.org");  // heure de Paris, ete compris
  struct tm t;
  getLocalTime(&t);  // attend que l'heure arrive
}
