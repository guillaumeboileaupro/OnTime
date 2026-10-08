#include "icones.h"
#include "display.h"

#define TRAIT 2  // epaisseur du contour des icones

static void soleil(int16_t cx, int16_t cy, int16_t r) {
  display.fillCircle(cx, cy, r / 2, GxEPD_BLACK);
  display.fillCircle(cx, cy, r / 2 - TRAIT, GxEPD_WHITE);
  for (int i = 0; i < 8; i++) {
    float a = i * PI / 4;
    display.drawLine(cx + cos(a) * r * 0.7, cy + sin(a) * r * 0.7, cx + cos(a) * r, cy + sin(a) * r, GxEPD_BLACK);
  }
}

static void formeNuage(int16_t cx, int16_t cy, int16_t r, int16_t retrait, uint16_t couleur) {
  int16_t p = r / 2;
  display.fillCircle(cx - p, cy + p / 2, p - retrait, couleur);
  display.fillCircle(cx + p, cy + p / 2, p - retrait, couleur);
  display.fillCircle(cx, cy, r * 2 / 3 - retrait, couleur);
  display.fillRect(cx - p, cy + p / 2, 2 * p, p - retrait, couleur);
}

static void nuage(int16_t cx, int16_t cy, int16_t r) {
  formeNuage(cx, cy, r, 0, GxEPD_BLACK);
  formeNuage(cx, cy, r, TRAIT, GxEPD_WHITE);
}

void iconeMeteo(Meteo meteo, int16_t cx, int16_t cy, int16_t r) {
  switch (meteo) {
    case SOLEIL:
      soleil(cx, cy, r);
      break;
    case NUAGE:
      nuage(cx, cy, r);
      break;
    case SOLEIL_NUAGE:
      soleil(cx - r / 3, cy - r / 3, r * 2 / 3);
      nuage(cx + r / 4, cy + r / 4, r * 3 / 4);
      break;
    case PLUIE:
      nuage(cx, cy - r / 3, r * 3 / 4);
      for (int i = -1; i <= 1; i++) {
        display.drawLine(cx + i * r / 2, cy + r / 2, cx + i * r / 2 - 3, cy + r, GxEPD_BLACK);
      }
      break;
  }
}
