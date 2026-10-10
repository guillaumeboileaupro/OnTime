#include "heure.h"
#include <stdio.h>

int heureEnSecondes(const char* date) {
  int h, m, s;
  sscanf(date + 11, "%d:%d:%d", &h, &m, &s);
  return h * 3600 + m * 60 + s;
}
