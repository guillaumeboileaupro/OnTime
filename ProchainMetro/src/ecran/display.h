#pragma once

// Pas encore d'ecran e-paper ? ECRAN_VIRTUEL a 1 : le code tourne sur l'ESP32 et
// l'image s'affiche sur le PC (EcranVirtuel/ecran.html). A 0 avec le vrai ecran.
// (Le simulateur PC n'est pas un ESP32 : il garde son propre faux GxEPD2.)
#define ECRAN_VIRTUEL 0

#if ECRAN_VIRTUEL && defined(ESP32)
#include "virtuel/ecran_virtuel.h"
#else
#include <GxEPD2_BW.h>
#endif

// Broches (DIN = GPIO23 et CLK = GPIO18 sont les broches SPI par defaut)
#define EPD_CS   33
#define EPD_DC   25
#define EPD_RST  26
#define EPD_BUSY 27

// Waveshare 4.2" V2. Pour un V1 : remplacer GxEPD2_420_GDEY042T81 par GxEPD2_420.
using Ecran = GxEPD2_BW<GxEPD2_420_GDEY042T81, GxEPD2_420_GDEY042T81::HEIGHT>;

extern Ecran display;
