#include "zones.h"
#include "../display.h"
#include "../icones.h"
#include "../texte.h"
#include <Fonts/FreeSans12pt7b.h>
#include <Fonts/FreeSansBold18pt7b.h>

// Bas de l'ecran : le metro qu'on vise a gauche, la meteo a droite
void dessinerMeteo(const Donnees& d) {
  display.drawFastHLine(16, 250, display.width() - 32, GxEPD_BLACK);

  display.setFont(&FreeSans12pt7b);
  display.setCursor(16, 284);
  display.print("Depart ");
  display.print(d.metro);

  iconeMeteo(d.meteo, 300, 274, 16);
  display.setFont(&FreeSansBold18pt7b);
  display.setCursor(324, 287);
  display.print(d.temperature);
  ecrireDegre(3, 25);
}
