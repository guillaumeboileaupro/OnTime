#include "zones.h"
#include "../display.h"
#include "../polices/FreeSansBold80pt7b.h"
#include <Fonts/FreeSans12pt7b.h>
#include <Fonts/FreeSansBold24pt7b.h>

#define CONTOUR 3  // epaisseur du contour des grands chiffres

// Chiffres blancs entoures de noir
static void chiffreContour(int x, int y, int valeur) {
  display.setTextColor(GxEPD_BLACK);
  for (int dx = -CONTOUR; dx <= CONTOUR; dx++)
    for (int dy = -CONTOUR; dy <= CONTOUR; dy++)
      if (dx * dx + dy * dy <= CONTOUR * CONTOUR) {
        display.setCursor(x + dx, y + dy);
        display.print(valeur);
      }
  display.setTextColor(GxEPD_WHITE);
  display.setCursor(x, y);
  display.print(valeur);
  display.setTextColor(GxEPD_BLACK);
}

// L'info principale : dans combien de minutes partir de chez soi
void dessinerDepart(const Donnees& d) {
  display.setFont(&FreeSans12pt7b);
  display.setCursor(16, 104);
  display.print("Partir dans");

  display.setFont(&FreeSansBold80pt7b);
  display.setCursor(10, 230);
  if (d.partirDans < 0) {  // pas de metro trouve : la police 80pt n'a que les chiffres, on dessine "--"
    for (int i = 0; i < CONTOUR; i++) {
      display.drawRect(20 + i, 164 + i, 50 - 2 * i, 16 - 2 * i, GxEPD_BLACK);
      display.drawRect(84 + i, 164 + i, 50 - 2 * i, 16 - 2 * i, GxEPD_BLACK);
    }
    display.setCursor(134, 230);
  }
  else chiffreContour(10, 230, d.partirDans);

  display.setFont(&FreeSansBold24pt7b);
  display.setCursor(display.getCursorX() + 12, 230);
  display.print("min");
}
