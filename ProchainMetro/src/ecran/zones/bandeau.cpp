#include "zones.h"
#include "../display.h"
#include "../texte.h"
#include <Fonts/FreeSans12pt7b.h>
#include <Fonts/FreeSansBold12pt7b.h>
#include <Fonts/FreeSansBold18pt7b.h>

#define HAUT 25  // marge en haut de l'ecran

void dessinerBandeau(const Donnees& d) {
  // Pastille de la ligne, comme sur les plans du metro
  display.fillCircle(38, HAUT + 34, 22, GxEPD_BLACK);
  display.setFont(&FreeSansBold18pt7b);
  display.setTextColor(GxEPD_WHITE);
  ecrireCentre(d.ligne, 38, HAUT + 47);
  display.setTextColor(GxEPD_BLACK);

  display.setFont(&FreeSans12pt7b);
  int16_t x1, y1;
  uint16_t w, h;
  display.getTextBounds(d.heure, 0, HAUT + 43, &x1, &y1, &w, &h);
  ecrireDroite(d.heure, 384, HAUT + 43);

  // Nom de la station, coupe pour ne pas deborder sur l'heure
  display.setFont(&FreeSansBold12pt7b);
  ecrireCoupe(d.station, 72, HAUT + 43, 384 - w - 12 - 72);
}
