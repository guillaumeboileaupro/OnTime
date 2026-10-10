#pragma once
// Remplace GxEPD2 sur PC : l'ecran est un canvas Adafruit GFX 1 bit,
// enregistre en image a la fin de chaque dessin (nextPage).
#include <Adafruit_GFX.h>
#include "../image/bmp.h"

#define GxEPD_BLACK 0x0000
#define GxEPD_WHITE 0xFFFF

struct GxEPD2_420_GDEY042T81 {
  static const uint16_t WIDTH = 400;
  static const uint16_t HEIGHT = 300;
  GxEPD2_420_GDEY042T81(int cs, int dc, int rst, int busy) {}
};

template <typename Panneau, int hauteurPage>
class GxEPD2_BW : public GFXcanvas1 {
public:
  GxEPD2_BW(Panneau) : GFXcanvas1(Panneau::WIDTH, Panneau::HEIGHT) {}

  void init(uint32_t, bool, uint16_t, bool) {}
  void setFullWindow() {}
  void setPartialWindow(uint16_t, uint16_t, uint16_t, uint16_t) {}
  void firstPage() {}
  bool nextPage() {
    enregistrerBmp("ecran.bmp", *this);
    return false;
  }
  bool nextPageToPrevious() { return false; }  // l'ancienne image : rien a enregistrer
  void clearScreen() {}
  void hibernate() {}
  void powerOff() {}
};
