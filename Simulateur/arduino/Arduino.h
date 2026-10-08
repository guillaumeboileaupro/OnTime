#pragma once
// Remplace le Arduino.h de l'ESP32 pour compiler sur PC
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "Print.h"

#define PROGMEM
#ifndef PI
#define PI 3.14159265358979f
#endif
#define radians(deg) ((deg) * PI / 180)

// Memoire RTC de l'ESP32 (garde pendant le sommeil profond) : une variable normale sur PC
#define RTC_DATA_ATTR

// Declare par Adafruit GFX, pas utilise par ProchainMetro
class __FlashStringHelper;
